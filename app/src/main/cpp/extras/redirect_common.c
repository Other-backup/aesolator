#define _GNU_SOURCE

#include "redirect_common.h"

#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static const char *DEFAULT_PACKAGE_NAME = "com.winlator.cmod";

static int aero_debug_enabled() {
    const char *flag = getenv("AERO_REDIRECT_DEBUG");
    return flag && (*flag == '1' || *flag == 'y' || *flag == 'Y' || *flag == 't' || *flag == 'T');
}

void aero_redirect_log(const char *scope, const char *format, ...) {
    if (!aero_debug_enabled()) return;

    fprintf(stderr, "[aero-redirect:%s] ", scope ? scope : "unknown");
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
    fputc('\n', stderr);
}

static const char *aero_package_name() {
    const char *package_name = getenv("AERO_RUNTIME_PACKAGE_NAME");
    return (package_name && *package_name) ? package_name : DEFAULT_PACKAGE_NAME;
}

static const char *aero_rootfs_path() {
    const char *rootfs = getenv("AERO_RUNTIME_ROOTFS_PATH");
    return (rootfs && *rootfs) ? rootfs : NULL;
}

static const char *aero_tmp_path() {
    const char *tmp_path = getenv("AERO_RUNTIME_TMP_PATH");
    return (tmp_path && *tmp_path) ? tmp_path : NULL;
}

static const char *aero_files_path() {
    const char *files_dir = getenv("AERO_RUNTIME_FILES_PATH");
    return (files_dir && *files_dir) ? files_dir : NULL;
}

static char *aero_strdup_or_null(const char *value) {
    if (!value) return NULL;
    size_t len = strlen(value);
    char *copy = (char *)malloc(len + 1);
    if (!copy) return NULL;
    memcpy(copy, value, len + 1);
    return copy;
}

static char *aero_replace_all(const char *input, const char *needle, const char *replacement, int *changed) {
    if (!input || !needle || !replacement) return aero_strdup_or_null(input);

    size_t input_len = strlen(input);
    size_t needle_len = strlen(needle);
    size_t replacement_len = strlen(replacement);
    if (needle_len == 0) return aero_strdup_or_null(input);

    size_t count = 0;
    const char *cursor = input;
    while ((cursor = strstr(cursor, needle)) != NULL) {
        count++;
        cursor += needle_len;
    }

    if (count == 0) return aero_strdup_or_null(input);

    size_t output_len = input_len + (replacement_len - needle_len) * count;
    char *output = (char *)malloc(output_len + 1);
    if (!output) return aero_strdup_or_null(input);

    const char *source = input;
    char *dest = output;
    while ((cursor = strstr(source, needle)) != NULL) {
        size_t prefix = (size_t)(cursor - source);
        memcpy(dest, source, prefix);
        dest += prefix;
        memcpy(dest, replacement, replacement_len);
        dest += replacement_len;
        source = cursor + needle_len;
    }
    strcpy(dest, source);
    if (changed) *changed = 1;
    return output;
}

static char *aero_apply_replacement(char *current, const char *needle, const char *replacement, int *changed) {
    char *updated = aero_replace_all(current, needle, replacement, changed);
    if (!updated) return current;
    free(current);
    return updated;
}

static int aero_matches_path_prefix(const char *input, const char *prefix) {
    if (!input || !prefix) return 0;
    size_t prefix_len = strlen(prefix);
    if (prefix_len == 0) return 0;
    if (strncmp(input, prefix, prefix_len) != 0) return 0;
    return input[prefix_len] == '\0' || input[prefix_len] == '/';
}

static char *aero_rewrite_absolute_prefix(const char *input, const char *prefix, const char *replacement_root) {
    if (!input || !prefix || !replacement_root) return NULL;
    if (!aero_matches_path_prefix(input, prefix)) return NULL;

    size_t root_len = strlen(replacement_root);
    size_t suffix_len = strlen(input) - strlen(prefix);
    size_t output_len = root_len + suffix_len;
    char *output = (char *)malloc(output_len + 1);
    if (!output) return NULL;

    memcpy(output, replacement_root, root_len);
    memcpy(output + root_len, input + strlen(prefix), suffix_len);
    output[output_len] = '\0';
    return output;
}

static char *aero_join_path_root(const char *root, const char *suffix) {
    if (!root || !suffix) return NULL;
    size_t root_len = strlen(root);
    size_t suffix_len = strlen(suffix);
    int trim_slash = root_len > 0 && root[root_len - 1] == '/' && suffix[0] == '/';
    size_t output_len = root_len + suffix_len - (trim_slash ? 1 : 0);
    char *output = (char *)malloc(output_len + 1);
    if (!output) return NULL;

    memcpy(output, root, root_len);
    char *cursor = output + root_len;
    if (trim_slash) suffix++;
    memcpy(cursor, suffix, strlen(suffix));
    output[output_len] = '\0';
    return output;
}

