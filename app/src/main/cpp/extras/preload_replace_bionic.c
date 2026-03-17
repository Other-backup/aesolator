#define _GNU_SOURCE

#include "redirect_common.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <stddef.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/un.h>
#include <unistd.h>

typedef int (*open_fn)(const char *, int, ...);
typedef int (*openat_fn)(int, const char *, int, ...);
typedef int (*fstatat_fn)(int, const char *, struct stat *, int);
typedef ssize_t (*read_fn)(int, void *, size_t);
typedef int (*ioctl_fn)(int, int, ...);
typedef ssize_t (*readlink_fn)(const char *, char *, size_t);
typedef int (*connect_fn)(int, const struct sockaddr *, socklen_t);
typedef int (*bind_fn)(int, const struct sockaddr *, socklen_t);

static open_fn real_open_fn;
static open_fn real_open64_fn;
static openat_fn real_openat_fn;
static fstatat_fn real_fstatat_fn;
static read_fn real_read_fn;
static ioctl_fn real_ioctl_fn;
static readlink_fn real_readlink_fn;
static connect_fn real_connect_fn;
static bind_fn real_bind_fn;
static volatile int resolving_symbols;
static volatile int redirect_ready;

__attribute__((constructor))
static void aero_redirect_mark_ready(void) {
    redirect_ready = 1;
}

static void ensure_resolved() {
    if (resolving_symbols) return;
    resolving_symbols = 1;
    if (!real_open_fn) real_open_fn = (open_fn)dlsym(RTLD_NEXT, "open");
    if (!real_open64_fn) real_open64_fn = (open_fn)dlsym(RTLD_NEXT, "open64");
    if (!real_openat_fn) real_openat_fn = (openat_fn)dlsym(RTLD_NEXT, "openat");
    if (!real_fstatat_fn) real_fstatat_fn = (fstatat_fn)dlsym(RTLD_NEXT, "fstatat");
    if (!real_read_fn) real_read_fn = (read_fn)dlsym(RTLD_NEXT, "read");
    if (!real_ioctl_fn) real_ioctl_fn = (ioctl_fn)dlsym(RTLD_NEXT, "ioctl");
    if (!real_readlink_fn) real_readlink_fn = (readlink_fn)dlsym(RTLD_NEXT, "readlink");
    if (!real_connect_fn) real_connect_fn = (connect_fn)dlsym(RTLD_NEXT, "connect");
    if (!real_bind_fn) real_bind_fn = (bind_fn)dlsym(RTLD_NEXT, "bind");
    resolving_symbols = 0;
}

static int flags_need_mode(int flags) {
    return (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
}

static int loader_bootstrap_passthrough() {
    return resolving_symbols || !redirect_ready;
}

static int syscall_openat_passthrough(int dirfd, const char *path, int flags, mode_t mode) {
    return (int)syscall(SYS_openat, dirfd, path, flags, mode);
}

static int syscall_fstatat_passthrough(int dirfd, const char *path, struct stat *statbuf, int flags) {
    return (int)syscall(SYS_newfstatat, dirfd, path, statbuf, flags);
}

static ssize_t syscall_readlink_passthrough(const char *path, char *buffer, size_t buffer_size) {
    return (ssize_t)syscall(SYS_readlinkat, AT_FDCWD, path, buffer, buffer_size);
}

static int rewrite_and_open(const char *symbol, open_fn fn, const char *path, int flags, va_list ap) {
    ensure_resolved();
    mode_t mode = 0;
    if (flags_need_mode(flags)) mode = va_arg(ap, mode_t);
    if (!fn || loader_bootstrap_passthrough()) {
        return syscall_openat_passthrough(AT_FDCWD, path, flags, mode);
    }
    if (aero_is_event_node(path)) {
        errno = ENOENT;
        return -1;
    }

    char *rewritten = aero_rewrite_path(path);
    const char *effective = rewritten ? rewritten : path;
    int result = flags_need_mode(flags) ? fn(effective, flags, mode) : fn(effective, flags);
    if (rewritten) {
        aero_redirect_log(symbol, "open path '%s' -> '%s'", path, effective);
        free(rewritten);
    }
    return result;
}

static int rewrite_and_openat(const char *path, int dirfd, int flags, va_list ap) {
    ensure_resolved();
    mode_t mode = 0;
    if (flags_need_mode(flags)) mode = va_arg(ap, mode_t);
    if (!real_openat_fn || loader_bootstrap_passthrough()) {
        return syscall_openat_passthrough(dirfd, path, flags, mode);
    }
    if (aero_is_event_node(path)) {
        errno = ENOENT;
        return -1;
    }

    char *rewritten = aero_rewrite_path(path);
    const char *effective = rewritten ? rewritten : path;
    int result = flags_need_mode(flags)
            ? real_openat_fn(dirfd, effective, flags, mode)
            : real_openat_fn(dirfd, effective, flags);
    if (rewritten) {
        aero_redirect_log("openat", "path '%s' -> '%s'", path, effective);
        free(rewritten);
    }
    return result;
}

static int rewrite_unix_sockaddr(
        const struct sockaddr *address,
        socklen_t address_len,
        struct sockaddr_un *storage,
        const struct sockaddr **effective_address,
        socklen_t *effective_length
) {
    if (!address || !storage || !effective_address || !effective_length) return 0;
    if (address->sa_family != AF_UNIX) return 0;
    if (address_len <= offsetof(struct sockaddr_un, sun_path)) return 0;

    const struct sockaddr_un *input = (const struct sockaddr_un *)address;
    if (input->sun_path[0] == '\0') return 0;

    size_t raw_len = strnlen(input->sun_path, sizeof(input->sun_path));
    if (raw_len == 0) return 0;

    char path[sizeof(input->sun_path) + 1];
    memcpy(path, input->sun_path, raw_len);
    path[raw_len] = '\0';

    char *rewritten = aero_rewrite_path(path);
    if (!rewritten) return 0;

    memset(storage, 0, sizeof(*storage));
    storage->sun_family = AF_UNIX;
    snprintf(storage->sun_path, sizeof(storage->sun_path), "%s", rewritten);
    free(rewritten);

    *effective_address = (const struct sockaddr *)storage;
    *effective_length = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + strlen(storage->sun_path) + 1);
    aero_redirect_log("unix-socket", "rewrote unix socket path '%s' -> '%s'", path, storage->sun_path);
    return 1;
}

