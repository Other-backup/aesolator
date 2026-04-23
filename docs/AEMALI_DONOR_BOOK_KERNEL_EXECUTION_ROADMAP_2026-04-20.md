# AeMali Donor / Book / Kernel Execution Roadmap 2026-04-20

## Current Truth

This roadmap is the execution ledger for the AeMali lane. It is not a
completion claim.

The broader final-driver roadmap is:
`docs/AEMALI_UNIVERSAL_MALI_DRIVER_MASTER_ROADMAP_2026-04-20.md`.

The first machine-readable support matrix is:
`docs/assets/aemali_support_matrix_2026-04-20.json`.

Facts:
- Mesa Panfrost/PanVK is the upstream source owner for modern Mali user-space.
- Lima remains the Mesa owner for Utgard.
- Android stock Vulkan is a vendor HAL route; Mesa PanVK on Android is a
  separate render-node/ICD route.
- The first live AeMali target is `Bifrost v7 / Mali-G76 MC4`.
- The second target is `Valhall v10 / Mali-G610`.
- No broad build is allowed before the evidence and source patchset are
  coherently stitched.

Hard boundary:
- AeMali must not claim "all Mali support".
- AeMali must not import proprietary libmali code.
- AeMali must not collapse Android HAL Vulkan, PanVK, Panfrost, Lima, and
  Vortek/Gladio wrapper policy into one fake driver.

## 403 Status

GitHub REST API state:
- `/data/data/com.termux/files/home/.cache/research/github-rate-limit-20260420.json`
- `x-ratelimit-remaining=0`
- reset: `2026-04-20T08:15:04Z`

The `403` is removed from the active donor path by switching from REST API
probing to git transport:

- tool: `/data/data/com.termux/files/home/tools/github_git_tree_probe.py`
- transport: `git clone --depth=1 --filter=blob:none --no-checkout`
- evidence path:
  `/data/data/com.termux/files/home/.cache/research/realme-kernel-git-tree-probe-20260420`
- result: `6/6` Realme kernel candidates probed successfully, `0` failures

Rule:
- Do not hammer GitHub API while remaining quota is `0`.
- Use cached API census for candidate selection.
- Use git tree probes for path evidence.
- Use targeted `git show HEAD:path` only for selected owner files after tree
  evidence proves the file matters.

## Evidence Inventory

### Official / Upstream

- Mesa Panfrost/PanVK docs:
  `https://docs.mesa3d.org/drivers/panfrost.html`
  - G31/G51/G52/G76: Bifrost v7, OpenGL ES 3.1, OpenGL 3.1, Vulkan 1.0
  - G310/G610: Valhall v10, Vulkan 1.4
  - PanVK conformant on G610, non-conformant on other GPUs
  - LLVM is required for Panfrost compiler builds
- Android Vulkan HAL docs:
  `https://source.android.com/docs/core/graphics/implement-vulkan`
- AOSP `hwvulkan` ABI:
  `https://android.googlesource.com/platform/frameworks/native/+/master/vulkan/include/hardware/hwvulkan.h`
- openSUSE ARM Mali operational matrix:
  `https://en.opensuse.org/ARM_Mali_GPU`
  - local `curl` receives `403`, but browser fetch is available and source is
    cited in the global ledger

### Local Book Corpus

Existing 131-book semantic frontier:
- ledger:
  `/data/data/com.termux/files/home/.codex/skills/local-corpus-knowledge-promotion/references/book-corpus-131-ledger-2026-04-17.md`
- books available: `131`
- semantic processed: `131`
- bytes read: `85123014`
- relevant domain counts:
  - `graphics-foundations`: `19`
  - `graphics-opengl`: `12`
  - `graphics-vulkan`: `4`
  - `graphics-x11`: `2`
  - `linux-kernel`: `4`
  - `arm64-assembly`: `4`
  - `arm-reverse-engineering`: `2`

Targeted 2026-04-20 PDF extraction:
- corpus:
  `/data/data/com.termux/files/home/.cache/research/aemali-books-corpus-20260420`
