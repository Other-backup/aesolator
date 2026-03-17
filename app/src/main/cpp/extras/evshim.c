#define _GNU_SOURCE

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

typedef uint8_t Uint8;
typedef uint16_t Uint16;
typedef uint32_t Uint32;
typedef int16_t Sint16;

typedef struct SDL_Joystick SDL_Joystick;

typedef struct SDL_version {
    Uint8 major;
    Uint8 minor;
    Uint8 patch;
} SDL_version;

typedef struct SDL_VirtualJoystickDesc {
    Uint16 version;
    Uint16 type;
    Uint16 naxes;
    Uint16 nbuttons;
    Uint16 nhats;
    Uint16 vendor_id;
    Uint16 product_id;
    Uint16 padding;
    Uint32 button_mask;
    Uint32 axis_mask;
    const char *name;
    void *userdata;
    void (*Update)(void *userdata);
    void (*SetPlayerIndex)(void *userdata, int player_index);
    int (*Rumble)(void *userdata, Uint16 low_frequency_rumble, Uint16 high_frequency_rumble);
    int (*RumbleTriggers)(void *userdata, Uint16 left_rumble, Uint16 right_rumble);
    int (*SetLED)(void *userdata, Uint8 red, Uint8 green, Uint8 blue);
    int (*SendEffect)(void *userdata, const void *data, int size);
} SDL_VirtualJoystickDesc;

#define SDL_INIT_JOYSTICK 0x00000200u
#define SDL_JOYSTICK_TYPE_GAMECONTROLLER 1
#define SDL_VIRTUAL_JOYSTICK_DESC_VERSION 1

static int g_debug_enabled = 0;

#define LOGI(...) dprintf(STDOUT_FILENO, __VA_ARGS__)
#define LOGE(...) dprintf(STDERR_FILENO, __VA_ARGS__)
#define LOGD(...) do { if (g_debug_enabled) dprintf(STDOUT_FILENO, __VA_ARGS__); } while (0)

#define MAX_GAMEPADS 4
static int vjoy_ids[MAX_GAMEPADS] = {-1};
static int read_fd[MAX_GAMEPADS] = {-1};
static int rumble_fd[MAX_GAMEPADS] = {-1};
static void *handle = NULL;
static pthread_mutex_t shm_mutex = PTHREAD_MUTEX_INITIALIZER;

struct gamepad_io {
    int16_t lx, ly, rx, ry, lt, rt;
    uint8_t btn[15];
    uint8_t hat;
    uint8_t _padding[4];
    uint16_t low_freq_rumble;
    uint16_t high_freq_rumble;
};

static int (*p_SDL_Init)(uint32_t flags);
static const char *(*p_SDL_GetError)(void);
static SDL_Joystick *(*p_SDL_JoystickOpen)(int device_index);
static int (*p_SDL_JoystickAttachVirtualEx)(const SDL_VirtualJoystickDesc *desc);
static int (*p_SDL_JoystickSetVirtualAxis)(SDL_Joystick *joystick, int axis, int16_t value);
static int (*p_SDL_JoystickSetVirtualButton)(SDL_Joystick *joystick, int button, uint8_t value);
static int (*p_SDL_JoystickSetVirtualHat)(SDL_Joystick *joystick, int hat, uint8_t value);
static void (*p_SDL_PumpEvents)(void);
static void (*p_SDL_Delay)(uint32_t ms);
static void (*p_SDL_GetVersion)(SDL_version *);

