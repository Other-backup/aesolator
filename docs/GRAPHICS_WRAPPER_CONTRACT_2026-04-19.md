# Graphics Wrapper Contract 2026-04-19

## Evidence

- VirGL upstream source: `https://gitlab.freedesktop.org/virgl/virglrenderer`
- Mesa upstream source: `https://gitlab.freedesktop.org/mesa/mesa`
- Fresh official Mesa `main` observed on 2026-04-19:
  `8736d1a9a6b323fc2c6c1bdb5d6a445f1cef1575`
- Fresh official VirGL `main` observed on 2026-04-19:
  `4e19e9992bc2fe8686fe844666626e03d8ec6e61`
- Release-payload rule: do not use Mesa or VirGL `main` as the default shipped payload source.
- Selected Mesa shipped-default source on 2026-04-19:
  `origin/staging/26.1` at `7f6d2b9ce9e299883e3581ce1e9955eb9ab39e10`,
  newer than `origin/26.1` while still staying on the official pre-release line.
- Latest VirGL public non-main MR observed on 2026-04-19:
  `uprev-mesa`, MR `1612`, SHA `f81bc77f2b32a3042f62e0ee6ee5f2c57ffc4cc6`,
  updated `2026-04-19T00:27:43.945Z`; classify as CI / Mesa-uprev evidence,
  not runtime renderer payload by itself.
- Selected VirGL runtime-significant non-main candidate on 2026-04-19:
  `free_sync_thread`, MR `1613`, SHA `6bfe93b5c4d6aaf0f50888f80d751ba3bfbae854`,
  title `vrend: free sync thread before fences on init failure`,
  updated `2026-04-17T16:38:19.384Z`.
- Selected VirGL stable base on 2026-04-19:
  release tag `virglrenderer-1.3.0` / `1.3.0`,
  commit `ca50e008863837e094747a69974dde3ae148aeaa`,
  date `2026-02-10T13:43:59Z`.
- `origin/26.1` / `mesa-26.1.0-rc1` baseline observed:
  `0108eba5edb46af3704a92805224363fe81ac0b2`.
- Raw local cache:
  `/data/data/com.termux/files/home/.cache/donors/virglrenderer-main`
- Raw local cache:
  `/data/data/com.termux/files/home/.cache/donors/mesa-main`
- Raw source ledger:
  `/data/data/com.termux/files/home/.cache/graphics-research/virgl/virglrenderer-ls-remote-20260419.tsv`
- Raw non-main upstream ledger:
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/nonmain-upstream-candidates-20260419.txt`
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/mesa-ref-ranking-20260419Trefrefresh2.txt`
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/virgl-open-mrs-ranking-20260419Trefrefresh3.tsv`
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/virgl-runtime-mr-decision-20260419Tgitlabapi2.tsv`
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/virgl-official-branches-20260419Tresume2.tsv`
- Official docs cached:
  `/data/data/com.termux/files/home/.cache/graphics-research/virgl/mesa-virgl-doc-20260419.html`
  `/data/data/com.termux/files/home/.cache/graphics-research/virgl/qemu-virtio-gpu-doc-20260419.html`
- Android Vulkan HAL truth:
  `https://source.android.com/docs/core/graphics/implement-vulkan`
  and
  `https://android.googlesource.com/platform/frameworks/native/+/master/vulkan/include/hardware/hwvulkan.h`
- Mali/PanVK truth:
  `https://docs.mesa3d.org/drivers/panfrost.html`
- Wrapper donor truth:
  `https://github.com/leegao/bionic-vulkan-wrapper`
  and
  `https://github.com/leegao/bionic-vulkan-wrapper/releases`
- Source-build script:
  `/data/data/com.termux/files/home/aesolator/tools/build-mesa-staging-graphics.sh`

## Official VirGL Branch Census

Official upstream branch truth on 2026-04-19:

- `main` is the only protected, regularly moving official head.
- `master` is stale at `2023-06-30`.
- `branch-0.9.1` is stale at `2021-04-20`.
- other official heads are topic branches, not a maintained release line:
  `tintou/revert`, `tintou/remove-shader-version-compute`,
  `tintou/ovr_multiview`, `tintou/u_format_struct`, `tintou/gles-rw`.

