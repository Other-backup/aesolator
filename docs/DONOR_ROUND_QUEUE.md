# Donor Round Queue (Strict Mode)

Date: `2026-03-05`

Execution mode: `1 round = 1 donor` (sequential by default; owner override is allowed and must be documented).

## Round States

- `pending`: donor not started.
- `active`: donor round in progress.
- `gate`: implementation done, regression gates running.
- `gate_hold`: round is frozen in gate (owner override, no additional reruns in this phase).
- `closed`: donor fully exhausted (`integrated` or `rejected-with-reason` for every signal).

## Round Closure Checklist (Required)

1. Donor line-manifest and file inventory updated.
2. Module-level behavior map created.
3. Transfer candidates mapped to Aeolator targets.
4. Every candidate marked `integrate` or `reject` with rationale.
5. Integrated changes include forensic/runtime visibility where applicable.
6. Regression gates passed:
   - container create/update/start
   - launch path (Wine/Proton/route selection)
   - X11 input/orientation
   - contents install/update/replace
   - forensic issue-bundle completeness
7. No unresolved conflicts in donor transfer map.
8. Donor round summary committed to docs.
9. Queue state updated (`active -> gate -> closed`).
10. Only then next donor round may start unless owner override is explicitly declared.

## Queue

| Round | Donor | State | Notes |
|---:|---|---|---|
| 1 | `utkarshdalal/GameNative` | `gate_hold` | soft-handoff completed; closure deferred by owner decision |
| 2 | `KreitinnSoftware/MiceWine-Application` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 3 | `Open-Wine-Components/umu-launcher` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 4 | `khanhduytran0/ExagearAndroidX11Server` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 5 | `olegos2/mobox` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 6 | `ewt45/termux-x11-fork` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 7 | `ewt45/winlator-fork` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 8 | `coffincolors/winlator` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 9 | `GameHub-Lite-5.3.3-RC2.apk` | `closed` | APK donor sweep completed (`integrated/rejected` finalized) |
| 10 | `Mob-FGSR/MobFGSR` | `gate_hold` | core upscaler lane integrated; closure deferred by owner override to next donor |
| 11 | `xXJSONDeruloXx/linux-fg` | `gate_hold` | target-fps/interpolation lane integrated; closure deferred by owner override to next donor |
| 12 | `Nukem9/dlssg-to-fsr3` | `gate_hold` | debug/interposer bridge lane integrated; closure deferred by owner override to next donor |
| 13 | `proqaz2-design/Frame-generation-` | `gate_hold` | mobile framegen mode/thermal lane integrated; closure deferred by owner override to next donor |
| 14 | `optiscaler/OptiScaler` | `gate_hold` | FG source/output routing lane integrated; closure deferred by owner override to next donor |
| 15 | `Eden-Android-9d2341eaea-standard.apk` | `closed` | Vulkan validation lane integrated; env-layer merge + VulkanSDK guard hardened; app-tree closure completed |

## Round 6 (termux-x11-fork) Historical Workset (Completed)

1. Rebuilt strict file inventory for `termux-x11-fork` and locked module buckets (`app/java`, `app/cpp`, `shell-loader`, `res`).
2. Re-opened transfer matrix by lanes (`loader_trust`, `touch_input_strategy`, `clipboard_sync`, `key_interceptor`, `native_xtrans`).
3. Marked each signal `integrate` or `reject_with_rationale`.
4. Landed no-regression app-tree deltas as contracts (no blind donor copy).
5. Completed round closure (`active -> closed`) with X11 launch/input/clipboard forensic anchors.

Round 6 control artifacts:
- `docs/rounds/R6_TERMUXX11_MATRIX.md`
- `docs/rounds/R6_TERMUXX11_FILE_COVERAGE.md`

## Round 7 (winlator-fork) Historical Workset (Completed)

1. Rebuilt strict file inventory for `winlator-fork` and locked module buckets (`ewt45 overlay`, `java mainline`, `native cpp`, `res`).
2. Opened transfer matrix by lanes (`xserver_lifecycle`, `diagnostics_hooks`, `storage_obb`, `key_input`, `xserver_ext`, `native_boundary`).
3. Marked each signal `integrate` or `reject_with_rationale`.
4. Landed no-regression app-tree deltas as contracts (no blind donor copy).
5. Completed round closure (`active -> closed`) with launch/PiP/input/forensic anchors.

Round 7 control artifacts:
- `docs/rounds/R7_WINLATORFORK_MATRIX.md`
- `docs/rounds/R7_WINLATORFORK_FILE_COVERAGE.md`

## Round 8 (coffincolors/winlator) Historical Workset (Completed)

