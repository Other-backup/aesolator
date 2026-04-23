#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <signal.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <dirent.h>
#include <linux/elf.h>
#include <asm/ptrace.h>

#ifndef ARRAY_SIZE
#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))
#endif

struct map_entry
{
    uint64_t start;
    uint64_t end;
    uint64_t file_off;
    char perms[8];
    char path[512];
};

struct map_table
{
    struct map_entry *entries;
    size_t count;
    size_t cap;
};

#define STACK_WORD_COUNT 8
#define FRAME_CHAIN_DEPTH 8
#define POINTER_WINDOW_WORD_COUNT 8
#define ATTACH_WAIT_TIMEOUT_MS 1200

struct pointer_window_spec
{
    const char *name;
    int reg_index;
};

static const struct pointer_window_spec pointer_window_specs[] =
{
    { "x0", 0 },
    { "x1", 1 },
    { "x2", 2 },
    { "x20", 20 },
    { "x21", 21 },
    { "x23", 23 },
    { "x24", 24 },
    { "x25", 25 },
    { "x27", 27 },
    { "x28", 28 },
};

static int read_word(pid_t tid, uint64_t addr, uint64_t *out)
{
    long value;
    errno = 0;
    value = ptrace(PTRACE_PEEKDATA, tid, (void *)(uintptr_t)addr, 0);
    if (value == -1 && errno) return -1;
    *out = (uint64_t)(unsigned long)value;
    return 0;
}

static int read_words(pid_t tid, uint64_t addr, uint64_t *out, size_t count)
{
    size_t i;
    for (i = 0; i < count; ++i)
    {
        if (read_word(tid, addr + i * sizeof(long), &out[i]) < 0) return -1;
    }
    return 0;
}

static long long now_ms(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (long long)ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
}

static int waitpid_stopped_timeout(pid_t tid, int *status, int timeout_ms)
{
    long long deadline = now_ms() + timeout_ms;

    for (;;)
    {
        pid_t rc = waitpid(tid, status, __WALL | WNOHANG);
        if (rc == tid) return 0;
        if (rc < 0)
        {
            if (errno == EINTR) continue;
            return -1;
        }
        if (now_ms() >= deadline)
        {
            errno = ETIMEDOUT;
            return -1;
        }
        usleep(10000);
    }
}

static void json_escape(FILE *out, const char *s)
{
    const unsigned char *p = (const unsigned char *)s;
    fputc('"', out);
    for (; *p; ++p)
    {
        switch (*p)
        {
            case '\\': fputs("\\\\", out); break;
            case '"':  fputs("\\\"", out); break;
            case '\n': fputs("\\n", out); break;
            case '\r': fputs("\\r", out); break;
            case '\t': fputs("\\t", out); break;
            default:
                if (*p < 0x20) fprintf(out, "\\u%04x", *p);
                else fputc(*p, out);
        }
    }
    fputc('"', out);
}

static void map_table_push(struct map_table *table, const struct map_entry *entry)
{
    if (table->count == table->cap)
    {
        size_t new_cap = table->cap ? table->cap * 2 : 128;
        struct map_entry *new_entries = realloc(table->entries, new_cap * sizeof(*new_entries));
        if (!new_entries)
        {
            perror("realloc maps");
            exit(2);
        }
        table->entries = new_entries;
        table->cap = new_cap;
    }
    table->entries[table->count++] = *entry;
}

static void load_maps(pid_t pid, struct map_table *table)
{
    char path[128];
    FILE *fp;
    char line[1024];

    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    fp = fopen(path, "r");
    if (!fp)
    {
        fprintf(stderr, "open maps failed for pid=%d: %s\n", pid, strerror(errno));
        return;
    }

    while (fgets(line, sizeof(line), fp))
    {
        struct map_entry entry;
        char dev[32] = "";
        unsigned long inode = 0;
        char mapped[512] = "";
        int n;

        memset(&entry, 0, sizeof(entry));
        n = sscanf(line, "%" SCNx64 "-%" SCNx64 " %7s %" SCNx64 " %31s %lu %511[^\n]",
                   &entry.start, &entry.end, entry.perms, &entry.file_off, dev, &inode, mapped);
        if (n < 6) continue;
        if (n == 7)
        {
            const char *trim = mapped;
            while (*trim == ' ' || *trim == '\t') trim++;
            snprintf(entry.path, sizeof(entry.path), "%s", trim);
        }
        map_table_push(table, &entry);
    }
    fclose(fp);
}