- `Computer Organization and Design ARM edition`: `ok`, `2535003` chars
- `TDCI_Arch`: `ok`, `21519` chars
- `Modern GPU Architecture - Ismayil Tahmazov`: `small_fragment`,
  `3197` chars

Book usage rule:
- Books set the engineering model: memory hierarchy, cache, pipeline,
  latency/throughput, SIMD, shader pipeline, ARM systems behavior.
- Books do not decide Mali UAPI, ISA, support matrix, or conformance.
  Mesa source, kernel headers, Arm docs, and device traces decide those.

### Site Intake

- Arm Mali vendor search:
  `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420`
  - `3879` raw hits
  - `3262` unique results
  - `81` raw search pages
  - `1499` HTML candidates pending body harvest
- Habr Mali:
  `/data/data/com.termux/files/home/.cache/research/habr-mali-intake-20260420/corpus`
  - `40` search hits
  - `20` unique pages
  - `0` fetch errors
- Habr expanded AeMali corpus:
  `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus`
  - topic queries: `11`
  - search hits: `202`
  - unique pages: `87`
  - fetch errors: `0`
- Seed hyperlink frontier:
  `/data/data/com.termux/files/home/.cache/research/graphics-seed-links-20260420`
  - `bakhi/mobileGPU/mali`: `28`
  - `pine64/Mali_driver`: `36`
  - `Habr 655673`: `50`
  - `Habr Mali search`: `33`
  - `fxlin/mali`: `139`
  - `libmali-rockchip`: `130`
  - `Elvees Mali`: `347`

### Kernel Donors

Realme kernel org census:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-opensource-intake-20260420`
- repos enumerated: `473`
- top candidates API-probed: `60`
- live MediaTek/GPU hits: `6`

Git tree probe:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-git-tree-probe-20260420`
- repos ok: `6`
- failures: `0`

