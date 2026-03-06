# Round 15: Eden APK IDE/HEX/ASM Notes

Date: `2026-03-06`  
Scope: static reverse pass (APK metadata + DEX strings + ELF symbol/disasm anchors)

## Artifact

- `/home/mikhail/Загрузки/Eden-Android-9d2341eaea-standard.apk`
- extracted workspace:
  - `/home/mikhail/work/eden-r15/classes.dex`
  - `/home/mikhail/work/eden-r15/lib/arm64-v8a/*.so`

## Toolchain used

- `/home/mikhail/android-sdk-local/build-tools/35.0.0/aapt`
- `/home/mikhail/android-sdk-local/build-tools/35.0.0/dexdump`
- `strings`
- `readelf`
- `llvm-objdump`

## Key static findings

1. Runtime model
- APK package: `dev.eden.eden_emulator.nightly`
- App version label: `7f7d9d6`
- Vulkan feature required in manifest (`android.hardware.vulkan.version`).

2. Native payload structure
- `libVkLayer_khronos_validation.so` present (`~23M`).
- Hook lane split:
  - `libhook_impl.so`
  - `libfile_redirect_hook.so`
  - `libgsl_alloc_hook.so`
  - `libmain_hook.so`
- Main runtime core: `libyuzu-android.so`.

3. Symbol-level hook anchors (ELF)
- `libhook_impl.so` exports:
  - `init_hook_param`
  - `hook_android_dlopen_ext`
  - `hook_android_load_sphal_library`
  - `hook_fopen`
  - `hook_gsl_memory_alloc_pure_64`
  - `hook_gsl_memory_free_pure`
- `libfile_redirect_hook.so` links to `hook_fopen`.
- `libgsl_alloc_hook.so` links to `hook_gsl_memory_alloc_pure_64` / `hook_gsl_memory_free_pure`.

4. ASM-level anchor (AArch64)
- `llvm-objdump` on `hook_android_dlopen_ext` confirms:
  - namespace-aware `android_dlopen_ext` routing path;
  - fallback branches and log anchors;
  - dynamic symbol binding (`dlsym`) for hook chain.
- `llvm-objdump` on `hook_fopen` confirms:
  - path-prefix checks;
  - redirect/passthrough branch split;
  - stack-check guarded return path.

5. DEX/string anchors relevant to transfer
- Driver resolver/manager model present:
  - `DriverResolver`, `DriverManagerFragment`, `DriverRepo`, `ResolvedDriver`.
- Driver-source strings include Turnip community feeds (`KIMCHI Turnip`, `freedreno_turnip-CI`).
- GPU diagnostics anchors:
  - `GPU_LOG_VULKAN_CALLS`
  - `GPU_LOG_DRIVER_DEBUG`
  - `SHOW_FPS`
  - `Vulkan API Version`.

## Transfer decision for Aeolator (R15 boundary)

Integrated into app-tree contract:
- Vulkan validation lane hardening:
  - request/effective separation (`AERO_VK_VALIDATION_REQUESTED` vs `AERO_VK_VALIDATION_LAYER`);
  - strict SDK-availability guard (`AERO_VK_VALIDATION_GUARD=vulkan_sdk_missing`);
  - forensic visibility (`UPSCALER_ROUTE_APPLIED` includes request/effective/guard/sdk-count fields).

Rejected by boundary:
- Direct reuse/transplant of Eden native hook binaries and emulator-specific runtime core.
- Direct copy of Eden DEX/UI architecture.

## Result

R15 app-tree extraction path is now backed by IDE/HEX/ASM-level evidence; no blind metadata-only transfer remains in this donor round.