static const struct map_entry *find_map(const struct map_table *table, uint64_t addr)
{
    size_t i;
    for (i = 0; i < table->count; ++i)
    {
        if (table->entries[i].start <= addr && addr < table->entries[i].end) return &table->entries[i];
    }
    return NULL;
}

static void print_map_json(FILE *out, const struct map_entry *map, uint64_t addr)
{
    if (!map)
    {
        fputs("null", out);
        return;
    }

    fprintf(out, "{");
    fprintf(out, "\"start\":\"0x%016" PRIx64 "\",\"end\":\"0x%016" PRIx64 "\",", map->start, map->end);
    fprintf(out, "\"offset\":\"0x%016" PRIx64 "\",", (uint64_t)(addr - map->start + map->file_off));
    fprintf(out, "\"perms\":"); json_escape(out, map->perms);
    fprintf(out, ",\"path\":"); json_escape(out, map->path);
    fprintf(out, "}");
}

static void print_words_json(FILE *out, const uint64_t *words, size_t count)
{
    size_t i;
    fputc('[', out);
    for (i = 0; i < count; ++i)
    {
        if (i) fputc(',', out);
        fprintf(out, "\"0x%016" PRIx64 "\"", words[i]);
    }
    fputc(']', out);
}

static void print_pointer_windows_json(FILE *out, pid_t tid, const struct map_table *maps,
                                       const struct user_pt_regs *regs)
{
    size_t i;
    int first = 1;

    fputc('[', out);
    for (i = 0; i < ARRAY_SIZE(pointer_window_specs); ++i)
    {
        const struct pointer_window_spec *spec = &pointer_window_specs[i];
        const struct map_entry *map;
        uint64_t addr = (uint64_t)regs->regs[spec->reg_index];
        uint64_t words[POINTER_WINDOW_WORD_COUNT] = {0};

        if (!addr) continue;
        map = find_map(maps, addr);
        if (!map) continue;
        if (read_words(tid, addr, words, ARRAY_SIZE(words)) < 0) continue;

        if (!first) fputc(',', out);
        first = 0;
        fprintf(out, "{");
        fprintf(out, "\"reg\":");
        json_escape(out, spec->name);
        fprintf(out, ",\"addr\":\"0x%016" PRIx64 "\",", addr);
        fprintf(out, "\"map\":");
        print_map_json(out, map, addr);
        fprintf(out, ",\"words\":");
        print_words_json(out, words, ARRAY_SIZE(words));
        fprintf(out, "}");
    }
    fputc(']', out);
}

static void print_regs_json(FILE *out, const struct user_pt_regs *regs)
{
    size_t i;

    fputc('{', out);
    for (i = 0; i < 29; ++i)
    {
        if (i) fputc(',', out);
        fprintf(out, "\"x%zu\":\"0x%016" PRIx64 "\"", i, (uint64_t)regs->regs[i]);
    }
    fprintf(out, ",\"fp\":\"0x%016" PRIx64 "\"", (uint64_t)regs->regs[29]);
    fprintf(out, ",\"lr\":\"0x%016" PRIx64 "\"", (uint64_t)regs->regs[30]);
    fprintf(out, ",\"sp\":\"0x%016" PRIx64 "\"", (uint64_t)regs->sp);
    fprintf(out, ",\"pc\":\"0x%016" PRIx64 "\"", (uint64_t)regs->pc);
    fprintf(out, ",\"pstate\":\"0x%016" PRIx64 "\"", (uint64_t)regs->pstate);
    fputc('}', out);
}

static void print_frame_chain_json(FILE *out, pid_t tid, const struct map_table *maps, uint64_t fp)
{
    int depth;

    fputc('[', out);
    for (depth = 0; depth < FRAME_CHAIN_DEPTH && fp; ++depth)
    {
        uint64_t pair[2] = {0};
        const struct map_entry *lr_map;
        uint64_t next_fp;
        uint64_t ret_addr;

        if (read_words(tid, fp, pair, ARRAY_SIZE(pair)) < 0) break;
        next_fp = pair[0];
        ret_addr = pair[1];
        lr_map = find_map(maps, ret_addr);

        if (depth) fputc(',', out);
        fprintf(out, "{");
        fprintf(out, "\"depth\":%d,", depth);
        fprintf(out, "\"fp\":\"0x%016" PRIx64 "\",", fp);
        fprintf(out, "\"next_fp\":\"0x%016" PRIx64 "\",", next_fp);
        fprintf(out, "\"lr\":\"0x%016" PRIx64 "\",", ret_addr);
        fprintf(out, "\"map\":");
        print_map_json(out, lr_map, ret_addr);
        fprintf(out, "}");

        if (!next_fp || next_fp <= fp || next_fp - fp > 0x100000) break;
        fp = next_fp;
    }
    fputc(']', out);
}

