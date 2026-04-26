/* evshim.c - SDL virtual joystick bridge for Android-hosted Wine.
 * Donor uplift:
 * - mmap-backed shared state instead of read()/pwrite() loops
 * - delta-only SDL updates
 * - adaptive polling
 * - late hotplug scan
 * - /dev/input/event* suppression to force Proton/SDL through the virtual path
 */

#define _GNU_SOURCE

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <sched.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

typedef struct SDL_Joystick SDL_Joystick;
typedef struct {
    int major, minor, patch;
} SDL_version;
typedef struct SDL_VirtualJoystickDesc {
    uint16_t version;
    uint16_t type;
    uint16_t naxes;
    uint16_t nbuttons;
    uint16_t nhats;
    uint16_t vendor_id;
    uint16_t product_id;
    uint16_t padding;
    uint32_t button_mask;
    uint32_t axis_mask;
    const char *name;
    void *userdata;
    void (*Update)(void *);
    void (*SetPlayerIndex)(void *, int);
    int (*Rumble)(void *, uint16_t, uint16_t);
    int (*RumbleTriggers)(void *, uint16_t, uint16_t);
    int (*SetLED)(void *, uint8_t, uint8_t, uint8_t);
    int (*SendEffect)(void *, const void *, int);
} SDL_VirtualJoystickDesc;

#define SDL_VIRTUAL_JOYSTICK_DESC_VERSION 1
#define SDL_JOYSTICK_TYPE_GAMECONTROLLER 1
#define SDL_INIT_JOYSTICK 0x00000200

static int g_debug_enabled = 0;
static int g_spinwait_enabled = 0;

#define LOGI(...) dprintf(STDOUT_FILENO, __VA_ARGS__)
#define LOGE(...) dprintf(STDERR_FILENO, __VA_ARGS__)
#define LOGD(...) do { if (g_debug_enabled) dprintf(STDOUT_FILENO, __VA_ARGS__); } while (0)

#define MAX_GAMEPADS 4
#define GAMEPAD_MEM_SIZE 64
#define GAMEPAD_VENDOR_ID 0x1234
#define GAMEPAD_PRODUCT_ID 0x5678
#define GAMEPAD_NAME "Ae.solator Virtual Gamepad"

#define POLL_FAST_NS 500000L
#define POLL_SLOW_NS 4000000L
#define IDLE_THRESHOLD 50
#define AXIS_DEADZONE 256

struct gamepad_io {
    int16_t lx, ly, rx, ry, lt, rt;
    uint8_t btn[15];
    uint8_t hat;
    uint8_t _padding[4];
    uint16_t low_freq_rumble;
    uint16_t high_freq_rumble;
};

struct controller_state {
    SDL_Joystick *js;
    volatile struct gamepad_io *mem;
    int mem_fd;
    int16_t last_axes[6];
    uint8_t last_btns[15];
    uint8_t last_hat;
    int active;
};

static int vjoy_ids[MAX_GAMEPADS] = {-1, -1, -1, -1};
static struct controller_state ctrl[MAX_GAMEPADS] = {0};
static int g_num_players = 0;
static void *handle = NULL;
static char g_data_path[256] = {0};
static char g_base_name[128] = {0};

static int (*p_SDL_Init)(uint32_t);
static const char *(*p_SDL_GetError)(void);
static SDL_Joystick *(*p_SDL_JoystickOpen)(int);
static int (*p_SDL_JoystickAttachVirtualEx)(const SDL_VirtualJoystickDesc *);
static int (*p_SDL_JoystickSetVirtualAxis)(SDL_Joystick *, int, int16_t);
static int (*p_SDL_JoystickSetVirtualButton)(SDL_Joystick *, int, uint8_t);
static int (*p_SDL_JoystickSetVirtualHat)(SDL_Joystick *, int, uint8_t);
static void (*p_SDL_PumpEvents)(void);
static void (*p_SDL_Delay)(uint32_t);
static void (*p_SDL_GetVersion)(SDL_version *);

