# AeMali Black Diamond Roadmap 2026-04-20

## Problem Statement

AeMali must not become a fake "all Mali everywhere" label. The real product
target is stricter:

- truthful Mali support split by hardware family and runtime transport;
- stronger PanVK/Panfrost/Lima downstream integration for Chapter 2;
- honest Android constraints around HAL vs Mesa render-node routes;
- build/package/runtime/forensics closure under the LLVM 22.1.1 contract.

Detailed donor/book/kernel execution order is now tracked in:
`docs/AEMALI_DONOR_BOOK_KERNEL_EXECUTION_ROADMAP_2026-04-20.md`.

## Intake Snapshot

Current reproducible research surfaces:

- Arm Mali vendor corpus:
  `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420`
  - `3879` raw search hits
  - `3262` unique results
  - `81` raw search pages
  - `1499` HTML page candidates
- Habr Mali corpus:
  `/data/data/com.termux/files/home/.cache/research/habr-mali-intake-20260420/corpus`
  - `40` search hits
  - `20` unique article/news pages
  - `0` fetch errors
- Seed hyperlink frontier:
  `/data/data/com.termux/files/home/.cache/research/graphics-seed-links-20260420`
  - `bakhi/mobileGPU/mali`: `28` links
  - `pine64/Mali_driver`: `36` links
  - `Habr 655673`: `50` links
  - `Habr Mali search`: `33` links
  - `fxlin/mali`: `139` links
  - `libmali-rockchip`: `130` links
  - `Elvees mali`: `347` links
- `realme-kernel-opensource` org census:
  `/data/data/com.termux/files/home/.cache/research/realme-kernel-opensource-intake-20260420`
  - `473` public repositories enumerated through GitHub API
  - `60` top candidates probed against GPU/MediaTek paths
  - `6` repositories had live MediaTek/GPU probe hits
  - deep follow-up probe is currently blocked by GitHub unauthenticated
    `403 rate limit exceeded`
- `realme-kernel-opensource` git tree fallback:
  `/data/data/com.termux/files/home/.cache/research/realme-kernel-git-tree-probe-20260420`
  - transport: `git-partial-no-checkout`
  - `6` repositories probed
  - `6` ok
  - `0` failed
  - API `403` removed from active kernel-donor path
- Targeted local PDF extraction:
  `/data/data/com.termux/files/home/.cache/research/aemali-books-corpus-20260420`
  - `Computer Organization and Design ARM edition`: `ok`,
    `2535003` extracted chars
  - `TDCI_Arch`: `ok`, `21519` extracted chars
  - `Modern GPU Architecture - Ismayil Tahmazov`: `small_fragment`,
    `3197` extracted chars

## Donor Classes

### Source-of-truth donors

- `mesa/mesa`
  - owner for PanVK, Panfrost, Lima, NIR, Vulkan/OpenGL frontends
- Linux DRM/panthor/panfrost kernel documentation and code
  - owner for render-node/kernel/UAPI truth
- Android Vulkan HAL docs/AOSP `hwvulkan`
  - owner for Android stock Vulkan loader/HAL truth

### High-value research donors

- `bakhi/mobileGPU`
  - value: job-chain, MMU, kbase, timeline, trace internals
  - limitation: RE notes, not production source owner
- `fxlin/mali`
  - value: kernel/user-space anatomy, address-space and MMU notes
  - limitation: note corpus, not current upstream support matrix
- `AgentFabulous/begonia:a10-rebase`
  - value: full Android 10 vendor-kernel source for `begonia` / MT6785 /
    `Mali-G76 MC4`; useful for IDE-level RE, kbase-era kernel-driver behavior,
    BSP memory/power integration, and MediaTek glue archaeology
  - limitation: vendor kernel donor, not upstream PanVK/Panfrost owner and not
    safe for blind import into the Mesa lane
