#define _GNU_SOURCE

#include "redirect_common.h"

#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <unistd.h>

extern char **environ;

typedef int (*open_fn)(const char *, int, ...);
typedef int (*openat_fn)(int, const char *, int, ...);
typedef int (*access_fn)(const char *, int);
typedef int (*stat_fn)(const char *, struct stat *);
typedef int (*lstat_fn)(const char *, struct stat *);
typedef FILE *(*fopen_fn)(const char *, const char *);
typedef int (*rename_fn)(const char *, const char *);
typedef int (*unlink_fn)(const char *);
typedef int (*mkdir_fn)(const char *, mode_t);
typedef int (*chdir_fn)(const char *);
typedef int (*rmdir_fn)(const char *);
typedef DIR *(*opendir_fn)(const char *);
typedef void *(*dlopen_fn)(const char *, int);
typedef int (*execve_fn)(const char *, char *const[], char *const[]);
typedef int (*execv_fn)(const char *, char *const[]);
typedef int (*execvp_fn)(const char *, char *const[]);
typedef int (*posix_spawn_fn)(pid_t *, const char *, const posix_spawn_file_actions_t *, const posix_spawnattr_t *, char *const[], char *const[]);
typedef int (*posix_spawnp_fn)(pid_t *, const char *, const posix_spawn_file_actions_t *, const posix_spawnattr_t *, char *const[], char *const[]);
typedef int (*connect_fn)(int, const struct sockaddr *, socklen_t);
typedef int (*bind_fn)(int, const struct sockaddr *, socklen_t);

static open_fn real_open_fn;
static open_fn real_open64_fn;
static openat_fn real_openat_fn;
static access_fn real_access_fn;
static stat_fn real_stat_fn;
static lstat_fn real_lstat_fn;
static fopen_fn real_fopen_fn;
static fopen_fn real_fopen64_fn;
static rename_fn real_rename_fn;
static unlink_fn real_unlink_fn;
static mkdir_fn real_mkdir_fn;
static chdir_fn real_chdir_fn;
static rmdir_fn real_rmdir_fn;
static opendir_fn real_opendir_fn;
static dlopen_fn real_dlopen_fn;
static execve_fn real_execve_fn;
static execv_fn real_execv_fn;
static execvp_fn real_execvp_fn;
static posix_spawn_fn real_posix_spawn_fn;
static posix_spawnp_fn real_posix_spawnp_fn;
static connect_fn real_connect_fn;
static bind_fn real_bind_fn;

static void ensure_resolved() {
    if (!real_open_fn) real_open_fn = (open_fn)dlsym(RTLD_NEXT, "open");
    if (!real_open64_fn) real_open64_fn = (open_fn)dlsym(RTLD_NEXT, "open64");
    if (!real_openat_fn) real_openat_fn = (openat_fn)dlsym(RTLD_NEXT, "openat");
    if (!real_access_fn) real_access_fn = (access_fn)dlsym(RTLD_NEXT, "access");
    if (!real_stat_fn) real_stat_fn = (stat_fn)dlsym(RTLD_NEXT, "stat");
    if (!real_lstat_fn) real_lstat_fn = (lstat_fn)dlsym(RTLD_NEXT, "lstat");
    if (!real_fopen_fn) real_fopen_fn = (fopen_fn)dlsym(RTLD_NEXT, "fopen");
    if (!real_fopen64_fn) real_fopen64_fn = (fopen_fn)dlsym(RTLD_NEXT, "fopen64");
    if (!real_rename_fn) real_rename_fn = (rename_fn)dlsym(RTLD_NEXT, "rename");
    if (!real_unlink_fn) real_unlink_fn = (unlink_fn)dlsym(RTLD_NEXT, "unlink");
    if (!real_mkdir_fn) real_mkdir_fn = (mkdir_fn)dlsym(RTLD_NEXT, "mkdir");
    if (!real_chdir_fn) real_chdir_fn = (chdir_fn)dlsym(RTLD_NEXT, "chdir");
    if (!real_rmdir_fn) real_rmdir_fn = (rmdir_fn)dlsym(RTLD_NEXT, "rmdir");
    if (!real_opendir_fn) real_opendir_fn = (opendir_fn)dlsym(RTLD_NEXT, "opendir");
    if (!real_dlopen_fn) real_dlopen_fn = (dlopen_fn)dlsym(RTLD_NEXT, "dlopen");
    if (!real_execve_fn) real_execve_fn = (execve_fn)dlsym(RTLD_NEXT, "execve");
    if (!real_execv_fn) real_execv_fn = (execv_fn)dlsym(RTLD_NEXT, "execv");
    if (!real_execvp_fn) real_execvp_fn = (execvp_fn)dlsym(RTLD_NEXT, "execvp");
    if (!real_posix_spawn_fn) real_posix_spawn_fn = (posix_spawn_fn)dlsym(RTLD_NEXT, "posix_spawn");
    if (!real_posix_spawnp_fn) real_posix_spawnp_fn = (posix_spawnp_fn)dlsym(RTLD_NEXT, "posix_spawnp");
    if (!real_connect_fn) real_connect_fn = (connect_fn)dlsym(RTLD_NEXT, "connect");
    if (!real_bind_fn) real_bind_fn = (bind_fn)dlsym(RTLD_NEXT, "bind");
}