1. Rebuilt strict file inventory for `coffincolors/winlator` and locked module buckets (`java`, `native cpp`, `res`, plugins, root metadata).
2. Opened transfer matrix by lanes (`bionic_runtime`, `launcher_discipline`, `graphics_dialogs`, `task_manager`, `xserver_java`, `native_boundary`).
3. Marked each signal `integrate` or `reject_with_rationale`.
4. Landed no-regression app-tree deltas as contracts (no blind donor copy).
5. Completed round closure (`active -> closed`) with launch/graphics/task-manager/forensic anchors.

Round 8 control artifacts:
- `docs/rounds/R8_COFFINCOLORS_MATRIX.md`
- `docs/rounds/R8_COFFINCOLORS_FILE_COVERAGE.md`

## Round 9 (GameHub APK donor lane) Historical Workset (Completed)

1. Rebuilt strict APK inventory from analysis workspace (`reports/*`) and locked module buckets (`winmonitor_schema`, `perf_renderdoc`, `trans_layer`, `native_binary`).
2. Opened transfer matrix by lanes (`winmonitor_contract`, `perf_diagnostics`, `trans_layer_contract`, `native_boundary`).
3. Marked each signal `integrate` or `reject_with_rationale`.
4. Landed no-regression app-tree deltas as contracts (forensic runtime snapshot + issue-bundle inclusion).
5. Completed round closure (`active -> closed`) with forensic/runtime snapshot anchors.

Round 9 control artifacts:
- `docs/rounds/R9_GAMEHUBAPK_MATRIX.md`
- `docs/rounds/R9_GAMEHUBAPK_FILE_COVERAGE.md`

## Round 1 -> Round 2 Soft Handoff (Smoothed Transition)

What was added to smooth the transition:
1. Round 1 kept as `gate_hold` instead of forcing artificial `closed`.
2. Round 2 opened with explicit owner-override record and no hidden state jumps.
3. Carry-over technical prerequisites from Round 1 were preserved:
   - launch dependency contracts remained active;
   - runtime/version selector normalization remained in place;
   - no rollback of gate-level fixes before Round 2 start.
4. Round 2 scope explicitly excluded Round 1 CI rerun dependency to avoid blocking momentum.

## Round 1 Hold Snapshot

- `2026-03-05`:
  - Round 1 transfer matrix is completed and remained in gate.
  - owner requested transition to Round 2 without additional rerun in this phase.
  - Round 1 state was frozen as `gate_hold`.

## Round 2 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 2 formally opened (`active`) via owner override.
  - MiceWine matrix + file coverage controls initialized.
- `2026-03-05` pass 2:
  - first donor transfer landed in task manager lane: windows rows now expose `RAM + CPU` live metrics.
- `2026-03-05` pass 3:
  - task manager refresh cadence aligned with donor baseline (`750ms`) for tighter realtime updates.
- `2026-03-05` pass 4:
  - orientation/config change path hardened (`XServerView` relayout + overlay relayout request).
- `2026-03-05` pass 5:
  - clipboard/browser bridge env guards hardened and explicit runtime markers added.
- `2026-03-05` closure:
  - Round 2 moved to `closed`.

## Round 3 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 3 opened as `active`.
  - `R3_UMU_MATRIX` and `R3_UMU_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - runtime provenance markers added for wrapper contract/env source tracking.
- `2026-03-05` pass 3:
  - runtime contract forensic marker event landed (`RUNTIME_WRAPPER_CONTRACT_APPLIED`).
- `2026-03-05` closure:
  - Round 3 moved to `closed`.

## Round 4 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 4 opened as `active`.
  - `R4_EXAGEAR_MATRIX` and `R4_EXAGEAR_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - concrete donor anchors fixed for touch fan-out, gesture FSM, transform lane, X11 lifecycle lane.
- `2026-03-05` pass 3:
  - xserver lock/focus hardening landed (`XServer`, `WindowManager`, `DesktopHelper`).
- `2026-03-05` closure:
  - Round 4 moved to `closed`.