- `CherishOS-Devices/android_device_realme_RM6785:tiramisu`
  - value: Android 13 device tree for the same MT6785 / `Mali-G76 MC4` class;
    useful for board config, proprietary-file manifests, vendor props,
    sepolicy, overlays, and runtime packaging/integration assumptions
  - limitation: product-integration donor only; it does not replace kernel
    owner truth or upstream Mesa support truth
- `realme-kernel-opensource`
  - value: broad public kernel-source donor organization with hundreds of
    Realme/Oppo-family kernel drops; current census found live MediaTek GPU
    ownership paths in MT6833 and MT6789 families
  - limitation: vendor-kernel archaeology and BSP integration evidence only;
    GPL kernel source can inform diagnostics/support matrix, but it is not a
    direct Mesa userspace import source and must not override upstream UAPI
    truth
- `Habr 655673`
  - value: reverse-engineering workflow and Mesa-first driver strategy framing
  - limitation: methodology source, not implementation authority
- `openSUSE ARM_Mali_GPU`
  - value: operational support matrix and kernel-floor cross-check
  - limitation: distro integration layer; upstream Mesa/kernel still outrank it

### Reference-only proprietary/package donors

- `tsukumijima/libmali-rockchip`
  - value: proprietary userspace packaging, firmware naming, ABI layout
  - limitation: Arm EULA binary surface; not acceptable code donor
- `pine64/Mali_driver`
  - value: historical downstream binary-driver packaging and coexistence notes
  - limitation: old downstream blob world; useful mainly as anti-pattern and
    compatibility evidence

## Engineering Truth From Intake

- PanVK/Panfrost/Lima must remain split by hardware family:
  - Utgard -> Lima
  - Midgard/Bifrost/Valhall/5th Gen -> Panfrost/PanVK
- Android stock Vulkan is vendor HAL.
  Mesa PanVK on Android remains a separate experimental userspace ICD overlay
  with render-node dependence.
- Upstream Mesa docs currently expose:
  - PanVK conformant on `Mali-G610`
  - non-conformant on other supported GPUs
  - supported Vulkan models up to `G720` and `G725`
- openSUSE cross-check adds kernel-floor signal:
  - Bifrost needs `5.10+`
  - `G57` needs `5.20+`
  - `G310/G610` need `6.10+`
  - `G720/G725` need `6.18+`
- AeMali therefore must model support as:
  - `supported + conformant`
  - `supported + non-conformant`
  - `unsupported family`
  - `unsupported due kernel/runtime transport`
- Local book evidence changes the analysis model, not the support matrix:
  - `Computer Organization and Design ARM edition` is useful for ARM memory,
    cache, pipeline, exception, and systems-background reasoning.
  - `TDCI_Arch` is useful for GPU pipeline evolution, unified shader,
    latency/throughput, SIMD/warp-style scheduling, cache and bandwidth
    tradeoff framing.
  - These books do not create Mali UAPI or ISA authority; Mesa source,
    kernel headers, Arm public docs, and device traces still decide driver
    behavior.
- The user's five-stage driver-development plan maps cleanly to owners:
  - hardware architecture -> `src/panfrost/lib`, XML descriptors, ISA/compiler
  - kernel/user UAPI -> Linux `panfrost_drm.h`, `panthor_drm.h`, vendor kbase
  - Mesa driver structure -> Gallium/PanVK winsys, BO, command stream
  - reverse engineering -> `pandecode`, vendor kernel archaeology, trace logs
  - minimal pipeline -> BO allocation, shader compile, submit/wait test

## Immediate Owner Fixes

### 1. PanVK API truth closure

Owner:
- `aesolator/patches/mesa/aemali/0002-panvk-aemali-api-truth-surface.patch`

Why:
- instance-level Vulkan version was clamped by policy
- physical-device `apiVersion` was not
- this creates misleading runtime truth and compatibility ambiguity