static int64_t sign_extend_bits(uint64_t value, unsigned int bits)
{
    uint64_t sign = 1ULL << (bits - 1);
    return (int64_t)((value ^ sign) - sign);
}

static size_t split_arm64_insns(const uint64_t *words, size_t word_count, uint32_t *out, size_t out_cap)
{
    size_t i, count = 0;
    for (i = 0; i < word_count && count + 1 < out_cap; ++i)
    {
        out[count++] = (uint32_t)(words[i] & 0xffffffffu);
        out[count++] = (uint32_t)((words[i] >> 32) & 0xffffffffu);
    }
    return count;
}

static int decode_arm64_adrp(uint32_t insn, uint64_t pc, int *reg, uint64_t *page)
{
    uint64_t imm;
    if ((insn & 0x9f000000u) != 0x90000000u) return -1;
    *reg = insn & 0x1f;
    imm = ((uint64_t)((insn >> 5) & 0x7ffff) << 2) | ((insn >> 29) & 0x3);
    *page = (pc & ~0xfffULL) + ((uint64_t)sign_extend_bits(imm, 21) << 12);
    return 0;
}

static int decode_arm64_add_imm64(uint32_t insn, int *rd, int *rn, uint64_t *imm)
{
    if ((insn & 0xff000000u) != 0x91000000u) return -1;
    *rd = insn & 0x1f;
    *rn = (insn >> 5) & 0x1f;
    *imm = ((uint64_t)((insn >> 10) & 0xfff)) << (((insn >> 22) & 0x1) ? 12 : 0);
    return 0;
}

static int decode_arm64_ldr_uimm64(uint32_t insn, int *rt, int *rn, uint64_t *imm)
{
    if ((insn & 0xffc00000u) != 0xf9400000u) return -1;
    *rt = insn & 0x1f;
    *rn = (insn >> 5) & 0x1f;
    *imm = ((uint64_t)((insn >> 10) & 0xfff)) * 8ULL;
    return 0;
}

static int decode_arm64_br(uint32_t insn, int *rn)
{
    if ((insn & 0xfffffc1fu) != 0xd61f0000u) return -1;
    *rn = (insn >> 5) & 0x1f;
    return 0;
}

static int decode_arm64_import_thunk_slot(uint64_t pc, const uint64_t *words, size_t word_count,
                                          const char **kind, uint64_t *slot_addr)
{
    uint32_t insns[8];
    size_t insn_count = split_arm64_insns(words, word_count, insns, ARRAY_SIZE(insns));
    int reg, add_rd, add_rn, ldr_rt, ldr_rn, br_rn;
    uint64_t page, add_imm, ldr_imm;

    if (insn_count < 3) return -1;
    if (decode_arm64_adrp(insns[0], pc, &reg, &page) < 0) return -1;
    if (decode_arm64_add_imm64(insns[1], &add_rd, &add_rn, &add_imm) < 0) return -1;
    if (add_rd != reg || add_rn != reg) return -1;

    *slot_addr = page + add_imm;
    if (insn_count >= 4 &&
        decode_arm64_ldr_uimm64(insns[2], &ldr_rt, &ldr_rn, &ldr_imm) == 0 &&
        decode_arm64_br(insns[3], &br_rn) == 0 &&
        ldr_rt == reg && ldr_rn == reg && br_rn == reg)
    {
        *kind = "iat_load";
        *slot_addr += ldr_imm;
        return 0;
    }

    if (decode_arm64_br(insns[2], &br_rn) == 0 && br_rn == reg)
    {
        *kind = "jump_slot";
        return 0;
    }

    return -1;
}

