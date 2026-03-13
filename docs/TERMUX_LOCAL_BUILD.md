# Termux Local Build Runbook

Updated: `2026-03-14`

## Scope

This document records the verified local build path for `aesolator` inside a
Termux ARM64 environment on-device.

## Verified Outcome

- `assembleDebug` completed successfully in Termux.
- APK output:
  `app/build/outputs/apk/debug/app-debug.apk`
- Verified install target:
  `NTN-LX1`
- Verified package after install:
  `com.winlator.cmod`
- Verified installed package version after local debug install:
  `versionCode=20`

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
- Host tools from Termux packages:
  - `cmake`
  - `ninja`
  - `clang`
  - `aapt2`
  - `adb`

## Build Command

```sh
env \
  JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk \
  ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
  ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
  PATH=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk/bin:/data/data/com.termux/files/home/android-sdk/platform-tools:/data/data/com.termux/files/home/android-sdk/cmdline-tools/latest/bin:/data/data/com.termux/files/home/android-sdk/cmake/3.22.1/bin:/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets \
  ./gradlew --no-daemon --stacktrace --info \
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
  assembleDebug
```

Run from repo root:
`/data/data/com.termux/files/home/aesolator`

## Local Compatibility Notes

- Stock SDK `cmake` host binary was not usable in Termux ARM64; local build used
  Termux-native `cmake` and `ninja`.
- Stock AGP-hosted `aapt2` Linux binary was not usable in Termux ARM64; local
  build required `-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`.
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

## Wi-Fi ADB Install

Verified install command:

```sh
adb -s 10.0.0.1:42363 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

`-d` was required because the device already had a higher `versionCode`
installed (`200265`), while the local debug APK uses `versionCode=20`.

## Quick Verification

- Device connected over Wi-Fi ADB.
- Package version after install:
  `versionCode=20`
- Launch smoke test succeeded:
  `monkey -p com.winlator.cmod -c android.intent.category.LAUNCHER 1`