Closure:
- clamp `VkPhysicalDeviceProperties.apiVersion` through AeMali policy too
- verified: `0001` then `0002` apply cleanly on
  `mesa-main origin/staging/26.1` with `patch_chain_ok=1`

### 2. Package truth closure

Owner:
- `aesolator/tools/build-mesa-staging-graphics.sh`

Why:
- asset names still encoded stale `26.1.0`
- actual source truth already comes from `RESOLVED_MESA_REF` + `COMMIT`

Closure:
- derive asset/version strings from the live selected source
- ship PanVK `meta.json` with honest support and conformance metadata

## Generation Bootstrap Decision

Do not begin AeMali from Utgard/Lima and do not begin from the newest
Valhall-only CSF path.

First live target:
- `Bifrost v7 / Mali-G76 MC4`

Why this is the strongest starting point:
- Mesa documents `G31/G51/G52/G76` as supported Bifrost v7 hardware with
  OpenGL ES `3.1`, OpenGL `3.1`, and Vulkan `1.0`.
- `G76` is close enough to the modern Panfrost/PanVK path to exercise real
  Mesa Mali internals, unlike Utgard/Lima where Vulkan is the wrong API target.
- `G76 MC4` has direct Android product evidence through `RM6785` and direct
  vendor-kernel archaeology through `begonia` / `realme-kernel-opensource`.
- It avoids the highest-risk first step of modern Valhall/CSF/panthor where
  kernel floors and render-node truth are more likely to block Android proof.

Second target:
- `Valhall v10 / Mali-G610`

Why:
- PanVK is documented as conformant on `G610`.
- This is the right conformance/reference target after the Bifrost bring-up
  path proves common BO/winsys/forensics/package truth.

Explicit non-start targets:
- `Utgard / Mali-400/450`: useful only as a Lima compatibility lane; not a
  Vulkan/AeMali first target.
- `G720/G725`: valuable future upper-bound targets; not first because they
  require newer kernel/runtime evidence and would mix feature bring-up with
  platform bring-up.

Universal-driver framing:
- `mali-uni` as a new monolithic Mesa driver is not the first implementation
  step.
- The first implementation step is a shared AeMali product core:
  support matrix, BO/runtime diagnostics, package metadata, route selection,
  and PanVK/Panfrost policy hooks.
- Only after G76 and G610 paths are proven should any upstream-shaped shared
  `mali_common` or driver restructuring be considered.

## Next Technical Waves

### Wave A. Knowledge closure

- Finish Arm HTML body harvest for the `1499` candidate pages.
- Continue local-book use through targeted extraction first; reserve heavy
  full-library intake only for broad-library waves.
- Promote recurring vendor/support/kernel facts into durable skills/rules.
- Expand first-hop seed links into focused second-hop domains:
  - Mesa/Panfrost docs
  - kernel panthor docs
  - Arm public docs
  - reverse-engineering writeups

### Wave B. PanVK product hardening

- Keep AeMali policy in downstream PanVK owner surfaces only:
  - API ceiling
  - extension masks
  - driver info/forensics
  - package metadata
- Do not blindly inject Vortek folklore into:
  - compiler backends
  - NIR lowering
  - pan_kmod / kernel UAPI internals
- Add downstream tests/checks for:
  - instance version == physical-device version policy ceiling
  - driverName/driverInfo truth
  - meta.json support matrix truth

### Wave C. Kernel/runtime matrix

- Build an explicit matrix per Mali family:
  - GPU family
  - Mesa minimum
  - kernel minimum
  - PanVK/Panfrost/Lima route
  - Android viability
  - render-node requirement
  - conformance state
- Treat missing render-node or incompatible kernel as explicit degraded state,
  not silent fallback

### Wave C1. Vendor-kernel archaeology

- Mine `AgentFabulous/begonia:a10-rebase` specifically for:
  - Mali kernel-driver location and split (`drivers/gpu` / MediaTek glue)
  - kbase/MMU/job submission/power policy behavior
  - vendor `ioctl` and memory-management assumptions
  - firmware naming, DVFS, devfreq, and thermal coupling