## Round 5 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 5 opened as `active`.
  - `R5_MOBOX_MATRIX` and `R5_MOBOX_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - donor anchors fixed for `install`, `patches/*`, `components/*`.
  - boundary rule recorded: runtime patch rows routed to runtime/build repos, not app-tree.
- `2026-03-05` pass 3:
  - launcher runtime bootstrap/path contract landed in `GuestProgramLauncherComponent`.
- `2026-03-05` closure:
  - Round 5 moved to `closed`.

## Round 6 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 6 opened as `active`.
  - `R6_TERMUXX11_MATRIX` and `R6_TERMUXX11_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - launch trust-state forensic markers landed in `XServerDisplayActivity`.
  - clipboard policy forensic marker lane landed.
- `2026-03-05` closure:
  - Round 6 moved to `closed`.

## Round 7 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 7 opened as `active`.
  - `R7_WINLATORFORK_MATRIX` and `R7_WINLATORFORK_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - PiP lifecycle continuity lane landed in `XServerDisplayActivity` (no runtime pause on PiP transition).
  - forensic marker `XSERVER_PIP_CONTINUITY` added for traceability.
- `2026-03-05` pass 3:
  - key-input dispatch fanout hardened in `XServerDisplayActivity` to prevent controller-event drop on chained negative logic.
- `2026-03-05` pass 4:
  - diagnostics/runtime stream row finalized as integrated (existing forensic stream hook contract reused).
  - native cpp donor lane bounded out of app-tree (explicit reject-with-rationale boundary).
- `2026-03-05` pass 5:
  - storage permission lane moved to forensic-visible contract in `MainActivity` (`all-files-access` prompt/actions logged).
- `2026-03-05` pass 6:
  - keyboard event consumption hardened (`ACTION_MULTIPLE` passthrough + no unconditional consume in `Keyboard.onKeyEvent`).
- `2026-03-05` pass 7:
  - storage/obb lane finalized as bounded integrate decision under contents-first contract.
  - key-input lane finalized (stable fanout + consumption semantics; donor unicode remap hack rejected).
- `2026-03-05` closure:
  - Round 7 moved to `closed`.

## Round 8 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 8 opened as `active`.
  - `R8_COFFINCOLORS_MATRIX` and `R8_COFFINCOLORS_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - runtime environment startup forensic markers landed in `XServerDisplayActivity` (`RUNTIME_ENV_COMPONENTS_PREPARED/STARTED`).
- `2026-03-05` pass 3:
  - transfer rows finalized across bionic/runtime, launcher, graphics/dialog, task-manager and xserver java lanes.
- `2026-03-05` closure:
  - Round 8 moved to `closed`.

## Round 9 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 9 opened as `active`.
  - `R9_GAMEHUBAPK_MATRIX` and `R9_GAMEHUBAPK_FILE_COVERAGE` initialized from APK analysis reports.
- `2026-03-05` pass 2:
  - forensic runtime snapshot lane landed:
    - new runtime capture contract file: `runtime-snapshot.json` in issue bundles;
    - forensic events: `FORENSIC_RUNTIME_SNAPSHOT_CAPTURED` / `FORENSIC_RUNTIME_SNAPSHOT_FAILED`.
- `2026-03-05` closure:
  - Round 9 moved to `closed`.

## Round 10 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 10 opened as `active`.
  - `R10_MOBFGSR_MATRIX` and `R10_MOBFGSR_FILE_COVERAGE` initialized.
  - app-tree upscaler lane rebuilt: unified `AE Upscaler / Frame Generation` UI + runtime env contract + forensic signal binding.
- `2026-03-06` pass 2:
  - integrated SoC-aware upscaler preset lane (`upscalerPreset`) with runtime auto-resolution and effective policy export:
    - `AERO_UPSCALER_PRESET_REQUESTED`, `AERO_UPSCALER_PRESET_EFFECTIVE`, `AERO_UPSCALER_SOC_CLASS`;
    - preset-aware clamps for generated-frames/target-fps/interpolation/thermal guard;
    - forensic marker extended with requested/effective policy values.
- `2026-03-05` hold:
  - Round 10 moved to `gate_hold`.
  - owner override recorded to open Round 11 before final Round 10 closure gates.

## Round 11 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 11 opened as `active`.
  - `R11_LINUXFG_MATRIX` and `R11_LINUXFG_FILE_COVERAGE` initialized.
  - integrated `Target FPS` + `Interpolation Factor` controls into shortcut contract and runtime env lane:
    - shortcut keys: `upscalerTargetFps`, `upscalerInterpolationFactor`;
    - runtime env: `AERO_UPSCALER_TARGET_FPS`, `AERO_FRAMEGEN_INTERPOLATION_FACTOR`,
      `AERO_MOBFGSR_TARGET_FPS`, `AERO_MOBFGSR_INTERPOLATION_FACTOR`;
    - forensic marker `UPSCALER_ROUTE_APPLIED` extended with both fields.
- `2026-03-06` pass 2:
  - pacing lane moved to raw+effective policy model via preset+SoC resolution;
  - forensic marker extended with `target_fps_effective` and `interpolation_factor_effective`.
- `2026-03-05` hold:
  - Round 11 moved to `gate_hold`.
  - owner override recorded to open Round 12 before final Round 11 closure gates.

## Round 12 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 12 opened as `active`.
  - `R12_DLSSGTOFSR3_MATRIX` and `R12_DLSSGTOFSR3_FILE_COVERAGE` initialized.
  - integrated dlssg-to-fsr3 debug control bridge into upscaler lane:
    - shortcut keys: `upscalerDebugOverlay`, `upscalerDebugTearLines`, `upscalerInterpolatedOnly`;
    - runtime env: `AERO_FRAMEGEN_DEBUG_OVERLAY`, `AERO_FRAMEGEN_DEBUG_TEAR_LINES`, `AERO_FRAMEGEN_INTERPOLATED_ONLY`;
    - bridge env for translator compatibility: `DLSSGTOFSR3_EnableDebugOverlay`, `DLSSGTOFSR3_EnableDebugTearLines`, `DLSSGTOFSR3_EnableInterpolatedFramesOnly`;
    - forensic marker `UPSCALER_ROUTE_APPLIED` extended with debug fields.
- `2026-03-06` pass 2:
  - bridge lane aligned with preset-aware effective framegen policy and SoC-aware preset markers.
- `2026-03-05` hold:
  - Round 12 moved to `gate_hold`.
  - owner override recorded to open Round 13 before final Round 12 closure gates.

## Round 13 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 13 opened as `active`.
  - `R13_FRAMEGENAPP_MATRIX` and `R13_FRAMEGENAPP_FILE_COVERAGE` initialized.
  - integrated framegen mode/thermal policy lane:
    - shortcut keys: `upscalerFramegenMode`, `upscalerThermalGuard`;
    - runtime env: `AERO_FRAMEGEN_MODE`, `AERO_FRAMEGEN_THERMAL_GUARD`;
    - mobfgsr policy env: `AERO_MOBFGSR_MODE`, `AERO_MOBFGSR_THERMAL_GUARD`,
      `AERO_MOBFGSR_MODEL_SCALE`, `AERO_MOBFGSR_QUALITY`, `AERO_MOBFGSR_FRAME_BUDGET_MS`;
    - forensic marker `UPSCALER_ROUTE_APPLIED` extended with `framegen_mode`/`thermal_guard`.
- `2026-03-06` pass 2:
  - thermal/mode policy now exported as effective values after preset resolution (auto/conservative/balanced/aggressive).
- `2026-03-05` hold:
  - Round 13 moved to `gate_hold`.
  - owner override recorded to open Round 14 before final Round 13 closure gates.

## Round 14 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 14 opened as `active`.
  - `R14_OPTISCALER_MATRIX` and `R14_OPTISCALER_FILE_COVERAGE` initialized.
  - integrated OptiScaler-style FG source/output routing:
    - shortcut keys: `upscalerFgSource`, `upscalerFgOutput`;
    - runtime env: `AERO_FRAMEGEN_SOURCE`, `AERO_FRAMEGEN_OUTPUT`, `AERO_DLSSG_TO_FSR3_BRIDGE`;
    - mobfgsr bridge env: `AERO_MOBFGSR_FG_SOURCE`, `AERO_MOBFGSR_FG_OUTPUT`;
    - forensic marker `UPSCALER_ROUTE_APPLIED` extended with `fg_source`/`fg_output`.
- `2026-03-06` pass 2:
  - source/output routing finalized under preset-aware policy and SoC trace markers.
- `2026-03-05` hold:
  - Round 14 moved to `gate_hold`.
  - owner override recorded to open Round 15 before final Round 14 closure gates.

## Round 15 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 15 opened as `active`.
  - `R15_EDENAPK_MATRIX` and `R15_EDENAPK_FILE_COVERAGE` initialized.
  - integrated APK-driven Vulkan validation lane:
    - shortcut key: `vulkanValidationLayer`;
    - runtime env: `AERO_VK_VALIDATION_LAYER`, `VK_INSTANCE_LAYERS=VK_LAYER_KHRONOS_validation`;
    - forensic marker `UPSCALER_ROUTE_APPLIED` extended with `vk_validation_layer`.
- `2026-03-06` pass 2:
  - env-layer merge hardened to prevent forensic lane from overwriting upscaler Vulkan validation:
    - forensic now appends to existing `VK_INSTANCE_LAYERS` instead of replacing it;
    - upscaler validation layer is applied only when upscaler backend is active (`Backend != Off`).
- `2026-03-06` hold:
  - Round 15 moved to `gate_hold`.
  - owner override recorded to continue UI hardening before final closure gates.
- `2026-03-06` pass 3:
  - strict VulkanSDK presence guard added for validation request path:
    - `AERO_VK_VALIDATION_GUARD=vulkan_sdk_missing` when validation requested without installed VulkanSDK lane;
    - forensic event expanded with requested/effective/guard fields.
  - compile gate revalidated after guard hardening.
- `2026-03-06` pass 4:
  - IDE/HEX/ASM pass completed for Eden donor (`DEX strings + ELF symbols + AArch64 disassembly anchors`);
  - request/effective validation env split finalized (`AERO_VK_VALIDATION_REQUESTED` + effective layer signal).
- `2026-03-06` closure:
  - Round 15 moved to `closed`.
