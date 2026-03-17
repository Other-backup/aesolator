#ifndef AERO_REDIRECT_COMMON_H
#define AERO_REDIRECT_COMMON_H

#include <stddef.h>

char *aero_rewrite_path(const char *input);
char *aero_rewrite_env_value(const char *name, const char *value);
char **aero_rewrite_envp(char *const envp[]);
void aero_free_envp(char **envp);
int aero_is_event_node(const char *path);
void aero_redirect_log(const char *scope, const char *format, ...);

#endif
