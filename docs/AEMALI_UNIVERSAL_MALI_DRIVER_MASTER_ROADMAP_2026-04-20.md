# AeMali Universal Mali Driver Master Roadmap 2026-04-20

## Цель

Финальный результат: AeMali как универсальный Mali driver lane для
Ae.solator, построенный на Mesa-compatible архитектуре.

Что это значит технически:
- единый product core для Mali route detection, support matrix, package
  metadata, runtime forensics, diagnostics и fallback policy;
- Mesa-native user-space route через Panfrost/PanVK/Lima, без fake blob-copy;
- kernel/vendor archaeology как источник диагностики и compatibility policy,
  а не слепой source import;
- Android HAL route и Mesa render-node route остаются разными транспортами;
- Vortek/Gladio остаются wrapper/orchestration классом, а не подменой Mesa
  compiler/kernel UAPI.

Что это не значит:
- не один магический драйвер, который честно поддерживает все Mali независимо
  от kernel/runtime;
- не импорт proprietary `libmali`;
- не форк Panfrost с произвольной Vortek-логикой внутри compiler/UAPI;
- не conformance claim без upstream/device proof.

## Жесткая Рефлексия

Текущее убеждение:
- Первый live target должен быть `Bifrost v7 / Mali-G76 MC4`.
- Второй target должен быть `Valhall v10 / Mali-G610`.
- Universal layer должен начинаться как shared product core и support matrix,
  а не как новый `src/gallium/drivers/mali-uni` с копированием Lima/Panfrost.

Почему:
- Mesa документирует G76 в Bifrost v7 lane с OpenGL ES 3.1, OpenGL 3.1,
  Vulkan 1.0.
- Mesa документирует G610 в Valhall v10 lane с Vulkan 1.4, а PanVK conformant
  именно на G610.
- Realme/MT6833 kernel donors дают реальные `gpu_mali`, `mali_bifrost`,
  `mali_valhall`, `ged`, power/DRM surfaces для Android vendor archaeology.
- У текущего устройства нет доказанного Mali runtime, поэтому device proof
  пока blocker.

Что может опровергнуть план:
- Mesa source изменит support table или PanVK policy.
- Target device окажется HAL-only без render-node/DRM route.
- LLVM 22.1.1 CLC surface не будет закрыт.
- Kernel donor evidence покажет vendor-only UAPI, несовместимый с upstream
  Panfrost/PanVK.

Как это влияет на execution:
- Сначала фиксируем evidence и support matrix.
- Потом укрепляем PanVK/Panfrost policy и package/runtime truth.
- Только после этого строим driver core abstraction.
- Build не запускается до coherent source-first stitch.

## Evidence Ledger

### Official / Upstream

- Mesa Panfrost/PanVK docs:
  `https://docs.mesa3d.org/drivers/panfrost.html`
  - owner: upstream user-space support truth
  - use: support matrix, conformance state, LLVM requirement
- Android Vulkan HAL:
  `https://source.android.com/docs/core/graphics/implement-vulkan`
  - owner: Android stock Vulkan route truth
  - use: separate HAL route from Mesa ICD route
- AOSP `hwvulkan`:
  `https://android.googlesource.com/platform/frameworks/native/+/master/vulkan/include/hardware/hwvulkan.h`
  - owner: Android HAL ABI surface
- openSUSE ARM Mali:
  `https://en.opensuse.org/ARM_Mali_GPU`
  - owner class: distro/operational matrix cross-check
  - use: kernel-floor and board-route evidence

### Arm / Habr / Web Intake

- Arm Mali search corpus:
  `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420`
  - `3879` raw hits
  - `3262` unique results
  - `1499` HTML candidates pending body harvest
- Habr Mali corpus:
  `/data/data/com.termux/files/home/.cache/research/habr-mali-intake-20260420/corpus`
  - `40` search hits
  - `20` unique pages
- Habr expanded AeMali corpus:
  `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus`
  - `202` search hits
  - `87` unique pages
  - `0` fetch errors
- Arm direct body fetch blocker:
  `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420/arm-body-fetch-blocker-20260420.json`
  - direct body fetch HTTP `403`: `21`
  - active route: Coveo metadata, accessible official pages, mirrors, explicit
    blocked rows
- Seed links:
  `/data/data/com.termux/files/home/.cache/research/graphics-seed-links-20260420`
  - bakhi, pine64, Habr, fxlin, libmali-rockchip, Elvees

### Local Books

