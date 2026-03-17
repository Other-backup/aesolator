# Host LLVM 22.1.1

Consumer-side notes for the shared host LLVM lane used by `Ae.solator` local
builds on `Termux/Android`, separate from rootfs.

Updated: `2026-03-18`

## Ownership

- canonical CI/release host: `wcp-runtime-lanes`
- canonical workflow: `.github/workflows/ci-host-llvm-toolchain.yml`
- canonical build script: `ci/toolchains/build-host-llvm-android.sh`
- canonical release tag: `host-llvm-22.1.1-latest`

`Ae.solator` must only fetch and use the published toolchain artifact. It must
not carry a second owner-side host-LLVM GitHub Actions lane.

## Purpose

- pin local host compiler tools to `LLVM 22.1.1`
- stop depending on mixed `termux 21.1.8` + NDK host binaries
- prepare a stable host toolchain for future `wine` / runtime builds

## Local Build

```sh
sh /data/data/com.termux/files/home/aesolator/tools/build-host-llvm-toolchain.sh
```

Installs into:

```text
/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux
```

## CI Build

Separate GitHub Actions lane:

- repo: `wcp-runtime-lanes`
- workflow: `.github/workflows/ci-host-llvm-toolchain.yml`
- build script: `ci/toolchains/build-host-llvm-android.sh`
- release tag: `host-llvm-22.1.1-latest`

The CI lane builds an Android ARM64 host-toolchain artifact from source and
publishes it as a separate release asset, so local device work can continue
while the heavy toolchain build runs remotely.

## Export

```sh
. /data/data/com.termux/files/home/aesolator/tools/env-android-local.sh
```

Or hydrate from the release asset:

```sh
export GITHUB_TOKEN=...
sh /data/data/com.termux/files/home/aesolator/tools/fetch-host-llvm-release.sh
```

For private repositories, `tools/fetch-host-llvm-release.sh` uses the GitHub
API directly and expects `GITHUB_TOKEN` or `AEO_GITHUB_TOKEN` in the local
shell environment.

The current consumer fetch path normalizes the published archive layout back to
the local contract path:

```text
/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux
```

If the local LLVM lane exists, the env helper prepends it to `PATH` and exports:

- `CC=clang`
- `CXX=clang++`
- `AR=llvm-ar`
- `RANLIB=llvm-ranlib`
- `STRIP=llvm-strip`
- `LD=ld.lld`
- `NM=llvm-nm`
- `OBJCOPY=llvm-objcopy`
- `OBJDUMP=llvm-objdump`
- `READELF=llvm-readelf`
- `RC=llvm-rc`
- `WINDRES=llvm-windres`
- `DLLTOOL=llvm-dlltool`

## Scope

- host-only toolchain
- not part of `imagefs`
- not packaged into the APK
- intended for local Android/Termux builds and later `wine` compilation

## Device Prep

Current aggressive non-root device prep for long local builds:

```sh
adb shell cmd power set-mode 0
adb shell cmd power set-adaptive-power-saver-enabled false
adb shell cmd power set-fixed-performance-mode-enabled true
adb shell svc power stayon true
adb shell settings put global stay_on_while_plugged_in 7
adb shell settings put global animator_duration_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global window_animation_scale 0
```

For this device class, the current practical default is:

- `CPU cores`: `8`
- `MemTotal`: about `5.8 GiB`
- `SwapTotal`: about `4.2 GiB`
- preferred LLVM compile profile: `JOBS=6`, `LLVM_PARALLEL_LINK_JOBS=1`
