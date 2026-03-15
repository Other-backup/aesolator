# GameNative X11 / Renderer / Driver Audit

Updated: `2026-03-15`

## Scope

Full donor inventory pass across:

- `com/winlator/xserver/*`
- `com/winlator/widget/TouchpadView.java`
- `com/winlator/widget/XServerView.java`
- `com/winlator/renderer/*`
- `com/winlator/core/GPUInformation.java`
- `com/winlator/core/GPUHelper.java`
- `com/winlator/core/DXVKHelper.java`
- `com/winlator/core/GeneralComponents.java`
- `com/winlator/xenvironment/components/*`
- `app/gamenative/ui/screen/xserver/XServerScreen.kt`
- `app/src/main/cpp/winlator/*`

## High-Value Donor Findings

### 1. Vulkan / driver probe layer

`GameNative` carries a real `GPUHelper` lane:

- `core/GPUHelper.java`
- `cpp/winlator/gpu_helper.c`

Useful donor value:

- async Vulkan API probe
- device-extension enumeration
- version helpers for policy decisions

Why it matters here:

- better routing for `VKD3D` / `DXVK` / Vortek-like stacks
- cleaner graphics-driver policy than string guessing
- reusable base for future unified Vulkan SDK / wrapper decisions

### 2. XConnector fd hygiene

`GameNative` native `xconnector_epoll.c` adds:

- tracked fd ownership
- safer close discipline
- `RLIMIT_NOFILE` raise path
- ancillary-fd tracking

Why it matters here:

- X11/runtime stacks die from quiet fd churn long before obvious crashes
- our app opens AF_UNIX sockets, eventfd, ancillary fds, SHM, DRI3 handles
- this is a real stability layer, not cosmetic donor noise

### 3. GPU classification helpers

`GameNative` `GPUInformation` carries useful hardware classification:

- `isAdreno6xx`
- `isAdreno8Elite`
- `isTurnipCapable`
- `isAdreno710_720_732`
- GPU card metadata lookup via `gpu_cards.json`

Why it matters here:

- driver defaults
- Turnip sysmem/gmem decisions
- wrapper / SDK policy selection
- adrenotools routing

### 4. Alternate Vulkan renderer lane

`GameNative` exposes a `VortekRendererComponent` plus config plumbing and
launch-time env policy inside `XServerScreen.kt`.

Useful donor value:

- hardware-buffer based alternate renderer path
- explicit `libvulkanPath` handoff
- GPU/API driven wrapper choices

Status in `aesolator`:

- not imported yet
- this is a larger renderer-lane project, not a blind copy candidate
- prerequisite donor pieces are `GPUHelper`, `GPUImage` buffer exposure, and
  stronger component/destination mapping

### 5. Touch gestures

`GameNative` `TouchpadView` contains a large gesture/touchscreen subsystem
backed by `TouchGestureConfig`.

Useful donor value:

- richer touchscreen gesture config
- double-tap / long-press / pan / zoom gesture mapping

Not immediate import material:

- it is tuned more for touchscreen gesture UX than our current desktop-trackpad
  closure problem
- direct import would expand state complexity in the most fragile input area

### 6. Runtime-specific input DLL payloads

`GameNative` `XServerScreen.kt` no longer treats input DLL payload as one flat
archive. It selects different assets for different runtime arches:

- `arm64ec_input_dlls.tzst`
- `x86_64_input_dlls.tzst`

Why it matters here:

- input/bootstrap payload should follow the actual runtime arch, not a generic
  lowest-common-denominator asset
- this is a small, self-contained transfer with low provenance risk
- it strengthens the X11/input lane without importing the donor gesture stack

## Areas Where `aesolator` Is Already Stronger

- forensic logging and issue-capture contract
- contents/source provenance model
- runtime/package metadata discipline
- UI/documentation/roadmap handoff structure
- driver-package metadata richness in `AdrenotoolsManager`
- safe catalog fallback for graphics-extension UI

