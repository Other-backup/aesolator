# Termux Local Build Runbook

Updated: `2026-03-29`

## Scope

This document records the verified local build path for `aesolator` inside a
Termux ARM64 environment on-device.

## Master Engineering Directive

Local build work follows `docs/MASTER_ENGINEERING_DIRECTIVE.md`.
If a build/config/tooling defect can be repaired in this environment, the
agent must apply the systemic fix, propagate it to related scripts/docs, and
verify instead of stopping at advisory instructions.
For broad build harvests, do not chase a partial tail; wait for the real stop,
then classify and repair the complete log.

## Verified Outcome

- `assembleDebug` completed successfully in Termux.
- APK output:
  `app/build/outputs/apk/debug/app-debug.apk`
- Verified package after install:
  `com.winlator.cmod`
- Verified installed package version after local debug install:
  `versionCode=21`
- Verified install targets:
  - `NTN-LX1` (historical)
  - `RMX3709RU` / `RMX3709` (`SM8475`, current)
- Verified host compiler lane in active local path:
  `LLVM 22.1.1`
- Verified copy/export target used during local passes:
  `/storage/emulated/0/Download/app-debug.apk`

## Local Tooling

- Java:
  `openjdk-17`
- Android SDK root:
  `/data/data/com.termux/files/home/android-sdk`
- Verified SDK components:
  - `platforms;android-34`
  - `build-tools;35.0.0`
  - `platform-tools`
  - `ndk;29.0.14206865`
- Host compiler/tools from the pinned local LLVM lane:
  - `clang`
  - `clang++`
  - `llvm-strip`
  - `llvm-readelf`
  - `ld.lld`
- Bootstrap tools from Termux packages:
  - `cmake`
  - `ninja`
  - `aapt2`
  - `adb`

## Build Command

```sh
sh tools/bootstrap-termux-host.sh
. ./tools/env-android-local.sh
./gradlew --no-daemon assembleDebug
```

Run from repo root:
`/data/data/com.termux/files/home/aesolator`

Authoritative lane rules:

- use the root `./gradlew` only
- do not treat `app/gradlew` as a second authoritative entrypoint
- `local.properties` is bootstrap input, not hidden build truth
- `preBuild` verifies bundled imagefs/runtime assets and generated JNI libs;
  it must not fetch donor archives from the network or rewrite
  `app/src/main/assets`

## Local Compatibility Notes

- Stock SDK `cmake` host binary was not usable in Termux ARM64; local build used
  Termux-native `cmake` and `ninja`.
- Stock AGP-hosted `aapt2` Linux binary was not usable in Termux ARM64; local
  build now exports the Gradle property through
  `GRADLE_OPTS=-Dorg.gradle.project.android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
  in `tools/env-android-local.sh`.
- Root `./gradlew` now auto-wires the same Termux `aapt2` override when the
  local binary exists, so the documented env script is still recommended for
  the full host lane, but no longer mandatory just to avoid the Maven `aapt2`
  crash on bare Termux shells.
- `tools/bootstrap-termux-host.sh` now hydrates Android SDK command-line tools
  automatically on a fresh device and can fetch the shared host LLVM release
  when shell auth is present.
- rootfs donor hydration is no longer part of the authoritative app build
  graph. If donor artifacts need refresh, do it explicitly through repo-side
  helper/release lanes instead of expecting `preBuild` to mutate source assets.
- `tools/env-android-local.sh` also keeps Termux `adb` first in `PATH`, so the
  local shell does not accidentally pick the desktop `platform-tools/adb`
  binary from the SDK.
- `tools/env-android-local.sh` also keeps the pinned host LLVM lane first in
  `PATH`, so local native builds use
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux/bin`
  instead of stale bootstrap compilers.
- Stock NDK host toolchain path assumed desktop `linux-x86_64` execution. Local
  Termux build required local-only host compatibility shims in the installed
  SDK/NDK runtime state so `clang`/`clang++` could use NDK Android sysroot and
  runtime libraries from a Termux-hosted compiler.
- `tools/install-ndk-termux-shims.sh` now installs Termux-hosted wrappers for
  NDK `llvm-strip` and `llvm-objcopy`, so `stripDebugDebugSymbols` no longer
  falls back to an unusable desktop host binary.

These host compatibility shims are local environment state, not repository
source-of-truth.

## Known Warnings

- `ndk.dir` in `local.properties` is deprecated; the local lane now prefers
  resolving the NDK root from `sdk.dir + pinned version` or explicit env.
- Native compile emitted regular source warnings in XR/Vulkan code paths.
- If a pass changes guest helper libraries staged into `imagefs/usr/lib`,
  verify the staged on-device files after install with a clean-session forensic
  capture rather than assuming the APK payload and app-private copy stayed in
  sync.

## Wi-Fi ADB Install

Reusable helper:

```sh
sh tools/adb-wifi-debug.sh connect <device-ip:port>
sh tools/adb-wifi-debug.sh install-debug <device-ip:port>
```

Same-device fallback after one successful wireless-debug connect:

```sh
sh tools/adb-wifi-debug.sh tcpip-loopback <device-ip:port>
sh tools/adb-wifi-debug.sh connect-loopback
sh tools/adb-wifi-debug.sh install-debug 127.0.0.1:5555
```

Verified install command:

```sh
adb -s 10.0.0.1:42363 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

`-d` was required because the device already had a higher `versionCode`
installed (`200265`), while the local debug APK uses `versionCode=21`.

Full pairing/connect helper runbook lives in:
`docs/ADB_WIFI_DEBUG.md`

## Prefix Pack Cache Lane

Repo-side fetch/build helpers for the extra runtime cache lane:

```sh
sh tools/prefix-pack/fetch-cache.sh status
sh tools/prefix-pack/fetch-cache.sh show vcpp_aio
sh tools/prefix-pack/fetch-cache.sh
sh tools/prefix-pack/build-offline-overlay.sh
```

On-device rootfs toolkit details and the audited source policy live in:
`docs/PREFIX_PACK_TOOLKIT.md`

## Quick Verification

- Device connected over Wi-Fi ADB.
- Package version after install:
  `versionCode=21`
- Launch smoke test succeeded:
  `monkey -p com.winlator.cmod -c android.intent.category.LAUNCHER 1`