Full Realme kernel git-tree probe:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-full-git-tree-probe-20260420`
- source list: all `473` cached Realme org candidates
- method: shallow blobless no-checkout git tree probing
- active reducer:
  `/data/data/com.termux/files/home/tools/aemali_kernel_donor_reduce.py`
- active enablement probe:
  `/data/data/com.termux/files/home/tools/aemali_kernel_enablement_probe.py`
- final reducer result over all `473` records:
  - `175` repos with `drivers/gpu/drm/panfrost`
  - `175` repos with `drivers/gpu/drm/lima`
  - `2` repos with `drivers/gpu/drm/panthor`
  - `28` dense `mediatek-vendor-kbase-ged` donors
  - `18` `mediatek-vendor-kbase` donors
  - `66` MediaTek display DRM-only donors
  - `186` repos with no Mali-relevant signal in the selected paths
- proof level for these `panfrost` / `lima` hits:
  `source-tree-presence`, not working route proof
- top-12 enablement sample result:
  - `/data/data/com.termux/files/home/.cache/research/realme-kernel-enablement-probe-20260420/enablement.partial.json`
  - `12/12` highest dense MTK donors classified as
    `vendor-kbase-enabled-candidate`
  - none of this top dense sample downgraded into a proven Panfrost runtime route
- mainline top-sample result:
  - `/data/data/com.termux/files/home/.cache/research/aemali-mainline-kernel-enablement-20260420/enablement.json`
  - selection: `2` `panthor` + `14` top `panfrost` donors
  - `16/16` classified as `mainline-source-present-enable-unproven`
  - result:
    source/config evidence is strong, but current DT/render-node/device proof is
    still insufficient for an honest Android runtime-route claim

Selected MT6833 owner-file extraction:
- `/data/data/com.termux/files/home/.cache/research/aemali-kernel-files-mt6833-androids-20260420`
- tree paths: `9183`
- GPU-selected paths: `338`
- extracted owner files: `22`
- companion RE note:
  `/data/data/com.termux/files/home/aesolator/docs/AEMALI_MT6833_KERNEL_RE_NOTES_2026-04-20.md`

Highest-value kernel donors:
- `realme_8-5G_8S-5G_9-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidS-kernel-source`
  - head: `d887c07ac1274da3a77cdd1ed38b79902789e986`
  - `drivers/misc/mediatek/gpu`: `7755` entries
  - `gpu_mali`: `4862` entries
  - `mali_bifrost`: `2354` entries
  - `mali_valhall`: `2507` entries
  - `ged`: `39` entries
  - `ged/src`: `18` entries
  - `ged/include`: `20` entries
- `realme_8-5G_8S-5G_9-5G_9i-5G_Narzo50-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidT-kernel-source`
  - head: `67969d76c14a5f567f31857beac6af2a98281318`
  - Valhall `mali-r25p0` and `mali-r27p0` platform files
- `realme_11_MT6833-AndroidT-kernel-source`
  - head: `90aaf973612e26413b66353d4fb9023f545100c5`
  - Valhall `mali-r25p0` and `mali-r27p0` platform files
- `realme_10_mt6789` / `realme_11_mt6789`
  - DRM/MediaTek display/BSP evidence
  - no live `gpu_mali` path in the current probe

Other kernel/product donors:
- `AgentFabulous/begonia:a10-rebase`
  - MT6785 / Mali-G76 MC4 vendor-kernel archaeology
- `CherishOS-Devices/android_device_realme_RM6785:tiramisu`
  - Android product integration for the same SoC class
- `NothingOSS/android_kernel_6.1_nothing_mt6878`
  - head: `d6343dbbb3eaeaf4a2a52d6ef6a30501a09ea464`
  - branch: `mt6878/Tetris/u`
  - source-tree evidence:
    `drivers/gpu/drm/panfrost`, `drivers/gpu/drm/lima`,
    `drivers/gpu/drm/mediatek`
  - enablement evidence:
    `CONFIG_DRM_LIMA=m`, `CONFIG_DRM_PANFROST=m` in `arch/arm64/configs/defconfig`
  - quick DT tree evidence:
    no explicit `mt6878` / `tetris` / `nothing` DTS path surfaced under
    `arch/arm64/boot/dts/mediatek` in the fast tree pass
  - deeper DT/render probe:
    `/data/data/com.termux/files/home/.cache/research/nothing-kernel-mt6878-20260420/dt_render_probe_20260420.txt`
    - Mediatek DTS exists only as generic upstream coverage up to visible
      families such as `mt8183`, `mt8192`, `mt8195`
    - Mali-compatible Mediatek DT nodes exist in that generic upstream set
      (for example `mt8183` + `arm,mali-bifrost`)
    - no explicit `mediatek,mt6878` / `tetris` board DT match was found
  - current classification:
    `mainline-source-present-enable-unproven`
  - meaning:
    strong mainline-source donor and useful Android 6.1 MTK reference, but DT
    / render-node / permission proof is still missing

Owner-lane ranking artifact:
- machine-readable:
  `/data/data/com.termux/files/home/aesolator/docs/assets/aemali_kernel_owner_lane_ranking_2026-04-20.json`
- readable:
  `/data/data/com.termux/files/home/aesolator/docs/AEMALI_KERNEL_OWNER_LANE_RANKING_2026-04-20.md`
- current lane heads:
  - mainline-source:
    `realme_neo8-AndroidB-common-source`,
    `realme_GT8pro-AndroidB-common-source`,
    `NothingOSS/android_kernel_6.1_nothing_mt6878`
  - vendor-kbase diagnostics:
    `realme_8-5G_8S-5G_9-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidS-kernel-source`,
    `AgentFabulous/begonia:a10-rebase`,
    `realme_q2pro-AndroidS-kernel-source`
  - Android BSP integration:
    `CherishOS-Devices/android_device_realme_RM6785:tiramisu`,
    `realme_neo8-AndroidB-common-source`,
    `NothingOSS/android_kernel_6.1_nothing_mt6878`

Reference-only donors:
- `tsukumijima/libmali-rockchip`
  - proprietary packaging / ABI / firmware evidence only
- `fxlin/mali`, `bakhi/mobileGPU`
  - RE notes, not source authority
- `pine64`, `openSUSE`, `Elvees`
  - operational packaging / board / distro evidence

## Hard Reflection

Known:
- G76 is the right first target because it joins Mesa support, Vulkan 1.0
  PanVK path, and real Android vendor-kernel evidence.
- G610 is the second target because PanVK conformance is documented there.
- MT6833 AndroidS kernel source is the densest vendor-kernel RE donor found so
  far.
- GitHub API 403 is not a research blocker anymore for tree evidence.
- The earlier `60`-repo probe was not enough for kernel donor selection. The
  final full-census evidence shows many repos with mainline `panfrost` and
  `lima` trees, so donor selection must be complete-org first, ranked second.
- Source tree presence is not device proof. Repos with `drivers/gpu/drm/panfrost`
  still need defconfig, Kconfig, device-tree, permissions, and render-node
  validation before they count as a working Android Panfrost route.
- Some Realme kernel donors already contain upstream `panfrost`/`lima` source
  trees. This improves source archaeology but does not override the Android
  runtime transport boundary: AeMali must still prove build enablement,
  compatible DT nodes, accessible `/dev/dri/renderD*`, and Mesa ICD packaging.

Assumptions:
- The Realme MT6833 kernel line is close enough to the target MediaTek Mali
  phone class to inform diagnostics and support-matrix policy.
- Vendor kbase/GED behavior can improve AeMali runtime detection and
  diagnostics without importing vendor kernel code.

Falsifiers:
- Mesa source or current kernel UAPI contradicts the support matrix.
- Device proof shows no render node / no compatible kernel route.
- Vendor files prove the target device uses only proprietary HAL/libmali and no
  Mesa-usable DRM path.
- PanVK build requires CLC pieces not present in the LLVM 22.1.1 toolchain.

Decision:
- Build AeMali as a product-integrated Mesa/PanVK/Panfrost route with explicit
  support states first.
- Do not start by creating a new upstream `mali-uni` driver directory.
- Do not mix Vortek internals into shader compiler or kernel UAPI. Use Vortek
  logic only as policy/forensics/wrapper-route inspiration unless source-level
  evidence proves a deeper owner.
- Split kernel donors into four lanes:
  mainline Panfrost/Lima, MediaTek vendor kbase/GED, MediaTek display DRM, and
  Android product integration. Each lane has a different owner and a different
  import policy.
- Rank source availability, device enablement, and runtime route proof as
  separate evidence levels.

## Execution Roadmap

### Phase 0: Evidence Normalization

Deliverables:
- `Arm` HTML candidate ledger with explicit `403` blocker rows and accessible
  official/mirror pages.
- Full Realme git-tree probe over all `473` cached repos, reduced by:
  `contains_panfrost`, `contains_lima`, `contains_panthor`,
  `contains_mtk_mali_kbase`, `contains_mtk_ged`, and `contains_mtk_drm`.
- Second-stage enablement probe over ranked source-tree donors:
  Kconfig/Makefile hooks, defconfig enablement, device-tree Mali nodes, and
  explicit route status.
- Parallel non-Realme validation donor:
  `NothingOSS/android_kernel_6.1_nothing_mt6878` as Android 6.1 MT6878
  mainline-source reference with Panfrost/Lima enabled as modules in defconfig.
- Selected file fetches from top-ranked donors:
  - `mali_kbase_config_mt6873.c`
  - `mali_kbase_config_platform.h`
  - `mali_kbase_cpu_mt6873.c`
  - `ged_dvfs.c`
  - `ged_gpu_tuner.c`
  - relevant `Kconfig` / `Makefile`
- Fast local PDF path for single-file requests; full corpus intake only for
  broad-library waves.

Done gate:
- Evidence ledger names exact files, commits, SHA or source URL.
- No claim of "read all internet" or "all books" without corpus evidence.

### Phase 1: Support Matrix Generator

Build a generated matrix from Mesa + openSUSE + kernel donor evidence:
- family: Utgard / Midgard / Bifrost / Valhall / 5th Gen
- GPU model
- Mesa route: Lima / Panfrost / PanVK
- OpenGL / GLES / Vulkan ceiling
- conformance state
- kernel minimum
- Android transport: HAL-only / render-node candidate / unsupported
- vendor-kernel evidence

Product output:
- JSON support matrix consumed by Ae.solator UI/package metadata.
- UI strings that say `supported`, `experimental`, `blocked by kernel`, or
  `unsupported`, not generic "Mali supported".

### Phase 2: G76 Bring-Up Lane

Owner target:
- Bifrost v7 / Mali-G76 MC4

Why:
- First target with both Mesa Vulkan route and Android vendor evidence.

Work:
- Compare Mesa PanVK/Panfrost G76 handling against Realme/Begonia kbase/GED
  evidence.
- Add diagnostics for:
  - missing render node
  - unsupported model
  - incompatible kernel
  - HAL-only Vulkan
  - no PanVK ICD route
- Keep Vulkan ceiling honest: Vulkan `1.0`, non-conformant unless upstream says
  otherwise.

### Phase 3: G610 Conformance Lane

Owner target:
- Valhall v10 / Mali-G610

Why:
- PanVK conformance target.

Work:
- Use Mesa support table and PanVK policy to model conformant path.
- Keep Vulkan `1.4` only where Mesa source and package metadata agree.
- Do not backport G610 claims to G76.

### Phase 4: Mesa Patchset Hardening

Current landed/proven:
- `0001-panvk-aemali-policy-surface.patch`
- `0002-panvk-aemali-api-truth-surface.patch`
  - verified as `0001 -> 0002` apply chain on `origin/staging/26.1`

Next patches:
- PanVK/Panfrost support-matrix metadata export.
- Driver info / forensics improvement.
- Optional extension mask profile by support class.
- Tests/check scripts for instance version vs physical-device version.

Forbidden:
- No compiler-backend hacks without ISA evidence.
- No UAPI mutation in Mesa userspace.
- No fake conformance flags.

### Phase 5: Runtime / Package Integration

Work:
- Package AeMali assets with dynamic source provenance.
- Include support matrix truth in `meta.json`:
  `providerLane`, `routeId`, `ownerLane`, `supportClass`,
  `kernelEvidenceClass`, `transportRequirements`,
  `rankedKernelDonors`, and `diagnosticKeys`.
- Expose route in Ae.solator:
  - `AeMali PanVK`
  - `Mali Gallium/Panfrost`
  - `Lima compatibility`
  - `Android HAL Vulkan`
  - `Vortek/Gladio wrapper`
- Make route failure observable in forensics.
- Keep runtime env aligned with the same ownership surface:
  route id, owner lane, support class, kernel evidence, transport
  requirements, and ranked donor trail.

Done gate:
- UI, package metadata, runtime env, and logs agree on the same route and
  support class.

### Phase 6: Toolchain Closure

Hard rule:
- LLVM `22.1.1` from `wcp-runtime-lanes` remains the only release toolchain.

Required:
- `libLLVM`
- `libclang-cpp`
- `opt`
- LLVM/Clang CMake configs
- `SPIRV-LLVM-Translator v22.1.1`
- `libclc`

Done gate:
- Mesa PanVK configure does not disable CLC.
- No fallback to older Termux LLVM.

### Phase 7: Device Proof

Required scenarios:
- G76-class device or explicit no-device blocker.
- Render node present / absent.
- HAL Vulkan only.
- PanVK ICD load attempt.
- OpenGL Panfrost route.
- Vortek/Gladio wrapper route remains separate.

Done gate:
- Runtime logs prove route selection and failure reason.
- No silent fallback.

## Completion Criteria

AeMali is not closed until:
- Arm body harvest is reduced to explicit integrated facts or blockers.
- Kernel donor tree evidence has selected-file RE notes, not only path counts.
- Support matrix exists and is consumed by package/UI/runtime surfaces.
- G76 target is implemented as the first live lane.
- G610 target is represented as conformance lane.
- Toolchain CLC closure is proven.
- Device proof exists or the no-device blocker is explicit.
- No API `403` is part of the active donor path.