int open(const char *path, int flags, ...) {
    va_list ap;
    va_start(ap, flags);
    int result = rewrite_and_open("open", real_open_fn, path, flags, ap);
    va_end(ap);
    return result;
}

int open64(const char *path, int flags, ...) {
    va_list ap;
    va_start(ap, flags);
    open_fn fn = real_open64_fn ? real_open64_fn : real_open_fn;
    int result = rewrite_and_open("open64", fn, path, flags, ap);
    va_end(ap);
    return result;
}

int openat(int dirfd, const char *path, int flags, ...) {
    va_list ap;
    va_start(ap, flags);
    int result = rewrite_and_openat(path, dirfd, flags, ap);
    va_end(ap);
    return result;
}

int connect(int sockfd, const struct sockaddr *address, socklen_t address_len) {
    ensure_resolved();
    if (!real_connect_fn) {
        errno = ENOSYS;
        return -1;
    }

    struct sockaddr_un rewritten_address;
    const struct sockaddr *effective_address = address;
    socklen_t effective_length = address_len;
    rewrite_unix_sockaddr(address, address_len, &rewritten_address, &effective_address, &effective_length);
    return real_connect_fn(sockfd, effective_address, effective_length);
}

int bind(int sockfd, const struct sockaddr *address, socklen_t address_len) {
    ensure_resolved();
    if (!real_bind_fn) {
        errno = ENOSYS;
        return -1;
    }

    struct sockaddr_un rewritten_address;
    const struct sockaddr *effective_address = address;
    socklen_t effective_length = address_len;
    rewrite_unix_sockaddr(address, address_len, &rewritten_address, &effective_address, &effective_length);
    return real_bind_fn(sockfd, effective_address, effective_length);
}

int fstatat(int dirfd, const char *path, struct stat *statbuf, int flags) {
    ensure_resolved();
    if (!real_fstatat_fn || loader_bootstrap_passthrough()) {
        return syscall_fstatat_passthrough(dirfd, path, statbuf, flags);
    }
    if (aero_is_event_node(path)) {
        errno = ENOENT;
        return -1;
    }

    char *rewritten = aero_rewrite_path(path);
    const char *effective = rewritten ? rewritten : path;
    int result = real_fstatat_fn(dirfd, effective, statbuf, flags);
    if (rewritten) free(rewritten);
    return result;
}

ssize_t readlink(const char *path, char *buffer, size_t buffer_size) {
    ensure_resolved();
    if (!real_readlink_fn || loader_bootstrap_passthrough()) {
        return syscall_readlink_passthrough(path, buffer, buffer_size);
    }

    char *rewritten = aero_rewrite_path(path);
    const char *effective = rewritten ? rewritten : path;
    ssize_t length = real_readlink_fn(effective, buffer, buffer_size);
    if (rewritten) free(rewritten);
    if (length <= 0 || !buffer || buffer_size == 0) return length;

    size_t safe_length = (size_t)length < buffer_size ? (size_t)length : buffer_size - 1;
    char snapshot[1024];
    if (safe_length >= sizeof(snapshot)) safe_length = sizeof(snapshot) - 1;
    memcpy(snapshot, buffer, safe_length);
    snapshot[safe_length] = '\0';

    char *redirected = aero_rewrite_path(snapshot);
    if (!redirected) return length;

    size_t redirected_len = strlen(redirected);
    if (redirected_len > buffer_size) redirected_len = buffer_size;
    memcpy(buffer, redirected, redirected_len);
    free(redirected);
    return (ssize_t)redirected_len;
}

int ioctl(int fd, int request, ...) {
    ensure_resolved();
    char link_path[64];
    char target_path[256];
    snprintf(link_path, sizeof(link_path), "/proc/self/fd/%d", fd);
    ssize_t length = readlink(link_path, target_path, sizeof(target_path) - 1);
    if (length > 0) {
        target_path[length] = '\0';
        if (aero_is_event_node(target_path)) {
            errno = ENOTTY;
            return -1;
        }
    }

    va_list ap;
    va_start(ap, request);
    void *arg = va_arg(ap, void *);
    va_end(ap);
    if (!real_ioctl_fn || loader_bootstrap_passthrough()) {
        return (int)syscall(SYS_ioctl, fd, request, arg);
    }
    return real_ioctl_fn(fd, request, arg);
}

ssize_t read(int fd, void *buffer, size_t count) {
    ensure_resolved();
    char link_path[64];
    char target_path[256];
    snprintf(link_path, sizeof(link_path), "/proc/self/fd/%d", fd);
    ssize_t length = readlink(link_path, target_path, sizeof(target_path) - 1);
    if (length > 0) {
        target_path[length] = '\0';
        if (aero_is_event_node(target_path)) {
            errno = EAGAIN;
            return -1;
        }
    }

    if (!real_read_fn || loader_bootstrap_passthrough()) {
        return (ssize_t)syscall(SYS_read, fd, buffer, count);
    }
    return real_read_fn(fd, buffer, count);
}