These donor areas should not replace current `aesolator` source-of-truth:

- package/source provenance
- contents lane semantics
- forensic/reporting contract
- container/runtime ownership model

## Import Decision Matrix

### Import Now

- `GPUHelper.java`
- `cpp/winlator/gpu_helper.c`
- `GPUInformation` hardware classification helpers
- `GPUImage.getHardwareBufferPtr()`
- `xconnector_epoll` fd-tracking / rlimit hygiene
- arch-specific input DLL asset selection and staged donor payloads
- `VortekRendererComponent` foundation
- donor APK-native renderer libs `libvirglrenderer.so`, `libvortekrenderer.so`

### Adapt Later

- `DXVKHelper` ideas for explicit env shaping
- `GeneralComponents` install-destination abstraction
- launch-time graphics policy from `XServerScreen.kt`

### Do Not Blind-Copy

- full `TouchGestureConfig` stack into current desktop-trackpad lane
- donor contents/release routing as source-of-truth
- donor UI shell or launcher architecture
- donor package provenance semantics
- donor binary-only `libwinlator_11.so`

## Immediate Imports Added To `aesolator`

This audit pass already injected the following donor-derived building blocks
into the local tree without switching app contracts:

- `app/src/main/java/com/winlator/cmod/core/GPUHelper.java`
- `app/src/main/cpp/winlator/gpu_helper.c`
- `app/src/main/java/com/winlator/cmod/core/GPUInformation.java`
  classification and `gpu_cards.json` lookup helpers
- `app/src/main/java/com/winlator/cmod/renderer/GPUImage.java`
  `getHardwareBufferPtr()`
- `app/src/main/java/com/winlator/cmod/xconnector/XConnectorEpoll.java`
  `RLIMIT_NOFILE` bootstrap hook
- `app/src/main/cpp/winlator/xconnector_epoll.c`
  tracked close / ancillary-fd tracking / rlimit support
- `app/src/main/assets/arm64ec_input_dlls.tzst`
- `app/src/main/assets/x86_64_input_dlls.tzst`
- `app/src/main/java/com/winlator/cmod/xenvironment/components/VortekRendererComponent.java`
- `app/src/main/java/com/winlator/cmod/contentdialog/VortekConfigDialog.java`
- `app/src/main/jniLibs/arm64-v8a/libvirglrenderer.so`
- `app/src/main/jniLibs/arm64-v8a/libvortekrenderer.so`
- `app/src/main/java/com/winlator/cmod/inputcontrols/ControllerManager.java`
- `app/src/main/java/com/winlator/cmod/inputcontrols/TouchMouse.java`
- `app/src/main/java/com/winlator/cmod/xserver/XKeycode.kt`
- `XServerDisplayActivity.extractInputDLLs()` now chooses donor-style
  arch-specific input payloads when the runtime arch and bundled asset match

Binary-native conclusion:

- donor `libwinlator_11.so` exports the older donor native
  `XInputStream` / `XOutputStream` path
- local `cmod` already supersedes that lane with source-backed Java stream
  classes plus richer native `GPUInformation`
- keep `libwinlator_11.so` as donor reference only unless a dedicated source
  reconstruction pass is opened
- dedicated audit note:
  [GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md](/data/data/com.termux/files/home/aesolator/docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md)

## Next Import Queue

1. Wire `GPUHelper` into graphics-driver policy and Vulkan SDK package routing.
2. Decide whether `GeneralComponents` should become a narrow internal helper
   for component destinations or stay donor-only reference material.
3. audit whether the input DLL lane needs broader runtime-family routing than
   `arm64ec` / `x86_64` before compile
4. Wire staged `VortekRendererComponent` into a real runtime route only after
   the first honest compile proves the batch is mechanically healthy.
5. Revisit touchscreen gesture borrowing only after desktop cursor/click
   closure is stable again.