int aero_is_event_node(const char *path) {
    return path && strncmp(path, "/dev/input/event", 16) == 0;
}

char *aero_rewrite_path(const char *input) {
    if (!input || !*input) return NULL;

    int changed = 0;
    char *current = aero_strdup_or_null(input);
    if (!current) return NULL;

    const char *rootfs = aero_rootfs_path();
    const char *tmp_path = aero_tmp_path();
    const char *files_dir = aero_files_path();
    const char *package_name = aero_package_name();

    if (rootfs) {
        current = aero_apply_replacement(current, "/data/data/app.gamenative/files/imagefs", rootfs, &changed);
        current = aero_apply_replacement(current, "/data/user/0/app.gamenative/files/imagefs", rootfs, &changed);
        current = aero_apply_replacement(current, "/data/data/com.winlator/files/rootfs", rootfs, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.winlator/files/rootfs", rootfs, &changed);
        current = aero_apply_replacement(current, "/data/data/com.winlator/files/imagefs", rootfs, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.winlator/files/imagefs", rootfs, &changed);

        char *termux_usr_tmp_root = aero_join_path_root(rootfs, "/usr/tmp");
        char *termux_usr_share_root = aero_join_path_root(rootfs, "/usr/share");
        char *termux_usr_lib_root = aero_join_path_root(rootfs, "/usr/lib");
        char *termux_usr_etc_root = aero_join_path_root(rootfs, "/usr/etc");
        char *termux_usr_var_root = aero_join_path_root(rootfs, "/usr/var");
        char *termux_usr_bin_root = aero_join_path_root(rootfs, "/usr/bin");
        char *termux_usr_root = aero_join_path_root(rootfs, "/usr");
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr/tmp", termux_usr_tmp_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr/tmp", termux_usr_tmp_root, &changed);
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr/share", termux_usr_share_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr/share", termux_usr_share_root, &changed);
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr/lib", termux_usr_lib_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr/lib", termux_usr_lib_root, &changed);
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr/etc", termux_usr_etc_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr/etc", termux_usr_etc_root, &changed);
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr/var", termux_usr_var_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr/var", termux_usr_var_root, &changed);
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr/bin", termux_usr_bin_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr/bin", termux_usr_bin_root, &changed);
        current = aero_apply_replacement(current, "/data/data/com.termux/files/usr", termux_usr_root, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.termux/files/usr", termux_usr_root, &changed);
        free(termux_usr_tmp_root);
        free(termux_usr_share_root);
        free(termux_usr_lib_root);
        free(termux_usr_etc_root);
        free(termux_usr_var_root);
        free(termux_usr_bin_root);
        free(termux_usr_root);
    }

    if (files_dir) {
        current = aero_apply_replacement(current, "/data/data/app.gamenative/files", files_dir, &changed);
        current = aero_apply_replacement(current, "/data/user/0/app.gamenative/files", files_dir, &changed);
        current = aero_apply_replacement(current, "/data/data/com.winlator/files", files_dir, &changed);
        current = aero_apply_replacement(current, "/data/user/0/com.winlator/files", files_dir, &changed);
    }

    current = aero_apply_replacement(current, "app.gamenative", package_name, &changed);

    if (rootfs && !aero_matches_path_prefix(current, rootfs)) {
        char *usr_tmp_root = aero_join_path_root(rootfs, "/usr/tmp");
        char *usr_local_root = aero_join_path_root(rootfs, "/usr/local");
        char *usr_root = aero_join_path_root(rootfs, "/usr");
        char *opt_root = aero_join_path_root(rootfs, "/opt");
        char *home_root = aero_join_path_root(rootfs, "/home");
        char *etc_root = aero_join_path_root(rootfs, "/etc");
        char *var_root = aero_join_path_root(rootfs, "/var");
        char *bin_root = aero_join_path_root(rootfs, "/bin");
        char *lib64_root = aero_join_path_root(rootfs, "/lib64");
        char *lib_root = aero_join_path_root(rootfs, "/lib");
        struct {
            const char *prefix;
            const char *replacement;
        } path_map[] = {
                {"/tmp", tmp_path ? tmp_path : NULL},
                {"/usr/tmp", usr_tmp_root},
                {"/usr/local", usr_local_root},
                {"/usr", usr_root},
                {"/opt", opt_root},
                {"/home", home_root},
                {"/etc", etc_root},
                {"/var", var_root},
                {"/bin", bin_root},
                {"/lib64", lib64_root},
                {"/lib", lib_root},
        };

        for (size_t i = 0; i < sizeof(path_map) / sizeof(path_map[0]); i++) {
            if (!path_map[i].replacement) continue;
            char *rewritten = aero_rewrite_absolute_prefix(current, path_map[i].prefix, path_map[i].replacement);
            if (!rewritten) continue;
            free(current);
            current = rewritten;
            changed = 1;
            break;
        }

        free(usr_tmp_root);
        free(usr_local_root);
        free(usr_root);
        free(opt_root);
        free(home_root);
        free(etc_root);
        free(var_root);
        free(bin_root);
        free(lib64_root);
        free(lib_root);
    }

    if (!changed) {
        free(current);
        return NULL;
    }

    aero_redirect_log("path", "rewrote path '%s' -> '%s'", input, current);
    return current;
}

