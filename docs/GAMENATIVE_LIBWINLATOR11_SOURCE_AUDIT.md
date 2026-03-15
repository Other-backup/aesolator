# GameNative libwinlator_11 Source Audit

Updated: `2026-03-15`

## Scope

This note tracks what was actually transferable from donor
`libwinlator_11.so` into `Ae.solator` as source-backed work, and what remains
binary-only donor territory.

The point is not to glorify the donor binary. The point is to avoid missing any
real runtime/X11/renderer value while still refusing a blind opaque drop-in.

## Inputs Inspected

- donor APK-native libraries
  - `GameNative/app/src/main/jniLibs/arm64-v8a/libwinlator_11.so`
  - `GameNative/app/src/main/jniLibs/arm64-v8a/libwinlator.so`
- donor source-backed native/runtime files
  - `GameNative/app/src/main/cpp/winlator/xconnector_epoll.c`
  - `GameNative/app/src/main/cpp/winlator/gpu_helper.c`
  - `GameNative/app/src/main/cpp/winlator/gpu_image.c`
  - `GameNative/app/src/main/cpp/extras/gpu_image.c`
  - donor Java/Kotlin X11 / renderer / launcher files under
    `GameNative/app/src/main/java/com/winlator`
- local source-backed target
  - `aesolator/app/src/main/java/com/winlator/cmod/*`
  - `aesolator/app/src/main/cpp/winlator/*`

## What Was Already Reconstructible And Is Now Staged

These donor strengths are no longer just "hidden in libwinlator_11.so".
They are now represented in the local source tree or staged runtime payloads:

- `xconnector_epoll` fd ownership / ancillary-fd hygiene
- Vulkan/API probe foundation via `GPUHelper.java` + `gpu_helper.c`
- renderer hardware-buffer accessor through `GPUImage`
- donor-style Unix socket constants for Steam/Vortek runtime paths
- donor bionic helper-libs staged locally:
  `libevshim.so`, `libdummyvk.so`
- donor renderer helper-libs staged locally:
  `libvirglrenderer.so`, `libvortekrenderer.so`
- donor Vortek component/config foundation
- donor arch-specific input payload staging:
  `arm64ec_input_dlls.tzst`, `x86_64_input_dlls.tzst`
- donor `XInputStream` / `XOutputStream` call shape already matched by local
  source-backed classes
- donor Java/Kotlin filename parity under `com/winlator/*` is now fully staged
  locally (`MISSING_COUNT 0` in the latest inventory pass)

## What `libwinlator_11.so` Still Does Not Authorize Us To Blind-Copy

- opaque JNI bridges with no documented local ownership
- donor-native stream/export assumptions tied to the older donor package graph
- binary-only behavior that cannot be reviewed, diffed, or preserved through a
  normal `Ae.solator` source build

The important example is the donor's older native stream bridge. Local
`Ae.solator` already keeps `XInputStream` / `XOutputStream` in source form, so
dropping the donor library in would create false parity rather than real
maintainable parity.

## Decision

`libwinlator_11.so` stays a donor audit reference, not a source-of-truth
artifact.

Current rule:

- transfer reconstructible behavior into local source or staged runtime assets
- document every adopted donor-native capability here
- do not replace local `libwinlator.so` or inject donor `libwinlator_11.so`
  unless a future pass reconstructs and reviews the missing behavior in source
  form first

## Honest State Before First Compile

For donor `libwinlator_11.so`, the reconstructible part of the lane is now
mostly exhausted.

What remains before compile is not "import more binary". What remains is:

- keep the local source-backed lane coherent
- compile the batch honestly
- validate that staged donor source/native foundations actually link and behave
  correctly
- only reopen the donor binary question if compile/runtime evidence points to a
  specific missing behavior that still has no source-backed counterpart