So the honest non-main policy for `virglrenderer` is still:

- stable payload base: latest release tag
- freshest beta overlay: official MR branch with runtime-significant logic

There is no Mesa-style `staging/<version>` official branch on the current
VirGL upstream.

## Owner Model

- `Wrapper` remains the Adreno Vulkan/OpenGL package lane.
- `Turnip` owns Vulkan calls for the Adreno lane.
- `Gallium` owns the Mesa OpenGL implementation family.
- On Android/KGSL Adreno, official Mesa source states that the Freedreno Gallium driver
  does not support KGSL and that Zink with Turnip should be used on those systems.
- The Adreno OpenGL lane must expose the real `zink` / `freedreno` choice, but the
  product default for KGSL remains `zink` over Turnip until a device-specific
  non-KGSL Freedreno Gallium surface is proven.
- `Zink` is therefore the safe default OpenGL-over-Vulkan Gallium implementation for
  the smartphone KGSL contract, not a cosmetic alias.
- `Vortek + Gladio` is one MediaTek wrapper family:
  Vortek owns Vulkan calls, Gladio owns OpenGL/GLES calls, and the runtime dispatcher selects the active provider from the wrapper route.
- `Vortek` Vulkan source is not a single class:
  Android stock Vulkan is a HAL-loaded vendor driver (`libvulkan.so` -> `/vendor/lib[64]/hw/vulkan.<soc>.so`);
  imported Winlator-style wrapper libraries are a userspace-wrapper class;
  Mesa/PanVK is a separate experimental transport class whose Android bits do
  not erase the remaining DRM/render-node ownership inside upstream Panfrost.
- `VirGL` is a separate universal virtualization wrapper family:
  it uses the qemu/virtio-gpu virglrenderer model and must not be hidden inside the Adreno Mesa wrapper or MediaTek wrapper family.

## Transfer Constraint

The current Ae.solator native tree embeds a custom Android CMake/JNI virgl server surface under
`app/src/main/cpp/virglrenderer`. Upstream `virglrenderer` is not a drop-in replacement:
it uses Meson, libepoxy/libdrm feature probes, split `src/vrend`, `venus`, `drm`, `proxy`, and
winsys modules. A safe source update requires a dedicated port ledger, not blind file replacement.
For official-source freshness policy, `virglrenderer` must be treated as a
two-layer input while non-main release branches do not exist:

- stable payload base: latest official release tag `virglrenderer-1.3.0`
- freshest beta overlay candidate: runtime-significant official MR `1613`

That is the honest non-main path. `origin/main` remains audit evidence only.

The shipped `virgl-23.1.9.tzst` and `zink-22.2.5.tzst` rootfs assets are old
glibc-linked Mesa payloads. They must not be described as fresh or bionic-native
until a new source-built payload is produced, packaged, and linked against the
declared runtime contract.

## Current Source-Built Artifacts

- Zink package:
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419T144812Z-zink-android-7f6d2b9ce9e2/package/zink-android-mesa-26.1-staging-7f6d2b9ce9e2.tar.zst`
  sha256 `ebf45c5c0208fe4072006acc3135e052831a07aea0b0c8ed5e77ab9bf8785b8b`
- Turnip package:
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419T150141Z-turnip-android-7f6d2b9ce9e2/package/turnip-android-mesa-26.1-staging-7f6d2b9ce9e2.tar.zst`
  sha256 `c4e01efed131a3a023c3c4e2119e0826b9b3f95ed4007696cbea004ee7e74ad1`