- Use it as RE evidence for the `Mali-G76 / Helio G90T` class.
- Do not upstream-copy its code into AeMali blindly; translate only proven
  design lessons into:
  - downstream diagnostics
  - support-matrix notes
  - compatibility heuristics
  - explicit blocked/unsupported states
- Mine `realme-kernel-opensource` candidate repos from the saved census:
  - `realme_8-5G_8S-5G_9-5G_9i-5G_Narzo50-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidT-kernel-source`
    has `drivers/misc/mediatek/gpu/gpu_mali/mali_valhall`
  - `realme_8-5G_8S-5G_9-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidS-kernel-source`
    has `mali_bifrost`, `mali_valhall`, `ged`, and MediaTek power glue
  - `realme_10_mt6789` / `realme_11_mt6789` repos expose DRM/MediaTek and
    BSP surfaces but need deeper probe after GitHub rate limit resets
- Extract only owner-level design signals:
  - GPU directory split
  - kbase/Valhall/Bifrost generation split
  - GED/devfreq/power/thermal coupling
  - DRM display-vs-compute boundary
  - Android vendor integration assumptions

### Wave C2. Android product-integration archaeology

- Mine `CherishOS-Devices/android_device_realme_RM6785:tiramisu` for:
  - graphics-related `BoardConfig.mk` and `device.mk` toggles
  - proprietary graphics blob census and naming
  - `vendor.prop` / `system.prop` graphics hints
  - `sepolicy`, `init`, `overlay`, `vndk`, and shim assumptions touching GPU
  - SoC-family runtime packaging differences across MT6785 products
- Use it to harden AeMali packaging/runtime truth for the `Mali-G76` product
  class without pretending those Android-device assumptions are the same thing
  as upstream PanVK/Panfrost driver support.

### Wave D. Toolchain closure

- Finish real LLVM 22.1.1 install surface:
  - `libLLVM`
  - `libclang-cpp`
  - `opt`
  - LLVM/Clang CMake configs
  - matching `SPIRV-LLVM-Translator`
  - `libclc`
- Until then, PanVK release claims remain blocked

### Wave E. Runtime/package integration

- Expose AeMali support classes in package metadata and UI:
  - supported + conformant
  - supported + experimental
  - unsupported family
  - unsupported on this kernel/runtime transport
- Keep `Vortek/Gladio` and `AeMali PanVK` as different classes:
  - wrapper policy/orchestration
  - Mesa driver implementation
- Keep `AeMali Gallium` at package/runtime parity with the rest of the visible
  graphics stack:
  - bundled asset discovery
  - custom package import/remove
  - root-overlay deploy
  - display-version truth
  - route degradation truth
  - metadata/env parity with `Vortek`, `Gladio`, and `VirGL`

## Anti-Fake-Completion Rules For AeMali

- Do not claim "all Mali support".
- Do not import proprietary blob code into Mesa lane.
- Do not collapse HAL Vulkan and PanVK into one route.
- Do not advertise Vulkan conformance beyond what Mesa currently proves.
- Do not ship stale package names or stale source provenance.
- Do not treat RE notes as upstream authority.

## Blockers

- No real Mali device proof on the current host.
- Full Arm HTML body harvest is not complete yet.
- LLVM 22.1.1 full CLC surface is still incomplete locally.
- GitHub unauthenticated API rate limit currently blocks the second-level
  `realme-kernel-opensource` deep probe.

## Done Definition For Next Wave

The next honest AeMali closure point requires all of:

- Arm HTML candidate body harvest materially reduced or completed
- dynamic package truth landed
- PanVK API truth patch landed
- explicit support matrix generated from upstream + vendor + distro evidence
- LLVM 22.1.1 CLC surface completed or blocked with concrete artifact proof
- device/runtime validation on real Mali hardware or explicit no-device blocker