static void print_thunk_live_json(FILE *out, pid_t tid, const struct map_table *maps, uint64_t pc,
                                  const uint64_t *words, size_t word_count)
{
    const char *kind = NULL;
    uint64_t slot_addr = 0, slot_value = 0;
    const struct map_entry *slot_value_map = NULL;

    if (decode_arm64_import_thunk_slot(pc, words, word_count, &kind, &slot_addr) < 0)
    {
        fputs("null", out);
        return;
    }

    if (read_word(tid, slot_addr, &slot_value) < 0) slot_value = 0;
    if (slot_value) slot_value_map = find_map(maps, slot_value);

    fprintf(out, "{");
    fprintf(out, "\"kind\":"); json_escape(out, kind);
    fprintf(out, ",\"slot_addr\":\"0x%016" PRIx64 "\"", slot_addr);
    fprintf(out, ",\"slot_value\":\"0x%016" PRIx64 "\"", slot_value);
    fprintf(out, ",\"slot_value_map\":");
    print_map_json(out, slot_value_map, slot_value);
    fprintf(out, "}");
}

static void read_text_file(char *dst, size_t dst_size, const char *path)
{
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    ssize_t n;
    if (fd < 0)
    {
        dst[0] = 0;
        return;
    }
    n = read(fd, dst, dst_size - 1);
    close(fd);
    if (n <= 0)
    {
        dst[0] = 0;
        return;
    }
    dst[n] = 0;
    while (n > 0 && (dst[n - 1] == '\n' || dst[n - 1] == '\r'))
    {
        dst[n - 1] = 0;
        --n;
    }
}

static size_t list_threads(pid_t pid, pid_t **out)
{
    char path[128];
    DIR *dir;
    struct dirent *de;
    pid_t *tids = NULL;
    size_t count = 0, cap = 0;

    snprintf(path, sizeof(path), "/proc/%d/task", pid);
    dir = opendir(path);
    if (!dir)
    {
        *out = NULL;
        return 0;
    }
    while ((de = readdir(dir)))
    {
        char *end = NULL;
        long tid;
        if (de->d_name[0] == '.') continue;
        tid = strtol(de->d_name, &end, 10);
        if (!end || *end) continue;
        if (count == cap)
        {
            size_t new_cap = cap ? cap * 2 : 16;
            pid_t *new_tids = realloc(tids, new_cap * sizeof(*new_tids));
            if (!new_tids)
            {
                perror("realloc tids");
                exit(2);
            }
            tids = new_tids;
            cap = new_cap;
        }
        tids[count++] = (pid_t)tid;
    }
    closedir(dir);
    *out = tids;
    return count;
}