131-book semantic corpus:
- `/data/data/com.termux/files/home/.codex/skills/local-corpus-knowledge-promotion/references/book-corpus-131-ledger-2026-04-17.md`
- `131/131` semantic processed
- `85123014` bytes read

New targeted PDFs:
- `/data/data/com.termux/files/home/.cache/research/aemali-books-corpus-20260420`
- `Computer Organization and Design ARM edition`: `ok`, `2535003` chars
- `TDCI_Arch`: `ok`, `21519` chars
- `Modern GPU Architecture`: `small_fragment`, `3197` chars

Book role:
- ARM/memory/cache/pipeline model
- GPU pipeline / shader / latency / throughput model
- not UAPI authority
- not Mali conformance authority

### Kernel Donors

Realme org census:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-opensource-intake-20260420`
- `473` repos
- `60` candidates probed
- `6` live GPU candidates

Realme git-tree probe:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-git-tree-probe-20260420`
- `6/6` ok
- `0` failures
- API `403` bypassed through git transport

Full Realme kernel git-tree probe:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-full-git-tree-probe-20260420`
- all `473` cached Realme org candidates were probed through shallow
  blobless no-checkout git transport
- final reducer found:
  - `175` repos with `drivers/gpu/drm/panfrost`
  - `175` repos with `drivers/gpu/drm/lima`
  - `2` repos with `drivers/gpu/drm/panthor`
  - `28` dense `mediatek-vendor-kbase-ged` donors
  - `18` `mediatek-vendor-kbase` donors
  - `66` MediaTek display DRM-only donors
  - `186` no-signal repos for the selected Mali/MTK paths
- proof level for `panfrost` / `lima` hits:
  `source-tree-presence`; enablement and runtime route remain unproven until
  Kconfig, defconfig, DT, render-node, permission, and Mesa ICD checks pass
- reducer:
  `/data/data/com.termux/files/home/tools/aemali_kernel_donor_reduce.py`
- enablement reducer:
  `/data/data/com.termux/files/home/tools/aemali_kernel_enablement_probe.py`
- top-12 dense enablement sample:
  `/data/data/com.termux/files/home/.cache/research/realme-kernel-enablement-probe-20260420/enablement.partial.json`
  - `12/12` classified as `vendor-kbase-enabled-candidate`
  - this confirms the densest MTK layer is vendor-kbase/GED/BSP evidence first,
    not a pre-proven Panfrost runtime lane
- top-16 mainline enablement sample:
  `/data/data/com.termux/files/home/.cache/research/aemali-mainline-kernel-enablement-20260420/enablement.json`
  - selection: `2` `panthor` + `14` top `panfrost`
  - `16/16` classified as `mainline-source-present-enable-unproven`
  - this confirms the mainline-rich donor layer currently gives source/build
    truth faster than device/runtime truth

Nothing kernel 6.1 donor:
- `/data/data/com.termux/files/home/.cache/research/nothing-kernel-mt6878-20260420`
- repo: `NothingOSS/android_kernel_6.1_nothing_mt6878`
- branch: `mt6878/Tetris/u`
- head: `d6343dbbb3eaeaf4a2a52d6ef6a30501a09ea464`
- tree evidence:
  - `drivers/gpu/drm/panfrost`
  - `drivers/gpu/drm/lima`
  - `drivers/gpu/drm/mediatek`
- enablement evidence:
  - `CONFIG_DRM_LIMA=m`
  - `CONFIG_DRM_PANFROST=m`
  - quick DT tree pass found no explicit `mt6878` / `tetris` / `nothing`
    path under `arch/arm64/boot/dts/mediatek`
  - deeper DT/render probe:
    generic upstream Mediatek DTS coverage is present and includes Mali-backed
    examples such as `mt8183` + `arm,mali-bifrost`, but there is still no
    explicit `mediatek,mt6878` / `tetris` device-tree match
  - current route status: `mainline-source-present-enable-unproven`

Owner-lane ranking:
- readable:
  `/data/data/com.termux/files/home/aesolator/docs/AEMALI_KERNEL_OWNER_LANE_RANKING_2026-04-20.md`
- machine-readable:
  `/data/data/com.termux/files/home/aesolator/docs/assets/aemali_kernel_owner_lane_ranking_2026-04-20.json`
- current execution interpretation:
  - transfer order must now follow owner lanes, not raw donor order
  - `mainline-source` lane feeds Mesa/PanVK/Panfrost source policy
  - `vendor-kbase diagnostics` lane feeds detection, DVFS/power/tuner
    diagnostics, and Android BSP forensic logic
  - `Android BSP integration` lane feeds config, props, packaging, display, and
    device-surface wiring

MT6833 owner-file RE notes:
- `/data/data/com.termux/files/home/aesolator/docs/AEMALI_MT6833_KERNEL_RE_NOTES_2026-04-20.md`
- selected files: `22`
- source tree paths inspected: `9183`
- GPU-selected paths: `338`

Habr / Arm / book synthesis:
- `/data/data/com.termux/files/home/aesolator/docs/AEMALI_HABR_ARM_BOOK_SYNTHESIS_2026-04-20.md`
- used for: RE method, Android driver delivery constraints, Arm source/import
  boundaries, and local book-backed GPU/ARM model separation

Highest-value donor:
- `realme_8-5G_8S-5G_9-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidS-kernel-source`
  - head: `d887c07ac1274da3a77cdd1ed38b79902789e986`
  - `gpu_mali`: `4862`
  - `mali_bifrost`: `2354`
  - `mali_valhall`: `2507`
  - `ged/src`: `18`
  - `ged/include`: `20`

Other donors:
- `AgentFabulous/begonia:a10-rebase`
  - MT6785 / G76 MC4 vendor-kernel archaeology
- `CherishOS-Devices/android_device_realme_RM6785:tiramisu`
  - Android product integration / props / sepolicy / proprietary files
- `NothingOSS/android_kernel_6.1_nothing_mt6878`
  - Android 6.1 MT6878 mainline-source reference with Panfrost/Lima built as
    modules in defconfig; useful for source and build policy, not yet for
    route proof
- `tsukumijima/libmali-rockchip`
  - proprietary ABI/package reference only
- `fxlin/mali`, `bakhi/mobileGPU`
  - RE notes and mental model, not source authority

## Universal Driver Architecture

### Layer 0: Support Matrix / Product Truth

Owner:
- Ae.solator docs/assets first, then Java/native runtime consumers.

Artifacts:
- `docs/assets/aemali_support_matrix_2026-04-20.json`
- package `meta.json`
- UI route labels
- runtime forensic keys

Responsibilities:
- supported GPU list
- API ceilings
- conformance status
- kernel minimum
- Android transport class
- render-node requirement
- known donor evidence
- owner-lane priority
- ranked donor trail for the active route

### Layer 1: Kernel / Runtime Detector

Inputs:
- `/dev/dri/renderD*`
- Android HAL/Vulkan properties
- Mesa ICD presence
- kernel version
- GPU model / compatible string when accessible
- vendor properties

Outputs:
- `aemali_route=panvk|panfrost|lima|hal-vulkan|unsupported`
- `aemali_support=conformant|supported-experimental|blocked-kernel|blocked-transport|unsupported`
- explicit reason string

Forbidden:
- silent fallback
- claiming Vulkan from HAL when PanVK was requested

### Layer 2: Shared BO / Winsys Policy

Scope:
- product-level diagnostics and metadata first;
- Mesa internal refactor only after G76/G610 proof.

Future Mesa shape:
- shared BO/cache/winsys abstraction only if upstream source shows duplicated
  Lima/Panfrost logic that can be safely unified.

Hard rule:
- no new kernel UAPI from userspace.

### Layer 3: Compiler / NIR Dispatcher

Correct direction:
- common NIR policy and capability tables;
- backend stays generation-specific:
  - Lima IR for Utgard
  - Midgard/Bifrost/Valhall compiler paths for Panfrost/PanVK

Forbidden first wave:
- fake common compiler wrapper that hides ISA differences.

### Layer 4: XML / Descriptor / Command Stream

Correct direction:
- inventory existing Panfrost XML/gen_pack descriptors;
- compare Lima command packets separately;
- design a normalized debug representation, not one byte-packer for all
  generations at once.

First useful product artifact:
- common `AeMaliDescriptorTrace` schema for logs and RE notes.

### Layer 5: Job Builder

Generation reality:
- Lima/Utgard: GP/PP split.
- Panfrost/Bifrost/Valhall: job graphs / dependencies.
- Panthor/CSF generations: newer queue model and kernel floor.

Implementation order:
- diagnostic abstraction first;
- source refactor only after device traces.

### Layer 6: Debug / RE Tooling

Required:
- pandecode-oriented trace plan
- common disassembler output format
- Perfetto/logcat integration plan
- kernel donor file notes for GED/DVFS/power coupling

## Execution Plan

### Phase 1: Corpus Completion

Tasks:
- Keep Arm HTML body harvest as an evidence lane, but stop direct fetch
  hammering when pages return HTTP `403`; preserve blocked rows and use
  accessible official/mirror pages for material facts.
- Expand Habr topic matrix beyond `Mali`:
  - `Panfrost`
  - `PanVK`
  - `Mesa Mali`
  - `Bifrost`
  - `Valhall`
  - `kbase`
  - `MediaTek Mali`
  - `GED GPU`
- Complete the full Realme kernel probe over all `473` cached repos and reduce
  it through path-class flags, not repo-name assumptions. Treat `panfrost`
  source-tree presence as a candidate class, not route proof.
- Run the second-stage kernel enablement probe over ranked donors:
  Kconfig/Makefile hooks, defconfig enablement, device-tree Mali nodes,
  render-node expectations, and Android permission implications.
- Keep `NothingOSS/android_kernel_6.1_nothing_mt6878` in the mainline-source
  donor lane as the first non-Realme Android 6.1 MTK reference; use it to
  cross-check module enablement and future DT/render-node probes.
- Extract selected kernel donor files through git transport, not GitHub REST,
  from each top-ranked class:
  - mainline Panfrost/Lima
  - MediaTek vendor kbase/GED
  - MediaTek display DRM
  - Android product integration
- Keep local PDF fast path for targeted files.

Done:
- every source has raw artifact, parsed artifact, and integration status.

### Phase 2: Support Matrix Generator

Tasks:
- Convert current support table into JSON.
- Add source references per row.
- Add kernel donor evidence per relevant SoC/GPU.
- Add Android transport classification.

Done:
- matrix is consumed by docs/package/UI/runtime planning.

### Phase 3: G76 Bring-Up

Tasks:
- Treat `G76 / Bifrost v7 / Vulkan 1.0` as first live target.
- Mine Realme MT6833 AndroidS kernel files:
  - `mali_kbase_config_*`
  - `mali_kbase_cpu_*`
  - `ged_dvfs.c`
  - `ged_gpu_tuner.c`
  - Kconfig/Makefile ownership
- Add runtime diagnostics for:
  - missing render node
  - unsupported model
  - incompatible kernel
  - HAL-only route
  - no PanVK ICD

Done:
- AeMali can say exactly why G76 is usable or blocked.

### Phase 4: G610 Conformance Lane

Tasks:
- Treat `G610 / Valhall v10 / Vulkan 1.4` as conformance reference.
- Keep G610 policy separate from G76.
- Add explicit `conformant=true` only for G610 where upstream docs/source
  agree.

Done:
- no back-propagated false conformance.

### Phase 5: Mesa Patchset

Already present:
- `0001-panvk-aemali-policy-surface.patch`
- `0002-panvk-aemali-api-truth-surface.patch`

Next patch classes:
- support-matrix metadata export
- PanVK driverInfo / forensic label consistency
- extension mask policy by support class
- build/package source provenance
- tests/checks for API truth

Done:
- patches apply on selected Mesa non-main branch.
- no CLC disable.
- no stale asset version.

### Phase 6: Ae.solator Runtime Integration

Tasks:
- add AeMali support matrix loader
- show support class in UI
- write forensic route keys
- route PanVK/Panfrost/Lima/HAL/Vortek separately
- fail visibly on blocked kernel/runtime

Done:
- UI, package metadata, runtime env, and logs agree.

### Phase 7: Toolchain Closure

Required:
- LLVM `22.1.1`
- `libLLVM`
- `libclang-cpp`
- `opt`
- LLVM/Clang CMake configs
- `SPIRV-LLVM-Translator v22.1.1`
- `libclc`

Done:
- PanVK configure passes with CLC enabled.
- no Termux LLVM downgrade.

### Phase 8: Device Proof

Required scenarios:
- G76-class device if available
- G610-class device if available
- no render node
- HAL-only Vulkan
- PanVK ICD present/missing
- OpenGL Panfrost present/missing

Done:
- route evidence appears in logs.
- no silent fallback.

## Stop Conditions

Do not claim final universal driver until:
- Arm/Habr/kernel/book evidence is reduced to durable decisions or blockers.
- support matrix is source-backed and consumed.
- G76 first lane is implemented and tested or blocked by device/kernel proof.
- G610 conformance lane is represented honestly.
- CLC/toolchain is closed.
- runtime/device proof exists.

If a blocker remains, it must name:
- exact owner
- evidence path
- reason
- required closure condition