- VirGL Gallium package:
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419T151524Z-virgl-gallium-android-7f6d2b9ce9e2/package/virgl-gallium-android-mesa-26.1-staging-7f6d2b9ce9e2.tar.zst`
  sha256 `cc24203f0a62a2e86a925d690b92dc20b5c35ade45984281073045ccdac2ff66`
- VirGL Gallium runtime support provenance:
  `libandroid-sysvshm.so` is source-built from
  `freewine11/android/android_sysvshm`,
  `libdrm.so` is source-built from official `libdrm-2.4.131`,
  `libc++_shared.so` is copied as runtime support from Termux.
- Provenance check on those three packages currently passes the
  `libc.so.6|ld-linux|/data/data/com.termux/files/usr/lib` grep gate.

## Mesa Mali Downstream Fork Lane

Current downstream rule:

- Do not represent Panfrost alone as the complete Mali product lane.
- The Mesa/Mali fork surface is split into:
  - Panfrost Gallium for OpenGL/GLES on supported Mali generations.
  - Lima Gallium for older Utgard-class coverage when Mesa still exposes it.
  - PanVK for Vulkan on supported Mali generations.
  - Zink/softpipe as controlled fallback or diagnostic routes, not as a fake
    claim that Mali Vulkan is available.
  - Vortek-derived policy glue for app compatibility: extension exposure,
    disabled-extension masks, Vulkan API cap, memory/resource knobs,
    runtime-source forensics, custom package metadata, and Android transport
    classification.
- The Vortek-derived part must live as downstream policy and packaging glue
  around Mesa/PanVK until a specific Mesa upstream owner accepts equivalent
  abstractions. It must not be blindly mixed into Panfrost compiler, kmod, or
  WSI internals as opaque app folklore.
- The valid Mesa-side insertion points for the wrapper contract are driver
  identity, startup diagnostics, route/profile/transport policy intake, and
  package-facing metadata surfaces in `panvk`, `panfrost`, and `lima`.
  That keeps `Vortek/Gladio` tightly bound to the AeMali lane without lying
  about kernel/HAL ownership or smuggling wrapper folklore into unrelated
  compiler internals.
- `tools/build-mesa-staging-graphics.sh` now exposes:
  - `panvk-android`: Vulkan/PanVK Mali package lane.
  - `mali-gallium-android`: OpenGL/GLES Mali Gallium package lane.
- `panvk-android` packages `meta.json` with
  `driverKind=aemali-panvk`, `policyEnv=AEMALI_DRIVER=1`,
  `transport=drm-render-node-experimental`, and `requiresRenderNode=true`,
  because current Mesa/PanVK source still carries DRM/render-node ownership
  even when Android stubs and HAL-facing symbols are present.
- `mali-gallium-android` packages `meta.json` with
  `driverKind=aemali-gallium`, `providerLane=aemali-gallium`,
  `graphicsStackProfile=aemali-universal`, and explicit Gallium/OpenGL
  transport truth. This is the OpenGL/GLES half of the AeMali Mesa lane and
  it must stay separate from wrapper-only `Gladio` identity.
  On the Android lane the bundle-owned probe/root library is `usr/lib/libEGL.so`
  and the asset must carry `libgallium_dri.so`; `usr/lib/libGL.so*` remains a
  base-rootfs ownership surface and must not be used as the AeMali Gallium
  bundled extraction probe.
- `AeMali Gallium` is not allowed to stay a bundled-only demo lane.
  It must support the same package lifecycle class as the other app-visible
  graphics owners:
  bundled asset discovery, custom package import/remove, root-overlay deploy,
  route degradation reporting, display-version truth, and metadata/env parity.
  If `Vortek`, `Gladio`, or `VirGL` can carry `meta.json` and custom overlays,
  `AeMali Gallium` must not be weaker.
- `panvk-android` package identity is `providerLane=aemali-panvk`, not a
  `Vortek`-mixed label. `Vortek` remains wrapper/orchestration ownership and
  must not be conflated with the AeMali Mesa route in package metadata.

AeMali is now a real downstream Mesa patchset, not only an app-side wrapper
label:

- patchset owner:
  `/data/data/com.termux/files/home/aesolator/patches/mesa/aemali/`
- current patch:
  `0001-panvk-aemali-policy-surface.patch`
- current patch:
  `0002-panvk-aemali-api-truth-surface.patch`
- current patch:
  `0003-gallium-aemali-policy-surface.patch`
- patched Mesa surfaces:
  `src/panfrost/vulkan/meson.build`,
  `src/panfrost/vulkan/panvk_instance.c`,
  `src/panfrost/vulkan/panvk_vX_physical_device.c`,
  `src/gallium/drivers/panfrost/{meson.build,pan_screen.c}`,
  `src/gallium/drivers/lima/{meson.build,lima_screen.c,lima_screen.h}`,
  plus new
  `src/panfrost/vulkan/panvk_aemali_policy.c/h`,
  `src/gallium/drivers/panfrost/pan_aemali_policy.c/h`,
  and `src/gallium/drivers/lima/lima_aemali_policy.c/h`
- runtime policy inputs:
  `AEMALI_DRIVER`, `AERO_MESA_DRIVER`, `AEMALI_PROFILE`,
  `AEMALI_VK_API_CEILING`, `AEMALI_ROUTE`, `AEMALI_TRANSPORT`,
  `AEMALI_PROVIDER_LANE`, `AEMALI_ROUTE_ID`, `AEMALI_OWNER_LANE`,
  `AEMALI_SUPPORT_CLASS`, `AEMALI_KERNEL_EVIDENCE_CLASS`,
  `AEMALI_TRANSPORT_REQUIREMENTS`, `AEMALI_RANKED_KERNEL_DONORS`
- product behavior:
  when enabled, PanVK reports `aemali-panvk`, records profile/route/transport
  in driver info, clamps advertised Vulkan API ceiling, and filters fragile
  compatibility-profile device extensions before exposure.
- product behavior:
  when enabled, Panfrost/Lima keep their real hardware-family split
  (`Panfrost = Midgard/Bifrost/Valhall+`, `Lima = Utgard`) but expose
  AeMali-branded renderer/vendor identity and startup diagnostics keyed off
  `AEMALI_OPENGL*` policy env, so the wrapper lane and the Mesa lane stay in
  one coherent contract instead of two parallel truths.

Current CLC/toolchain blocker:

- Mesa 26.1 PanVK/Panfrost is in Mesa's CLC-required driver set.
- `panvk-android` and `mali-gallium-android` therefore cannot honestly keep
  `-Dllvm=disabled`.
- `tools/build-mesa-staging-graphics.sh` now uses lane-aware LLVM options:
  non-CLC lanes keep `-Dllvm=disabled`; PanVK/Mali lanes require
  `-Dllvm=enabled -Dmesa-clc=enabled -Dprecomp-compiler=enabled`.
- PanVK/Mali setup now treats `SPIRV-LLVM-Translator` and `libclc` as
  first-class CLC support. The script builds `LLVMSPIRVLib`, `llvm-spirv`, and
  `libclc` into the per-run support prefix with the mandatory LLVM 22.1.1
  toolchain before Mesa Meson setup, and exposes both `lib/pkgconfig` and
  `share/pkgconfig` to Meson.
- The local LLVM 22.1.1 compiler exists, but the matching LLVM link libraries
  are currently incomplete under
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux/lib`.
  `llvm-config --libs core` reports missing component libraries.
