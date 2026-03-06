# Round 15 Matrix: `Eden-Android-9d2341eaea-standard.apk`

Date: `2026-03-05`  
Round state: `closed`

## Round Scope

Donor source:
- `/home/mikhail/Загрузки/Eden-Android-9d2341eaea-standard.apk`

Primary donor anchors:
- APK package metadata (`aapt dump badging`)
- native payload list (`unzip -l`) including `lib/arm64-v8a/libVkLayer_khronos_validation.so`
- manifest capability markers (Vulkan-required runtime app profile)

## Transfer Matrix

| Signal Cluster | Donor Anchor | Aeolator Target | Status | Decision |
|---|---|---|---|---|
| Vulkan validation layer availability in Android runtime package | APK file list (`libVkLayer_khronos_validation.so`) | shortcut/runtime debug lane | `integrated` | `integrate` |
| Vulkan-focused runtime diagnostics mindset | manifest + native payload profile | forensic-visible env contract | `integrated` | `integrate` |
| Donor emulator core/runtime stack | APK native libs (`libyuzu-android.so` etc.) | app-tree | `rejected` | `reject_with_rationale` |
| Donor UI and services architecture | `classes.dex`, manifest activities/services | app-tree | `rejected` | `reject_with_rationale` |

## Progress Log

### 2026-03-05 / Pass 1

- Added `Enable Vulkan Validation Layer` control to upscaler/framegen settings.
- Persisted shortcut key:
  - `vulkanValidationLayer`.
- Runtime lane integration in `XServerDisplayActivity`:
  - `AERO_VK_VALIDATION_LAYER` (`0/1`);
  - `VK_INSTANCE_LAYERS=VK_LAYER_KHRONOS_validation` when enabled.
- Forensic lane update:
  - `UPSCALER_ROUTE_APPLIED` includes `vk_validation_layer`.

### 2026-03-06 / Pass 2

- Hardened env merge path to remove `VK_INSTANCE_LAYERS` collision between upscaler and forensic lanes.
- Forensic bridge now appends to already prepared layer list instead of replacing it.
- Upscaler validation layer activation is now gated by active backend (`Backend != Off`), eliminating false-positive validation enables.

### 2026-03-06 / Pass 3

- Added strict Vulkan SDK presence guard for validation-layer request path:
  - validation layer request now requires installed Vulkan SDK lane (`AERO_VULKAN_SDK_PROFILE_COUNT > 0`);
  - added guard env signal: `AERO_VK_VALIDATION_GUARD=vulkan_sdk_missing`;
  - forensic event `UPSCALER_ROUTE_APPLIED` extended with:
    - `vk_validation_layer_requested`;
    - `vk_validation_layer`;
    - `vk_validation_guard`.
- Closure gates re-run on app-tree contract:
  - compile gate (`:app:compileDebugJavaWithJavac`) passed;
  - no env-layer overwrite on `VK_INSTANCE_LAYERS`;
  - fallback route behavior preserved when validation request is blocked by guard.

### 2026-03-06 / Pass 4 (IDE/HEX/ASM)

- Completed static reverse pass on Eden donor:
  - DEX strings (`classes.dex`) for driver resolver/logging anchors;
  - ELF symbol inspection for hook lane (`libhook_impl.so`, `libfile_redirect_hook.so`, `libgsl_alloc_hook.so`);
  - AArch64 disassembly anchors (`hook_android_dlopen_ext`, `hook_fopen`) via `llvm-objdump`.
- Added explicit request/effective split for validation lane:
  - `AERO_VK_VALIDATION_REQUESTED`;
  - `AERO_VK_VALIDATION_LAYER` (effective);
  - forensic enrichment with `vulkan_sdk_profile_count`.
- Evidence and transfer boundaries recorded in:
  - `docs/rounds/R15_EDENAPK_HEX_ASM_NOTES.md`.

## Round Closure

Round 15 is marked `closed` for app-tree and contract scope.
