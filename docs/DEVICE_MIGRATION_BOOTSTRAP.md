# Device Migration Bootstrap

Updated: `2026-03-18`

This document is the repo-tracked bootstrap path for moving `Ae.solator`
development onto a new `Termux/Android` device without reopening old setup
tails.

## Master Engineering Directive

Migration/bootstrap work follows `docs/MASTER_ENGINEERING_DIRECTIVE.md`.
If the environment permits direct remediation, fix broken paths, configs,
scripts, docs, and verification steps automatically rather than leaving manual
instructions as the primary result.
Process changes must be propagated to `AGENTS.md`, `README.md`, this bootstrap
doc, the roadmap, and the reflective journal.

## Target Layout

- workspace root: `/data/data/com.termux/files/home`
- app repo: `/data/data/com.termux/files/home/aesolator`
- CI/runtime repo: `/data/data/com.termux/files/home/wcp-runtime-lanes`
- Android SDK root: `/data/data/com.termux/files/home/android-sdk`
- host LLVM root:
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux`
- fresh APK copy:
  `/storage/emulated/0/Download/app-debug.apk`
- app-owned external forensic logs:
  `/storage/emulated/0/Ae.solator/logs`

## Main Rules

- work in `main`, not side branches
- push both repos before moving devices
- keep `AGENTS.md`, roadmap, and reflective journal current
- if the user explicitly activates approval-gated review mode, follow
  `docs/CODEX_OPERATING_CONTRACT.md`
- without `adb`, trust app-owned forensic files before shell `logcat`
- host `LLVM 22.1.1` is owned by `wcp-runtime-lanes`, consumed by
  `aesolator`

## Bootstrap Order

1. Clone or restore both repos into the target layout.
2. Run `tools/bootstrap-termux-host.sh` from `aesolator`.
3. Source `tools/env-android-local.sh`.
4. Verify that `local.properties`, SDK paths, and host LLVM paths are live.
5. Continue from the latest roadmap/journal state, external forensic logs, and
   `docs/CODEX_OPERATING_CONTRACT.md`.
6. Build from the repo root only:
   `./gradlew` is authoritative, `app/gradlew` is not a separate lane.

## Minimal Commands

```sh
cd /data/data/com.termux/files/home/aesolator
sh tools/bootstrap-termux-host.sh
. tools/env-android-local.sh
git branch --show-current
git status --short
```

Expected branch after migration:

```text
main
```

## What The Bootstrap Script Does

- installs the baseline `Termux` packages used by the current build lane
- creates Android SDK directories if missing
- installs Android SDK command-line tools if missing
- installs Android SDK packages through `sdkmanager`
- writes `local.properties`
- fetches the shared `LLVM 22.1.1` host toolchain release if GitHub auth is
  present in the shell

## SDK / NDK Expectations

Pinned working versions:

- Android platform: `android-34`
- build-tools: `35.0.0`
- NDK: `29.0.14206865`

`local.properties` should resolve to bootstrap inputs similar to:

```properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
cmake.dir=/data/data/com.termux/files/usr
```

`ndk.dir` may still appear on older devices, but the current root lane should
not require it when `sdk.dir` and the pinned NDK version are enough to resolve
the NDK root.

## Host LLVM

Consumer-side fetch path:

```sh
export GITHUB_TOKEN=...
sh /data/data/com.termux/files/home/aesolator/tools/fetch-host-llvm-release.sh
```

Ownership:

- workflow owner repo: `wcp-runtime-lanes`
- release tag: `host-llvm-22.1.1-latest`

Once hydrated, the local env helper should expose:

- `clang`
- `clang++`
- `llvm-strip`
- `ld.lld`
- `llvm-readelf`

from:

```text
/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux/bin
```

## Current Resume Point

The old wrapper/sysvshm blocker and the donor-`RUNPATH` payload blocker are
both closed locally; the active failure is now narrower and later in the
X11-driver attach path:

- fresh clean-session proof reaches runtime streams and no longer shows
  `libandroid_shmget` / `Found no drivers`
- donor absolute `RUNPATH` / `RPATH` is now sanitized both in installed
  runtime roots and in `imagefs/usr/lib`, and the imagefs sanitizer now writes
  `.winlator/.elf_runpath_sanitizer_version=2` so the app stops rescanning the
  full tree every launch
- current surviving chain is:
  `winex11.drv` `PROCESS_ATTACH` `RETURN 0`
  -> `Initialization of L"winex11.drv" failed`
  -> `winewayland.drv` `status=c0000135`
  -> `nodrv_CreateWindow`
  -> `XSERVER_EXIT_REQUESTED` / self-exit
- current local fix lane now does three things:
  1. stage a source-built `libandroid-sysvshm.so` compatibility bridge with
     both `shm*` and `libandroid_shm*` exports
  2. sanitize donor absolute `RUNPATH` / `RPATH` in-place during runtime
     install and repair
  3. sanitize the rootfs `imagefs/usr/lib` closure and skip short ASCII
     linker-script placeholders like `librt.so` instead of misclassifying them
     as failed ELF rewrites
- next proof target is no longer generic payload hygiene; it is targeted
  instrumentation or static dependency closure around the `winex11.drv`
  attach-time failure on `Container-1`

Primary forensic sources on-device:

- `/storage/emulated/0/Ae.solator/logs/forensics/*.jsonl`
- `/storage/emulated/0/Ae.solator/logs/fatal_crash_*.txt`
- `/storage/emulated/0/Ae.solator/logs/wine_loader_*.txt`
- `/storage/emulated/0/Ae.solator/logs/box64_*.txt`
- `/storage/emulated/0/Ae.solator/logs/fex_runtime_*.txt`

## Clean Start Checklist

- `git status --short` is empty in both repos
- `git branch --show-current` is `main`
- `tools/env-android-local.sh` prints the expected SDK and LLVM roots
- `./gradlew :app:help --configuration-cache` succeeds from repo root
- `/storage/emulated/0/Download/app-debug.apk` exists after the first local
  build
- app-owned forensic directory is writable:
  `/storage/emulated/0/Ae.solator/logs`

## Related Docs

- `docs/CODEX_OPERATING_CONTRACT.md`
- `docs/ADB_WIFI_DEBUG.md`
- `docs/HOST_LLVM_22_1_1_TOOLCHAIN.md`
- `docs/TERMUX_LOCAL_BUILD.md`
- `docs/ROOTFS_RUNTIME_STATIC_AUDIT.md`
- `docs/PAYLOAD_ROOTFS_STATIC_INVENTORY.md`
- `docs/SECOND_DEV_ROADMAP.md`
- `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`
