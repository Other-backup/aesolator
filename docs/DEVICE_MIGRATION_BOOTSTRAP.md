# Device Migration Bootstrap

Updated: `2026-03-18`

This document is the repo-tracked bootstrap path for moving `Ae.solator`
development onto a new `Termux/Android` device without reopening old setup
tails.

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
- without `adb`, trust app-owned forensic files before shell `logcat`
- host `LLVM 22.1.1` is owned by `wcp-runtime-lanes`, consumed by
  `aesolator`

## Bootstrap Order

1. Clone or restore both repos into the target layout.
2. Run `tools/bootstrap-termux-host.sh` from `aesolator`.
3. Source `tools/env-android-local.sh`.
4. Verify that `local.properties`, SDK paths, and host LLVM paths are live.
5. Continue from the latest roadmap/journal state and external forensic logs.

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

`local.properties` should resolve to:

```properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
ndk.dir=/data/data/com.termux/files/home/android-sdk/ndk/29.0.14206865
cmake.dir=/data/data/com.termux/files/usr
```

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

The active app blocker is still early native bootstrap, not guest runtime:

- last good checkpoints reach `XSERVER_BOOTSTRAP_XSERVER_BEGIN`
- failure is still before `XSERVER_BOOTSTRAP_XSERVER_READY`

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
- `/storage/emulated/0/Download/app-debug.apk` exists after the first local
  build
- app-owned forensic directory is writable:
  `/storage/emulated/0/Ae.solator/logs`

## Related Docs

- `docs/ADB_WIFI_DEBUG.md`
- `docs/HOST_LLVM_22_1_1_TOOLCHAIN.md`
- `docs/TERMUX_LOCAL_BUILD.md`
- `docs/ROOTFS_RUNTIME_STATIC_AUDIT.md`
- `docs/PAYLOAD_ROOTFS_STATIC_INVENTORY.md`
- `docs/SECOND_DEV_ROADMAP.md`
- `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`
