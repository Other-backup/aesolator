# Termux Local Build Runbook

Updated: `2026-03-18`

## Scope

This document records the verified local build path for `aesolator` inside a
Termux ARM64 environment on-device.

## Verified Outcome

- `assembleDebug` completed successfully in Termux.
- APK output:
  `app/build/outputs/apk/debug/app-debug.apk`
- Verified package after install:
  `com.winlator.cmod`
- Verified installed package version after local debug install:
  `versionCode=20`
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

## Local Compatibility Notes

- Stock SDK `cmake` host binary was not usable in Termux ARM64; local build used
  Termux-native `cmake` and `ninja`.
- Stock AGP-hosted `aapt2` Linux binary was not usable in Termux ARM64; local
  build now exports the Gradle property through
  `GRADLE_OPTS=-Dorg.gradle.project.android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
  in `tools/env-android-local.sh`.
- `tools/bootstrap-termux-host.sh` now hydrates Android SDK command-line tools
  automatically on a fresh device and can fetch the shared host LLVM release
  when shell auth is present.
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
- `stripDebugDebugSymbols` attempted to execute desktop `llvm-strip` from the
  NDK host bundle. This failed in Termux, but AGP packaged the libraries
  unstripped and the build still completed successfully.

These host compatibility shims are local environment state, not repository
source-of-truth.

## Known Warnings

- `ndk.dir` in `local.properties` is deprecated, but build still succeeded.
- Native compile emitted regular source warnings in XR/Vulkan code paths.
- `llvm-strip` desktop host binary is not executable in Termux ARM64; packaging
  continued with unstripped native libraries.
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

Verified install command:

```sh
adb -s 10.0.0.1:42363 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

`-d` was required because the device already had a higher `versionCode`
installed (`200265`), while the local debug APK uses `versionCode=20`.

Full pairing/connect helper runbook lives in:
`docs/ADB_WIFI_DEBUG.md`

## Quick Verification

- Device connected over Wi-Fi ADB.
- Package version after install:
  `versionCode=20`
- Launch smoke test succeeded:
  `monkey -p com.winlator.cmod -c android.intent.category.LAUNCHER 1`