static int aero_is_preload_var(const char *name) {
    if (!name) return 0;
    return strcmp(name, "LD_PRELOAD") == 0 || strcmp(name, "BOX64_LD_PRELOAD") == 0;
}

static int aero_should_drop_preload_token(const char *token) {
    if (!token || !*token) return 1;
    if (strstr(token, "libpluviagoldberg.so")) return 1;
    if (strstr(token, "/data/data/app.gamenative/")) return 1;
    if (strstr(token, "/data/user/0/app.gamenative/")) return 1;
    if (strstr(token, "/data/data/com.winlator/")) return 1;
    if (strstr(token, "/data/user/0/com.winlator/")) return 1;
    return 0;
}

static char *aero_sanitize_preload_value(const char *value) {
    if (!value) return NULL;

    size_t source_len = strlen(value);
    char *scratch = aero_strdup_or_null(value);
    if (!scratch) return NULL;

    char *output = (char *)calloc(source_len + 1, 1);
    if (!output) {
        free(scratch);
        return NULL;
    }

    int changed = 0;
    size_t out_len = 0;
    char *cursor = scratch;
    while (*cursor) {
        while (*cursor == ':' || isspace((unsigned char)*cursor)) cursor++;
        if (!*cursor) break;

        char *end = cursor;
        while (*end && *end != ':' && !isspace((unsigned char)*end)) end++;
        char saved = *end;
        *end = '\0';

        if (!aero_should_drop_preload_token(cursor)) {
            char *rewritten = aero_rewrite_path(cursor);
            const char *token = rewritten ? rewritten : cursor;
            size_t token_len = strlen(token);
            if (token_len > 0) {
                if (out_len > 0) output[out_len++] = ':';
                memcpy(output + out_len, token, token_len);
                out_len += token_len;
            }
            if (rewritten) free(rewritten);
        } else {
            changed = 1;
        }

        *end = saved;
        cursor = end;
    }

    output[out_len] = '\0';
    if (strcmp(output, value) != 0) changed = 1;

    free(scratch);
    if (!changed) {
        free(output);
        return NULL;
    }

    aero_redirect_log("env", "rewrote preload '%s' -> '%s'", value, output);
    return output;
}

char *aero_rewrite_env_value(const char *name, const char *value) {
    if (!value) return NULL;
    if (aero_is_preload_var(name)) {
        return aero_sanitize_preload_value(value);
    }
    return aero_rewrite_path(value);
}

char **aero_rewrite_envp(char *const envp[]) {
    if (!envp) return NULL;

    size_t count = 0;
    while (envp[count]) count++;

    char **result = (char **)calloc(count + 1, sizeof(char *));
    if (!result) return NULL;

    size_t out_index = 0;
    for (size_t i = 0; i < count; i++) {
        const char *entry = envp[i];
        if (!entry) continue;

        const char *equals = strchr(entry, '=');
        if (!equals) {
            result[out_index++] = aero_strdup_or_null(entry);
            continue;
        }

        size_t name_len = (size_t)(equals - entry);
        char *name = (char *)malloc(name_len + 1);
        if (!name) continue;
        memcpy(name, entry, name_len);
        name[name_len] = '\0';

        const char *value = equals + 1;
        char *rewritten_value = aero_rewrite_env_value(name, value);
        const char *effective_value = rewritten_value ? rewritten_value : value;
        if (aero_is_preload_var(name) && (!effective_value || !*effective_value)) {
            free(rewritten_value);
            free(name);
            continue;
        }

        size_t entry_len = strlen(name) + 1 + strlen(effective_value);
        char *rewritten_entry = (char *)malloc(entry_len + 1);
        if (rewritten_entry) {
            snprintf(rewritten_entry, entry_len + 1, "%s=%s", name, effective_value);
            result[out_index++] = rewritten_entry;
        }

        free(rewritten_value);
        free(name);
    }

    result[out_index] = NULL;
    return result;
}

void aero_free_envp(char **envp) {
    if (!envp) return;
    for (size_t i = 0; envp[i]; i++) free(envp[i]);
    free(envp);
}