- The current published host-LLVM release asset was proven tool-only:
  archive listing has no `libLLVM*`, `libclang-cpp`, LLVM CMake package, or
  `opt`. The canonical owner fix is in `wcp-runtime-lanes`: build the full
  install surface with shared `libLLVM`/`libclang-cpp` instead of a narrow
  `install-distribution` subset.
- Because the user contract requires the 22.1.1 toolchain, silently falling
  back to Termux LLVM 21 is forbidden for release payloads.

Expanded graphics donor scan, generated after widening beyond Wine/Winlator:

- raw GitHub cache:
  `/data/data/com.termux/files/home/.cache/omega-donor-research/20260419T202520Z-graphics-global-github`
- expanded manifest:
  `/data/data/com.termux/files/home/.codex/donors/chapter2_donor_manifest.graphics_expanded.json`
- discovery summary:
  `repositories_seen=218`, `candidates_selected=48`,
  `graphics_source_candidates=31`, `verified_mesa_source_trees=3`,
  `verified_virgl_source_trees=5`, `verified_android_app_trees=7`.
- manifest summary after merge:
  `donor_count=610`, `enabled_count=518`, `graphics_source_enabled=8`,
  `graphics_runtime_enabled=22`.
- graphics inventory frontier:
  `/data/data/com.termux/files/home/.cache/omega-donor-frontier/20260419T203202Z-graphics-global-inventory`