#define GETFUNCPTR(name) \
do { \
    if (!(p_##name = (typeof(p_##name))dlsym(handle, #name))) { \
        LOGE("Failed to load SDL symbol %s\n", #name); \
    } \
} while (0)

#if defined(__GNUC__) || defined(__clang__)
#define ATOMIC_LOAD(ptr) __atomic_load_n(ptr, __ATOMIC_ACQUIRE)
#define ATOMIC_STORE(ptr, val) __atomic_store_n(ptr, val, __ATOMIC_RELEASE)
#else
#define ATOMIC_LOAD(ptr) (*(volatile typeof(*(ptr)) *)(ptr))
#define ATOMIC_STORE(ptr, val) do { *(volatile typeof(*(ptr)) *)(ptr) = (val); __asm__ __volatile__("" ::: "memory"); } while (0)
#endif

static const char *runtime_tmp_dir(void) {
    const char *tmp_dir = getenv("AERO_RUNTIME_TMP_PATH");
    return (tmp_dir && *tmp_dir) ? tmp_dir : "/data/data/com.winlator.cmod/files/imagefs/tmp";
}

static const char *shared_memory_base_name(void) {
    const char *name = getenv("EVSHIM_SHM_NAME");
    return (name && *name) ? name : "controller-shm0";
}

static void build_shared_memory_path(int player_index, char *buffer, size_t buffer_size) {
    const char *tmp_dir = runtime_tmp_dir();
    const char *base_name = shared_memory_base_name();
    if (player_index <= 0) {
        snprintf(buffer, buffer_size, "%s/%s", tmp_dir, base_name);
    } else {
        snprintf(buffer, buffer_size, "%s/%s-%d", tmp_dir, base_name, player_index);
    }
}

static inline int16_t apply_deadzone(int16_t val) {
    int16_t abs_val = val < 0 ? -val : val;
    return abs_val < AXIS_DEADZONE ? 0 : val;
}

static char *make_virtual_pad_name(void) {
    return strdup(GAMEPAD_NAME);
}

static int OnRumble(void *userdata, uint16_t low, uint16_t high) {
    int idx = (int)(intptr_t)userdata;
    if (idx < 0 || idx >= MAX_GAMEPADS || !ctrl[idx].mem) return -1;
    volatile struct gamepad_io *mem = ctrl[idx].mem;
    ATOMIC_STORE(&mem->low_freq_rumble, low);
    ATOMIC_STORE(&mem->high_freq_rumble, high);
    return 0;
}

static void *event_pump_thread(void *arg) {
    (void)arg;
    for (;;) {
        p_SDL_PumpEvents();
        p_SDL_Delay(5);
    }
    return NULL;
}

static void *unified_updater(void *arg) {
    (void)arg;
    struct timespec fast_sleep = {0, POLL_FAST_NS};
    struct timespec slow_sleep = {0, POLL_SLOW_NS};
    int idle_count = 0;

    for (int i = 0; i < g_num_players; i++) {
        if (vjoy_ids[i] < 0 || !ctrl[i].mem) continue;
        ctrl[i].js = p_SDL_JoystickOpen(vjoy_ids[i]);
        if (!ctrl[i].js) {
            LOGE("P%d: SDL_JoystickOpen failed\n", i);
            continue;
        }
        ctrl[i].active = 1;
        LOGI("EVSHIM P%d active\n", i + 1);
    }

    LOGI("EVSHIM updater active (fast=%ldus slow=%ldus) pid=%d\n", POLL_FAST_NS / 1000, POLL_SLOW_NS / 1000, getpid());

    for (;;) {
        int had_updates = 0;

        for (int i = 0; i < g_num_players; i++) {
            if (!ctrl[i].active) continue;

            volatile struct gamepad_io *mem = ctrl[i].mem;
            SDL_Joystick *js = ctrl[i].js;
            int16_t axes[6];

            axes[0] = apply_deadzone(ATOMIC_LOAD(&mem->lx));
            axes[1] = apply_deadzone(ATOMIC_LOAD(&mem->ly));
            axes[2] = apply_deadzone(ATOMIC_LOAD(&mem->rx));
            axes[3] = apply_deadzone(ATOMIC_LOAD(&mem->ry));
            axes[4] = ATOMIC_LOAD(&mem->lt);
            axes[5] = ATOMIC_LOAD(&mem->rt);

            for (int a = 0; a < 6; a++) {
                if (axes[a] != ctrl[i].last_axes[a]) {
                    p_SDL_JoystickSetVirtualAxis(js, a, axes[a]);
                    ctrl[i].last_axes[a] = axes[a];
                    had_updates = 1;
                }
            }

            for (int b = 0; b < 15; b++) {
                uint8_t btn = ATOMIC_LOAD(&mem->btn[b]);
                if (btn != ctrl[i].last_btns[b]) {
                    p_SDL_JoystickSetVirtualButton(js, b, btn);
                    ctrl[i].last_btns[b] = btn;
                    had_updates = 1;
                }
            }

            uint8_t hat = ATOMIC_LOAD(&mem->hat);
            if (hat != ctrl[i].last_hat) {
                p_SDL_JoystickSetVirtualHat(js, 0, hat);
                ctrl[i].last_hat = hat;
                had_updates = 1;
            }
        }

        if (had_updates) {
            idle_count = 0;
            if (g_spinwait_enabled) {
                sched_yield();
            } else {
                nanosleep(&fast_sleep, NULL);
            }
        } else {
            idle_count++;
            if (idle_count > IDLE_THRESHOLD) {
                nanosleep(&slow_sleep, NULL);
            } else {
                nanosleep(&fast_sleep, NULL);
            }
        }
    }
    return NULL;
}

static void *watchdog_thread(void *arg) {
    (void)arg;
    struct timespec check_interval = {1, 0};

    while (1) {
        pthread_t tid;
        int result = pthread_create(&tid, NULL, unified_updater, NULL);
        if (result != 0) {
            LOGE("Failed to create updater thread: %d\n", result);
            nanosleep(&check_interval, NULL);
            continue;
        }

        pthread_join(tid, NULL);
        LOGE("Updater thread exited unexpectedly, respawning in 1s...\n");
        nanosleep(&check_interval, NULL);
    }
    return NULL;
}

static int create_virtual_pad_for_slot(int idx, const char *path) {
    int fd = open(path, O_RDWR);
    if (fd < 0) return 0;

    void *mem = mmap(NULL, GAMEPAD_MEM_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (mem == MAP_FAILED) {
        close(fd);
        return 0;
    }

    SDL_VirtualJoystickDesc desc = {0};
    desc.version = SDL_VIRTUAL_JOYSTICK_DESC_VERSION;
    desc.type = SDL_JOYSTICK_TYPE_GAMECONTROLLER;
    desc.naxes = 6;
    desc.nbuttons = 15;
    desc.nhats = 1;
    desc.vendor_id = GAMEPAD_VENDOR_ID;
    desc.product_id = GAMEPAD_PRODUCT_ID;
    desc.Rumble = &OnRumble;
    desc.userdata = (void *)(intptr_t)idx;
    desc.name = make_virtual_pad_name();

    int vjoy_id = p_SDL_JoystickAttachVirtualEx(&desc);
    if (vjoy_id < 0) {
        munmap(mem, GAMEPAD_MEM_SIZE);
        close(fd);
        return 0;
    }

    ctrl[idx].mem_fd = fd;
    ctrl[idx].mem = (volatile struct gamepad_io *)mem;
    vjoy_ids[idx] = vjoy_id;
    if (idx >= g_num_players) g_num_players = idx + 1;
    return 1;
}

static void try_attach_controller(int idx) {
    if (idx < 0 || idx >= MAX_GAMEPADS || ctrl[idx].active || ctrl[idx].mem) return;

    char path[384];
    if (idx == 0) {
        snprintf(path, sizeof(path), "%s/%s", g_data_path, g_base_name);
    } else {
        snprintf(path, sizeof(path), "%s/%s-%d", g_data_path, g_base_name, idx);
    }

    if (access(path, F_OK) != 0) return;
    if (!create_virtual_pad_for_slot(idx, path)) return;
    LOGI("EVSHIM hotplug attached P%d from %s\n", idx + 1, path);
}

static void *hotplug_thread(void *arg) {
    (void)arg;
    struct timespec interval = {2, 0};
    LOGI("EVSHIM hotplug active\n");

    while (1) {
        nanosleep(&interval, NULL);
        for (int i = 0; i < MAX_GAMEPADS; i++) {
            if (!ctrl[i].active) {
                try_attach_controller(i);
            }
        }
    }
    return NULL;
}

__attribute__((constructor))
static void initialize_all_pads(void) {
    const char *debug_flag = getenv("EVSHIM_DEBUG");
    g_debug_enabled = debug_flag && strchr("1yYtT", *debug_flag);
    const char *spinwait = getenv("EVSHIM_SPINWAIT");
    g_spinwait_enabled = spinwait && strchr("1yYtT", *spinwait);

    handle = dlopen("libSDL2-2.0.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!handle) {
        LOGE("dlopen SDL failed: %s\n", dlerror());
        return;
    }

    GETFUNCPTR(SDL_Init);
    GETFUNCPTR(SDL_GetError);
    GETFUNCPTR(SDL_JoystickOpen);
    GETFUNCPTR(SDL_JoystickAttachVirtualEx);
    GETFUNCPTR(SDL_JoystickSetVirtualAxis);
    GETFUNCPTR(SDL_JoystickSetVirtualButton);
    GETFUNCPTR(SDL_JoystickSetVirtualHat);
    GETFUNCPTR(SDL_PumpEvents);
    GETFUNCPTR(SDL_Delay);
    GETFUNCPTR(SDL_GetVersion);

    p_SDL_Init(SDL_INIT_JOYSTICK);

    SDL_version version;
    p_SDL_GetVersion(&version);
    LOGI("EVSHIM SDL %d.%d.%d\n", version.major, version.minor, version.patch);

    g_num_players = getenv("EVSHIM_MAX_PLAYERS") ? atoi(getenv("EVSHIM_MAX_PLAYERS")) : 1;
    if (g_num_players < 1) g_num_players = 1;
    if (g_num_players > MAX_GAMEPADS) g_num_players = MAX_GAMEPADS;

    strncpy(g_data_path, runtime_tmp_dir(), sizeof(g_data_path) - 1);
    strncpy(g_base_name, shared_memory_base_name(), sizeof(g_base_name) - 1);

    int attached = 0;
    for (int player = 0; player < g_num_players; ++player) {
        char path[384];
        build_shared_memory_path(player, path, sizeof(path));
        if (create_virtual_pad_for_slot(player, path)) {
            attached++;
        } else {
            LOGD("EVSHIM missing shared state for P%d at %s\n", player + 1, path);
        }
    }

    pthread_t pump_tid;
    pthread_create(&pump_tid, NULL, event_pump_thread, NULL);
    pthread_detach(pump_tid);

    if (attached > 0) {
        pthread_t watchdog_tid;
        pthread_create(&watchdog_tid, NULL, watchdog_thread, NULL);
        pthread_detach(watchdog_tid);
        LOGI("EVSHIM attached %d controller(s)\n", attached);
    }

    pthread_t hotplug_tid;
    pthread_create(&hotplug_tid, NULL, hotplug_thread, NULL);
    pthread_detach(hotplug_tid);
}

static inline int is_event_node(const char *path) {
    return path && !strncmp(path, "/dev/input/event", 16);
}

typedef int (*open_f)(const char *, int, ...);
static open_f real_open;

int open(const char *path, int flags, ...) __attribute__((visibility("default")));
int open(const char *path, int flags, ...) {
    if (is_event_node(path)) {
        errno = ENOENT;
        return -1;
    }

    if (!real_open) {
        real_open = (open_f)dlsym(RTLD_NEXT, "open");
    }

    va_list ap;
    va_start(ap, flags);
    mode_t mode = (flags & O_CREAT) ? va_arg(ap, mode_t) : 0;
    va_end(ap);
    return real_open(path, flags, mode);
}