static int flags_need_mode(int flags) {
    return (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
}

static const char *effective_path(char **storage, const char *path) {
    *storage = aero_rewrite_path(path);
    return *storage ? *storage : path;
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
    ensure_resolved();
    va_list ap;
    va_start(ap, flags);
    mode_t mode = 0;
    if (flags_need_mode(flags)) mode = va_arg(ap, mode_t);
    va_end(ap);

    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = flags_need_mode(flags) ? real_open_fn(effective, flags, mode) : real_open_fn(effective, flags);
    free(rewritten);
    return result;
}

int open64(const char *path, int flags, ...) {
    ensure_resolved();
    va_list ap;
    va_start(ap, flags);
    mode_t mode = 0;
    if (flags_need_mode(flags)) mode = va_arg(ap, mode_t);
    va_end(ap);

    open_fn fn = real_open64_fn ? real_open64_fn : real_open_fn;
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = flags_need_mode(flags) ? fn(effective, flags, mode) : fn(effective, flags);
    free(rewritten);
    return result;
}

int openat(int dirfd, const char *path, int flags, ...) {
    ensure_resolved();
    va_list ap;
    va_start(ap, flags);
    mode_t mode = 0;
    if (flags_need_mode(flags)) mode = va_arg(ap, mode_t);
    va_end(ap);

    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = flags_need_mode(flags)
            ? real_openat_fn(dirfd, effective, flags, mode)
            : real_openat_fn(dirfd, effective, flags);
    free(rewritten);
    return result;
}

int access(const char *path, int mode) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_access_fn(effective, mode);
    free(rewritten);
    return result;
}

int stat(const char *path, struct stat *buffer) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_stat_fn(effective, buffer);
    free(rewritten);
    return result;
}

int lstat(const char *path, struct stat *buffer) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_lstat_fn(effective, buffer);
    free(rewritten);
    return result;
}

FILE *fopen(const char *path, const char *mode) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    FILE *result = real_fopen_fn(effective, mode);
    free(rewritten);
    return result;
}

FILE *fopen64(const char *path, const char *mode) {
    ensure_resolved();
    fopen_fn fn = real_fopen64_fn ? real_fopen64_fn : real_fopen_fn;
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    FILE *result = fn(effective, mode);
    free(rewritten);
    return result;
}

int rename(const char *old_path, const char *new_path) {
    ensure_resolved();
    char *old_rewritten = NULL;
    char *new_rewritten = NULL;
    const char *effective_old = effective_path(&old_rewritten, old_path);
    const char *effective_new = effective_path(&new_rewritten, new_path);
    int result = real_rename_fn(effective_old, effective_new);
    free(old_rewritten);
    free(new_rewritten);
    return result;
}

int unlink(const char *path) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_unlink_fn(effective);
    free(rewritten);
    return result;
}

int mkdir(const char *path, mode_t mode) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_mkdir_fn(effective, mode);
    free(rewritten);
    return result;
}

int chdir(const char *path) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_chdir_fn(effective);
    free(rewritten);
    return result;
}

int rmdir(const char *path) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_rmdir_fn(effective);
    free(rewritten);
    return result;
}

DIR *opendir(const char *path) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    DIR *result = real_opendir_fn(effective);
    free(rewritten);
    return result;
}

void *dlopen(const char *path, int flags) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    void *result = real_dlopen_fn(effective, flags);
    free(rewritten);
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

int execve(const char *path, char *const argv[], char *const envp[]) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    char **sanitized_envp = aero_rewrite_envp(envp);
    int result = real_execve_fn(effective, argv, sanitized_envp ? sanitized_envp : (char *const *)envp);
    aero_free_envp(sanitized_envp);
    free(rewritten);
    return result;
}

int execv(const char *path, char *const argv[]) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_execv_fn(effective, argv);
    free(rewritten);
    return result;
}

int execvp(const char *path, char *const argv[]) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    int result = real_execvp_fn(effective, argv);
    free(rewritten);
    return result;
}

int posix_spawn(pid_t *pid, const char *path, const posix_spawn_file_actions_t *file_actions,
                const posix_spawnattr_t *attrp, char *const argv[], char *const envp[]) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, path);
    char **sanitized_envp = aero_rewrite_envp(envp ? envp : environ);
    int result = real_posix_spawn_fn(pid, effective, file_actions, attrp, argv, sanitized_envp ? sanitized_envp : (envp ? envp : environ));
    aero_free_envp(sanitized_envp);
    free(rewritten);
    return result;
}

int posix_spawnp(pid_t *pid, const char *file, const posix_spawn_file_actions_t *file_actions,
                 const posix_spawnattr_t *attrp, char *const argv[], char *const envp[]) {
    ensure_resolved();
    char *rewritten = NULL;
    const char *effective = effective_path(&rewritten, file);
    char **sanitized_envp = aero_rewrite_envp(envp ? envp : environ);
    int result = real_posix_spawnp_fn(pid, effective, file_actions, attrp, argv, sanitized_envp ? sanitized_envp : (envp ? envp : environ));
    aero_free_envp(sanitized_envp);
    free(rewritten);
    return result;
}