- inventory summary:
  `donors_total=30`, `inventory_only=30`, `blocked=0`.
- largest newly inventoried graphics source donors:
  `lfdevs/mesa-for-android-container` (`12447` paths, `39` patch-like),
  `alexvorxx/zink-xlib-termux` (`10311` paths, `38` patch-like),
  `Grima04/mesa-turnip-kgsl` (`8308` paths, `19` patch-like).
- high-signal VirGL/Android source donors were also inventoried:
  `Container-On-Android/virglrenderer`, `Crosvm-Android/virglrenderer`,
  `GlassOnTin/virglrenderer`, `TinkerBoard-Android/rockchip-android-external-virglrenderer`,
  `Aof-Dev/virglrenderer-android`, and related Android virglrenderer forks.

This pass is evidence expansion, not a claim that all GitHub or all global web
sources are exhausted.

Adapter-family support target for this lane is source-explicit, not marketing:

- Utgard-era OpenGL/GLES: Lima where upstream Mesa still supports it.
- Midgard/Bifrost/Valhall OpenGL/GLES: Panfrost Gallium.
- Midgard/Bifrost/Valhall/CSF Vulkan: PanVK where upstream Mesa exposes the
  device and feature level.
- Android stock vendor Vulkan: system HAL route, not Mesa/PanVK.
- Android userspace wrapper Vulkan: imported Vortek Vulkan driver package.
- Android PanVK/Mesa Vulkan: explicit render-node experimental route unless
  the target device proves an accessible DRM/Panthor/Panfrost kernel surface.

## Official VirGL Android Probe

Live official-source probe ledger:

- configure probe with proper Android wrappers:
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419Tvirglrenderer-config-probe3/meson-setup.log`
- compile probe with Android stub support:
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419Tvirglrenderer-config-probe4/meson-compile-rerun3.log`
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419Tvirglrenderer-config-probe4/meson-compile-rerun4.log`

Observed closure so far:

- official `virglrenderer` does configure for Android/bionic on LLVM 22.1.1
  when the host wrappers carry the correct Android target/sysroot/link surface;
- official `virglrenderer` also compiles into Android ELF artifacts on this host
  once the support include/lib surface is completed;
- the old assumption that official upstream is categorically non-buildable on
  this host is false;
- local bundled VirGL tree is not a near-tip mirror:
  `local=112`, `upstream=392`, `common=47`, `local_only=65`, `upstream_only=345`
  from:
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/virgl-tree-diff-local_only-20260419Tresume1.txt`
  `/data/data/com.termux/files/home/.cache/graphics-research/freshest-nonmain/virgl-tree-diff-up_only-20260419Tresume1.txt`

Resolved probe seams:

- correct Android cross wrappers instead of bare compiler path;
- Android logging and property headers through Mesa `android_stub`;
- Android stub support libs for `log`, `cutils`, `hardware`,
  `nativewindow`, `sync`;
- `gbm.h` routed from Mesa source owner, not guessed from broken host export.
- `EGL/eglplatform.h` and `KHR/khrplatform.h` routed from the current
  source-built Mesa include surface.

Current probe artifacts:

- `libvirglrenderer.so`
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419Tvirglrenderer-config-probe4/build/src/libvirglrenderer.so`
  sha256 `33ce01adbc64f4a2c9b778a3a8fb294b42472c42d856d637fb307b0c5ec081fc`
- `virgl_test_server`
  `/data/data/com.termux/files/home/aesolator/out/graphics-source-builds/20260419Tvirglrenderer-config-probe4/build/vtest/virgl_test_server`
  sha256 `47b0d3d8e4d0750d9424bad5bee0ba9d8195562faaab44a5f639952b1a623d10`

Artifact truth from the probe:

- `libvirglrenderer.so` is an Android `aarch64` ELF shared object;
- `virgl_test_server` is an Android `aarch64` PIE executable;
- forbidden grep on both artifacts is currently clean for
  `libc.so.6|ld-linux|/data/data/com.termux/files/usr/lib`;
- `libvirglrenderer.so` currently needs
  `libm.so`, `libepoxy.so`, `libdrm.so`, `libgbm.so`,
  `liblog.so`, `libcutils.so`, `libc.so`.

So the current live frontier is no longer basic compile viability.
The next owner work is:

- move the same support-layer closure from the `main` portability radar onto the
  selected non-main shipping base (`virglrenderer-1.3.0` plus chosen beta overlay);
- package the resulting runtime surface instead of leaving it in probe-only form;
- arbitrate whether `liblog.so` and `libcutils.so` stay as stub sidecars or are
  folded into a dedicated shared Android support package for Chapter 2.

## Runtime Contract

- Top-level container graphics selection is architectural, not archive-shaped:
  `Wrapper`, `VirGL`, `Vortek`. `Zink` and `Freedreno Gallium` belong under
  the `Wrapper` / Adrenotools Mesa lane; `Gladio` belongs under the
  `Vortek + Gladio` MediaTek lane.
- VirGL UI must open its own dialog and emit
  `AERO_GRAPHICS_STACK_PROFILE=universal-virgl`.
- VirGL runtime must keep its own package selection, `GALLIUM_DRIVER`,
  `VIRGL_SERVER_PATH`, and source revision forensics. It must not inherit
  Wrapper/Adrenotools Vulkan ICD routing as its default ownership path.
- MediaTek route must expose one combined `Vortek + Gladio` configuration
  surface: Vortek package, Gladio package, Vulkan API cap, memory/resource
  knobs, exposed extensions, explicit `auto|vulkan-first|opengl-first`
  routing, and a distinct Vortek Vulkan driver-source selector.
  Auto mode chooses Vortek for Vulkan/DXVK routes and Gladio for
  OpenGL/WineD3D routes.
- The Vortek Vulkan source selector must distinguish:
  - `system`: Android HAL / stock vendor loader
  - `custom userspace wrapper`: imported `libvulkan*.so` payload with local metadata
  - `custom drm-render-node experimental`: imported payload that explicitly
    declares render-node dependence and degrades when `/dev/dri/renderD*`
    is unavailable
- Vortek custom Vulkan packages are owned separately from Adrenotools packages.
  They install from ZIP or TZST archives under app-local contents storage, carry their own `meta.json`
  contract, may deploy an overlay payload into the container rootfs, and may
  independently select a host-side library path for the native Vortek bridge.
- Legacy Gladio selection remains accepted for old containers, but new
  top-level creation must not present Gladio as a separate wrapper.
- New Mesa payloads must be built with `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux/bin/clang`.
- The LLVM 22.1.1 compiler is mandatory; Termux bionic CRT/resource-path glue may be used only as
  an explicit build-surface dependency when the local LLVM archive lacks Android CRT builtins.

## 2026-04-19 Wrapper UI / Runtime Closure Checkpoint

Applied owner model:

- New-container graphics driver order is now `Wrapper`, `VirGL`, `Vortek`.
- `Zink` is no longer a top-level container graphics driver. It is the default
  Mesa OpenGL implementation inside the `Wrapper` / Adrenotools route.
- `Freedreno Gallium` remains selectable inside the `Wrapper` route for
  explicitly proven non-KGSL or device-specific cases.
- `Gladio` is no longer a top-level new-container graphics driver. It is the
  OpenGL/GLES half of the `Vortek + Gladio` MediaTek wrapper route.
- `VirGL` is separate and universal: it owns its package version selector,
  Gallium driver selector, `VIRGL_NO_READBACK`, and VirGL-specific forensic
  env without inheriting the Adrenotools Vulkan ICD path.
- `Vortek + Gladio` owns MediaTek routing through one dialog:
  Vortek package, Gladio package, Vulkan cap, memory/resource knobs,
  extension knobs, image-cache policy, no-error policy, and
  `auto|vulkan-first|opengl-first` dispatch.

Runtime/env ownership encoded in source:

- `Wrapper` emits `AERO_WRAPPER_OPENGL_DRIVER`,
  `AERO_OPENGL_GALLIUM_DRIVER`, and `GALLIUM_DRIVER` for the selected
  `zink|freedreno` OpenGL route.
- `VirGL` emits `AERO_GRAPHICS_STACK_PROFILE=universal-virgl`,
  `AERO_VIRGL_PACKAGE`, `AERO_VIRGL_PACKAGE_VERSION`,
  `AERO_VIRGL_GALLIUM_DRIVER`, `AERO_VIRGL_PROVIDER_LANE`,
  `AERO_VIRGL_ROUTE_ID`, `AERO_VIRGL_SUPPORT_CLASS`,
  `AERO_VIRGL_PACKAGE_ENTRY_REQUESTED`, `AERO_VIRGL_PACKAGE_ENTRY`,
  and `AERO_VIRGL_ROUTE_DEGRADED_REASON`,
  and the selected `GALLIUM_DRIVER`.
- `VirGL` does not apply `VK_ICD_FILENAMES` or `VK_DRIVER_FILES` from
  Adrenotools as its default route.
- `Vortek + Gladio` emits `AERO_MEDIATEK_WRAPPER_MODE` and chooses Vortek for
  Vulkan/DXVK routes or Gladio for OpenGL/WineD3D routes in `auto` mode.
- `Vortek + Gladio` now also emits requested vs active Vulkan-source truth:
  driver entry, source kind, transport, source repo, host-side library path,
  container-side library path, render-node requirement, render-node
  availability, and explicit degraded reason when a custom package falls back.
- Custom graphics package `meta.json` may now carry:
  `providerLane`, `routeId`, `ownerLane`, `supportClass`,
  `kernelEvidenceClass`, `transportRequirements`,
  `rankedKernelDonors`, and `diagnosticKeys`.
- Bundled `graphics_driver/*.tzst` payloads that participate in the live
  route contract now follow the same rule: root `meta.json` is required for
  `virgl-*`, `gladio-*`, and `vortek-*` archives, and bundled package parsing
  must reuse extracted archive truth instead of hard-coded UI-only defaults.
- Bundled `Vortek` / `Gladio` / `VirGL` metadata now also carries durable
  provenance pointers back to the support matrix, owner-lane ranking, global
  graphics research ledger, and AeMali Habr/Arm/book synthesis so package
  truth is not stranded in chat-only memory.
- Bundled graphics archives must also stay artifact-clean:
  no `.DS_Store`, `._*`, or similar host-tooling residue inside shipped
  payloads.

Verification checkpoint:

- Unit proof:
  `/data/data/com.termux/files/home/aesolator/out/build-logs/test-graphics-drivers-20260419Twrappers.log`
- APK build proof:
  `/data/data/com.termux/files/home/aesolator/out/build-logs/assemble-debug-20260419Tgraphics-wrapper-routing.log`
- Install proof:
  `/data/data/com.termux/files/home/aesolator/out/build-logs/install-0.9q-wrapper-routing-192.168.43.4-40693.log`
- Installed APK:
  `/data/data/com.termux/files/home/aesolator/app/build/outputs/apk/debug/app-debug.apk`
- Installed version observed through `dumpsys package`:
  `com.winlator.cmod` / `versionName=0.9q` / `versionCode=21`.
- APK sha256 after the wrapper-routing rebuild:
  `fb76fa2494333c9ce45b64fb2c2f157ae613c1aecc3f2fd98fc65d43aa4709d9`.

Device-proof residual:

- App launch proof reached `com.winlator.cmod/.MainActivity`, and no fresh
  `AndroidRuntime` fatal crash was observed in the sampled logcat window.
- ADB UI-tree proof of the New Container dialog is blocked by the device
  lockscreen at this checkpoint: `mCurrentFocus` is `NotificationShade` while
  `mFocusedApp` is `com.winlator.cmod/.MainActivity`.
- Do not claim visual UI closure for the dialog until the device is unlocked
  and the ADB UI dump/screenshot captures the actual Ae.solator screen.
