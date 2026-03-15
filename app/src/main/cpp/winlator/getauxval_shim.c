#include <errno.h>
#include <fcntl.h>
#include <unistd.h>

typedef struct AuxvEntry {
    unsigned long type;
    unsigned long value;
} AuxvEntry;

// Avoid compiler-rt's private bionic copy, which crashes when this .so is dlopen'ed.
unsigned long getauxval(unsigned long type) {
    int fd = open("/proc/self/auxv", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return 0;

    AuxvEntry entry;
    while (read(fd, &entry, sizeof(entry)) == (ssize_t)sizeof(entry)) {
        if (entry.type == 0) break;
        if (entry.type == type) {
            close(fd);
            return entry.value;
        }
    }

    close(fd);
    errno = ENOENT;
    return 0;
}