#define GETFUNCPTR(name) \
do { \
    if (!(p_##name = (typeof(p_##name))dlsym(handle, #name))) { \
        LOGE("Failed to load SDL symbol %s\n", #name); \
    } \
} while (0)

static const char *runtime_tmp_dir() {
    const char *tmp_dir = getenv("AERO_RUNTIME_TMP_PATH");
    return (tmp_dir && *tmp_dir) ? tmp_dir : "/data/data/com.winlator.cmod/files/imagefs/tmp";
}

static const char *shared_memory_base_name() {
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

static int OnRumble(void *userdata, uint16_t low_frequency_rumble, uint16_t high_frequency_rumble) {
    int index = (int)(intptr_t)userdata;
    if (index < 0 || index >= MAX_GAMEPADS || rumble_fd[index] < 0) return -1;

    uint16_t values[2] = {low_frequency_rumble, high_frequency_rumble};

    pthread_mutex_lock(&shm_mutex);
    ssize_t written = pwrite(rumble_fd[index], values, sizeof(values), 32);
    pthread_mutex_unlock(&shm_mutex);

    if (written != (ssize_t)sizeof(values)) {
        LOGE("Rumble write failed (P%d): %s\n", index, strerror(errno));
    }

    LOGD("Rumble P%d low=%u high=%u\n", index, low_frequency_rumble, high_frequency_rumble);
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

static void *vjoy_updater(void *arg) {
    int index = (int)(intptr_t)arg;
    int fd = read_fd[index];
    if (fd < 0) {
        LOGE("P%d: read_fd not initialized\n", index);
        return NULL;
    }

    SDL_Joystick *joystick = p_SDL_JoystickOpen(vjoy_ids[index]);
    if (!joystick) {
        LOGE("P%d: SDL_JoystickOpen failed\n", index);
        return NULL;
    }

    struct gamepad_io current_state;
    struct gamepad_io last_state = {0};

    for (;;) {
        pthread_mutex_lock(&shm_mutex);
        ssize_t count = read(fd, &current_state, sizeof(current_state));
        if (count == sizeof(current_state) && memcmp(&current_state, &last_state, sizeof(current_state)) != 0) {
            p_SDL_JoystickSetVirtualAxis(joystick, 0, current_state.lx);
            p_SDL_JoystickSetVirtualAxis(joystick, 1, current_state.ly);
            p_SDL_JoystickSetVirtualAxis(joystick, 2, current_state.rx);
            p_SDL_JoystickSetVirtualAxis(joystick, 3, current_state.ry);
            p_SDL_JoystickSetVirtualAxis(joystick, 4, current_state.lt);
            p_SDL_JoystickSetVirtualAxis(joystick, 5, current_state.rt);

            for (int i = 0; i < 15; ++i) {
                p_SDL_JoystickSetVirtualButton(joystick, i, current_state.btn[i]);
            }
            p_SDL_JoystickSetVirtualHat(joystick, 0, current_state.hat);
            last_state = current_state;
        } else if (count < 0) {
            LOGE("P%d: read error: %s\n", index, strerror(errno));
        }
        pthread_mutex_unlock(&shm_mutex);
        p_SDL_Delay(5);
    }
    return NULL;
}

__attribute__((constructor))
static void initialize_all_pads(void) {
    const char *debug_flag = getenv("EVSHIM_DEBUG");
    g_debug_enabled = debug_flag && strchr("1yYtT", *debug_flag);

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

    pthread_t pump_thread;
    pthread_create(&pump_thread, NULL, event_pump_thread, NULL);
    pthread_detach(pump_thread);

    int players = getenv("EVSHIM_MAX_PLAYERS") ? atoi(getenv("EVSHIM_MAX_PLAYERS")) : 1;
    if (players < 1) players = 1;
    if (players > MAX_GAMEPADS) players = MAX_GAMEPADS;

    for (int player = 0; player < players; ++player) {
        char path[512];
        build_shared_memory_path(player, path, sizeof(path));

        read_fd[player] = open(path, O_RDONLY);
        rumble_fd[player] = open(path, O_WRONLY);
        if (read_fd[player] < 0 || rumble_fd[player] < 0) {
            LOGE("P%d: failed to open shared file '%s': %s\n", player, path, strerror(errno));
            if (read_fd[player] >= 0) close(read_fd[player]);
            if (rumble_fd[player] >= 0) close(rumble_fd[player]);
            read_fd[player] = -1;
            rumble_fd[player] = -1;
            continue;
        }

        SDL_VirtualJoystickDesc description = {0};
        description.version = SDL_VIRTUAL_JOYSTICK_DESC_VERSION;
        description.type = SDL_JOYSTICK_TYPE_GAMECONTROLLER;
        description.naxes = 6;
        description.nbuttons = 15;
        description.nhats = 1;
        description.Rumble = &OnRumble;
        description.userdata = (void *)(intptr_t)player;

        char name[64];
        snprintf(name, sizeof(name), "Ae.solator Controller %d", player + 1);
        description.name = strdup(name);

        vjoy_ids[player] = p_SDL_JoystickAttachVirtualEx(&description);
        if (vjoy_ids[player] < 0) {
            LOGE("P%d: SDL attach failed: %s\n", player, p_SDL_GetError());
            close(read_fd[player]);
            close(rumble_fd[player]);
            read_fd[player] = -1;
            rumble_fd[player] = -1;
            free((void *)description.name);
            continue;
        }

        pthread_t thread;
        pthread_create(&thread, NULL, vjoy_updater, (void *)(intptr_t)player);
        pthread_detach(thread);
    }
}
