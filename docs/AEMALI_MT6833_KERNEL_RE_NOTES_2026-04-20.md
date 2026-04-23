# AeMali MT6833 / MT6785 Kernel RE Notes 2026-04-20

## Scope

Эта запись фиксирует reverse-engineering чтение выбранных Realme/MediaTek
kernel donor файлов для AeMali. Это не перенос vendor kernel кода в Mesa.
Цель: извлечь runtime diagnostics, support-matrix constraints и device-proof
гейты для Mali маршрута в Ae.solator.

## Evidence Set

Primary donor:
- `realme-kernel-opensource/realme_8-5G_8S-5G_9-5G_Narzo30-5G_V13_Q3i_realme10_mt6833-AndroidS-kernel-source`
- commit: `d887c07ac1274da3a77cdd1ed38b79902789e986`

Local evidence:
- tree paths:
  `/data/data/com.termux/files/home/.cache/research/aemali-kernel-files-mt6833-androids-20260420/parsed/tree-paths.txt`
- selected paths:
  `/data/data/com.termux/files/home/.cache/research/aemali-kernel-files-mt6833-androids-20260420/parsed/selected-paths.txt`
- extracted file hashes:
  `/data/data/com.termux/files/home/.cache/research/aemali-kernel-files-mt6833-androids-20260420/parsed/extracted-files.tsv`
- extracted files:
  `/data/data/com.termux/files/home/.cache/research/aemali-kernel-files-mt6833-androids-20260420/raw/files`

Counts:
- full tree paths inspected: `9183`
- GPU-selected paths: `338`
- extracted owner files: `22`

## Kernel/User Boundary Map

Observed kernel-side owner families:
- `drivers/misc/mediatek/gpu/ged`: MediaTek GPU Energy/diagnostic/DVFS bridge.
- `drivers/misc/mediatek/gpu/gpu_mali/mali_bifrost`: vendor Arm kbase tree.
- `drivers/misc/mediatek/gpu/gpu_mali/mali_valhall`: vendor Arm kbase tree for newer family.
- `drivers/gpu/drm/mediatek`: display/DRM subsystem evidence.

Product implication:
- AeMali must report whether a device exposes upstream Mesa-usable DRM render
  nodes or only Android/vendor HAL/kbase surfaces.
- GED/kbase evidence improves diagnostics and support-class explanations; it
  does not by itself make PanVK usable on Android without the needed kernel
  route.

## GED Findings

`ged_main.c` exposes a bridge-command router with commands for:
- GPU frequency boost, around `GED_BRIDGE_COMMAND_BOOST_GPU_FREQ`.
- DVFS probing, around `GED_BRIDGE_COMMAND_DVFS_PROBE`.
- GPU DVFS information query, around `GED_BRIDGE_COMMAND_QUERY_GPU_DVFS_INFO`.
- GPU tuner status, around `GED_BRIDGE_COMMAND_GPU_TUNER_STATUS`.

`ged_dvfs.c` exposes a dense DVFS surface:
- exported callback and query symbols such as `ged_query_info`,
  `ged_dvfs_cal_gpu_utilization_*`, and `ged_dvfs_gpu_freq_commit_fp`;
- module parameters for GPU loading, block/idle accounting, DVFS enable,
  custom boost and upper-bound frequencies, timer-based emulation, bandwidth
  ratio, and debug state;
- thermal limit callback registration through MediaTek GPufreq glue.

`ged_gpu_tuner.c` exposes package/status tuner logic through the bridge command
surface.

AeMali product action:
- Add runtime forensic keys for `ged_present`, `ged_dvfs_surface`,
  `ged_gpu_tuner_surface`, and `vendor_kbase_surface` when such paths are
  detectable from device filesystem or logs.
- Keep those keys diagnostic-only unless device permissions and kernel ABI are
  explicitly proven. Ae.solator must not write vendor DVFS/sysfs knobs blindly.

## Kbase / Power Findings

The extracted MT6785 kbase platform files show:
- MFG bus-idle polling through MediaTek register ranges.
- MFG hardware clock-gating setup.
- platform power-on/power-off callbacks calling MediaTek GPufreq power glue.
- GPU clock-switch notification into GED DVFS.
- suspend/resume paths guarded by a platform mutex.

AeMali product action:
- Treat power/clock/DVFS behavior as kernel/vendor-owned.
- For user-space Mesa routing, only diagnose whether the render route is
  present and stable; do not assume Ae.solator can control frequency or power.
- When route failures happen on MediaTek Mali, logs must separate:
  `missing_render_node`, `vendor_kbase_only`, `hal_vulkan_only`,
  `permission_denied`, and `mesa_driver_missing`.

## Donor Layer Correction

Earlier `60`-repo API probe was too narrow for kernel-donor selection. The
full Realme census must cover all `473` public repos from the cached org list.

The active full git-tree probe is:
- `/data/data/com.termux/files/home/.cache/research/realme-kernel-full-git-tree-probe-20260420`

The reducer is:
- `/data/data/com.termux/files/home/tools/aemali_kernel_donor_reduce.py`

Early partial reduction over the first `33` records already found:
- `15` repos with `drivers/gpu/drm/panfrost`;
- `15` repos with `drivers/gpu/drm/lima`;
- `4` dense `mediatek-vendor-kbase-ged` donors.

This falsifies the weaker assumption that only the initial `6` live GPU
candidates mattered.

Boundary:
- `drivers/gpu/drm/panfrost` or `drivers/gpu/drm/lima` present in a kernel
  source tree is source-availability evidence, not device enablement proof.
- The next filter must read defconfig/Kconfig/device-tree/vendor properties
  before calling a repo a working Panfrost device route.

## Roadmap Impact

AeMali donor selection now has four kernel lanes:
- Mainline Panfrost/PanVK evidence lane: repos with `drivers/gpu/drm/panfrost`
  and `drivers/gpu/drm/lima`.
- MediaTek vendor kbase/GED lane: repos with `gpu_mali`, `mali_bifrost`,
  `mali_valhall`, and `ged`.
- MediaTek display/DRM lane: repos with `drivers/gpu/drm/mediatek`.
- Android product integration lane: device trees, vendor properties,
  proprietary file manifests, init and sepolicy.

Selection rule:
- Use mainline Panfrost/PanVK donors for Mesa/kernel UAPI truth.
- Use MediaTek kbase/GED donors for diagnostics and route classification.
- Use Android product donors for packaging, properties, permissions, and
  runtime route proof.
- Do not import proprietary kbase/GED code into Mesa user-space.
- Do not treat mainline driver source presence as proof that a production
  Android device exposes a render node.

## Next Execution Slice

1. Let the full `473` repo git-tree probe finish or resume from cached
   `raw/*/probe.json`.
2. Run `aemali_kernel_donor_reduce.py` against the complete probe output.
3. Extract owner files from top-ranked donors in each class:
   mainline Panfrost, mainline Lima, MediaTek kbase/GED, MediaTek display DRM.
4. Update `aemali_support_matrix_2026-04-20.json` with donor-backed
   `kernel_evidence_class` and `transport_requirements`.
5. Add Ae.solator runtime diagnostic model for Mali route classification.

## Blockers

- No Mali device proof exists in the current local device session.
- Arm direct body fetch produced HTTP `403`; the active route is metadata,
  accessible official pages, mirrors, and explicit blocked rows.
- Proprietary Arm Mali user-space binaries are reference evidence only and
  remain license-gated for source import.