static int sample_thread(pid_t pid, pid_t tid, const struct map_table *maps, int sample_no)
{
    struct iovec iov;
    struct user_pt_regs regs;
    char comm_path[128], wchan_path[128];
    char comm[128], wchan[128];
    const struct map_entry *pc_map, *lr_map;
    uint64_t pc_words[4] = {0}, lr_words[4] = {0}, stack_words[STACK_WORD_COUNT] = {0};
    int status = 0;

    if (ptrace(PTRACE_ATTACH, tid, 0, 0) < 0)
    {
        fprintf(stderr, "attach tid=%d failed: %s\n", tid, strerror(errno));
        return -1;
    }
    if (waitpid_stopped_timeout(tid, &status, ATTACH_WAIT_TIMEOUT_MS) < 0)
    {
        fprintf(stderr, "waitpid tid=%d failed/timed out after %dms: %s\n",
                tid, ATTACH_WAIT_TIMEOUT_MS, strerror(errno));
        ptrace(PTRACE_DETACH, tid, 0, 0);
        return -1;
    }
    if (!WIFSTOPPED(status))
    {
        fprintf(stderr, "tid=%d did not enter stopped state after attach (status=0x%x)\n", tid, status);
        ptrace(PTRACE_DETACH, tid, 0, 0);
        return -1;
    }

    memset(&regs, 0, sizeof(regs));
    iov.iov_base = &regs;
    iov.iov_len = sizeof(regs);
    if (ptrace(PTRACE_GETREGSET, tid, (void *)NT_PRSTATUS, &iov) < 0)
    {
        fprintf(stderr, "getregset tid=%d failed: %s\n", tid, strerror(errno));
        ptrace(PTRACE_DETACH, tid, 0, 0);
        return -1;
    }

    snprintf(comm_path, sizeof(comm_path), "/proc/%d/task/%d/comm", pid, tid);
    snprintf(wchan_path, sizeof(wchan_path), "/proc/%d/task/%d/wchan", pid, tid);
    read_text_file(comm, sizeof(comm), comm_path);
    read_text_file(wchan, sizeof(wchan), wchan_path);

    pc_map = find_map(maps, regs.pc);
    lr_map = find_map(maps, regs.regs[30]);
    read_words(tid, (uint64_t)regs.pc, pc_words, ARRAY_SIZE(pc_words));
    read_words(tid, (uint64_t)regs.regs[30], lr_words, ARRAY_SIZE(lr_words));
    read_words(tid, (uint64_t)regs.sp, stack_words, ARRAY_SIZE(stack_words));

    fprintf(stdout, "{");
    fprintf(stdout, "\"sample\":%d,", sample_no);
    fprintf(stdout, "\"ts_ms\":%lld,", now_ms());
    fprintf(stdout, "\"pid\":%d,\"tid\":%d,", pid, tid);
    fprintf(stdout, "\"pc\":\"0x%016" PRIx64 "\",", (uint64_t)regs.pc);
    fprintf(stdout, "\"lr\":\"0x%016" PRIx64 "\",", (uint64_t)regs.regs[30]);
    fprintf(stdout, "\"sp\":\"0x%016" PRIx64 "\",", (uint64_t)regs.sp);
    fprintf(stdout, "\"fp\":\"0x%016" PRIx64 "\",", (uint64_t)regs.regs[29]);
    fprintf(stdout, "\"pstate\":\"0x%016" PRIx64 "\",", (uint64_t)regs.pstate);
    fprintf(stdout, "\"comm\":"); json_escape(stdout, comm);
    fprintf(stdout, ",\"wchan\":"); json_escape(stdout, wchan);
    fprintf(stdout, ",\"regs\":");
    print_regs_json(stdout, &regs);
    fprintf(stdout, ",\"pc_words\":");
    print_words_json(stdout, pc_words, ARRAY_SIZE(pc_words));
    fprintf(stdout, ",\"lr_words\":");
    print_words_json(stdout, lr_words, ARRAY_SIZE(lr_words));
    fprintf(stdout, ",\"stack_words\":");
    print_words_json(stdout, stack_words, ARRAY_SIZE(stack_words));
    fprintf(stdout, ",\"pc_map\":");
    print_map_json(stdout, pc_map, (uint64_t)regs.pc);
    fprintf(stdout, ",\"lr_map\":");
    print_map_json(stdout, lr_map, (uint64_t)regs.regs[30]);
    fprintf(stdout, ",\"pc_thunk_live\":");
    print_thunk_live_json(stdout, tid, maps, (uint64_t)regs.pc, pc_words, ARRAY_SIZE(pc_words));
    fprintf(stdout, ",\"lr_thunk_live\":");
    print_thunk_live_json(stdout, tid, maps, (uint64_t)regs.regs[30], lr_words, ARRAY_SIZE(lr_words));
    fprintf(stdout, ",\"frame_chain\":");
    print_frame_chain_json(stdout, tid, maps, (uint64_t)regs.regs[29]);
    fprintf(stdout, ",\"pointer_windows\":");
    print_pointer_windows_json(stdout, tid, maps, &regs);
    fprintf(stdout, "}\n");
    fflush(stdout);

    ptrace(PTRACE_DETACH, tid, 0, 0);
    return 0;
}

int main(int argc, char **argv)
{
    pid_t pid;
    int samples = 3;
    int interval_ms = 200;
    struct map_table maps = {0};
    int sample_no;

    if (argc < 2)
    {
        fprintf(stderr, "usage: %s <pid> [samples] [interval_ms]\n", argv[0]);
        return 2;
    }

    pid = (pid_t)atoi(argv[1]);
    if (argc >= 3) samples = atoi(argv[2]);
    if (argc >= 4) interval_ms = atoi(argv[3]);
    if (pid <= 0) return 2;
    if (samples <= 0) samples = 1;
    if (interval_ms < 0) interval_ms = 0;

    load_maps(pid, &maps);

    for (sample_no = 1; sample_no <= samples; ++sample_no)
    {
        pid_t *tids = NULL;
        size_t i, count = list_threads(pid, &tids);
        if (!count)
        {
            tids = malloc(sizeof(*tids));
            if (!tids)
            {
                perror("malloc tid");
                exit(2);
            }
            tids[0] = pid;
            count = 1;
        }
        for (i = 0; i < count; ++i) sample_thread(pid, tids[i], &maps, sample_no);
        free(tids);
        if (sample_no != samples && interval_ms) usleep((useconds_t)interval_ms * 1000U);
    }

    free(maps.entries);
    return 0;
}
