# Donor Round Queue (Strict Mode)

Date: `2026-04-26`

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
| 16 | `AndreVto/proton-wine` | `pending` | Black Diamond runtime donor for Proton 11 ARM64EC, prefix-pack contract, Android-facing build scripts, and bootstrap parity |
| 17 | `MaxsTechReview/WinNative` | `active` | app-side donor refreshed to `b4297a39ade7ca46e3505b2c26ceafa5f0f69146` on `2026-04-26`; runtime/input and renderer deltas under transfer |
| 18 | `palazos/winlatorCmod` | `active` | app-side donor refreshed to `9a27d00aeaaed884624355539c0d352769d285d9`; controller/gamepad false-positive lane transferred and still under audit |

## 2026-04-26 Ae.solator App-Donor Freshness Gate

Scope correction:
- Current implementation scope is `aesolator` only.
- Wine/Proton source donors are FreeWine/runtime-source inputs and are excluded from this app-side freshness gate.
- No `freewine11` or `wcp-runtime-lanes` source-root change is part of this gate.

Fresh app-side donor refs:
- `utkarshdalal/GameNative`: `cbea7f75f7bd5b1dd5f665148c91251cf4a89b39`, `2026-04-24T17:55:31+05:30`, `Feat/eos overlay utkarsh (#1286)`.
- `MaxsTechReview/WinNative`: `b4297a39ade7ca46e3505b2c26ceafa5f0f69146`, `2026-04-26T00:37:03-05:00`, `fix: restore Range Button functionality in input controls (#287)`.
- `palazos/winlatorCmod`: `9a27d00aeaaed884624355539c0d352769d285d9`, `2026-04-20T12:36:39-03:00`, `Drop Locale import from uinput-fpc filter`.
- `EtchDroid`: `6873618e6683a47394c567857a293b863e405c8a`, `2026-04-19T23:24:51+02:00`, utility donor only.
- `libaums`: `52167edc43ef6bc1f91bea8fba99dbba431caa84`, `2025-06-12T16:33:19+02:00`, USB utility donor only.

Transferred in this app-side gate:
- GameNative XInput2 raw motion/button event surface plus 7-button pointer mapping.
- GameNative renderer post-process pipeline: offscreen scene buffer, render-scale effects, source texture filters, FSR1 EASU/RCAS, scaling mode, and vivid shader classes.
- GameNative stale Wine process hard-kill prelaunch stage, merged with local native lifecycle reaper and forensic events.
- WinNative `RANGE_BUTTON` binding exemption, generalized so unbound controls no longer steal touch events while range buttons remain dynamic.
- GameNative X11 pointer-grab confinement and donor ClipCursor tolerance are merged into the local X server route.
- palazos / Winlator-family controller hotplug stability is merged through local `FakeInputWriter.softRelease()` plus physical-device dedupe, while shutdown keeps hard cleanup.
- WinNative `patchelf` native JNI rewrite surface is merged into the local `libpatchelf.so` owner instead of leaving Java-only ELF mutation logic.
- Fresh user forensic profile-install failures are closed as a class:
  foreign profile schema aliases, bionic-vs-glibc donor label precedence, and
  `10.0.99-arm64ec-*` rolling/bleeding-edge capability suffix aliases now resolve through one identity path.
- WCP/WCP.xz runtime-package intake is hardened as a class:
  suffix-aware archive probing covers `.wcp`, `.wcp.xz`, `.wcp.zst`, `.txz`,
  `.tzst`, raw `.tar`, and `.zip`; profile-less Wine/Proton payloads are
  synthesized from root shape; bionic/glibc ownership is resolved from payload
  sentinels plus ELF markers before canonical install-root binding.
- Content-install forensics now records archive format, root shape, runtime
  classifier scores/signals, and install-root diagnostics so user-submitted
  logs can explain package recognition and broken-install state without a
  local reproduction.
- Decision ledger:
  `docs/DONOR_ZERO_APP_LEDGER_2026-04-26.md`.

Current verification:
- `./gradlew :app:testDebugUnitTest --tests com.winlator.cmod.contents.ContentProfileParserTest --tests com.winlator.cmod.contents.ContentProfileIdentityTest --tests com.winlator.cmod.inputcontrols.InputDeviceHeuristicsTest --tests com.winlator.cmod.core.ProcessHelperSplitCommandTest --tests com.winlator.cmod.core.TarCompressorUtilsTest :app:assembleDebug --stacktrace --no-build-cache`
- Result: `BUILD SUCCESSFUL` on `2026-04-26`.
- Expanded package-intake gate:
  `./gradlew :app:testDebugUnitTest --tests com.winlator.cmod.contents.ImportedContentHeuristicsTest --tests com.winlator.cmod.contents.ContentProfileParserTest --tests com.winlator.cmod.contents.ContentProfileIdentityTest --tests com.winlator.cmod.inputcontrols.InputDeviceHeuristicsTest --tests com.winlator.cmod.core.ProcessHelperSplitCommandTest --tests com.winlator.cmod.core.TarCompressorUtilsTest :app:assembleDebug --stacktrace --no-build-cache`
- Expanded result: `BUILD SUCCESSFUL` on `2026-04-26`.
- Local APK install: `adb -s 192.168.43.4:39057 install -r app/build/outputs/apk/debug/app-debug.apk` returned `Success`.

## R10-R14 Normalized Status (2026-03-13)

This queue entry alone is not enough to explain current state, because `gate_hold`
here means:
- app-side transfer is already integrated;
- donor closure is still incomplete.

Normalized interpretation:
- `R10`: app-side closed, donor round still open on package/runtime consumption + closure gates.
- `R11`: app-side closed, donor round still open on package-side pacing validation + closure gates.
- `R12`: app-side closed, donor round still open on wrapper-side `DLSSGTOFSR3_*` validation + closure gates.
- `R13`: app-side closed, donor round still open on mode/thermal consumer validation + device gates.
- `R14`: app-side closed, donor round still open on FG source/output wrapper validation + closure gates.
- Ownership split for the remaining closure work:
  - app-side continuation in `aeolator` -> app owner
  - runtime/archive/wrapper closure gates -> runtime owner

Reference:
- `docs/R10_R14_STATUS_SNAPSHOT.md`

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
- `2026-04-25`:
  - Round 1 remains historically `gate_hold`, but under the Chapter 2 /
    public-release / donor-rootfs contract it is now also the mandatory
    re-open target after the next queued runtime donors.
  - The reason is narrower than "redo GameNative":
    current closure work is specifically rootfs/runtime/layout/public-release
    parity against the live product, not a replay of the older soft-handoff.

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

## Round 16 Progress Snapshot

- `2026-04-25` pass 1:
  - Round 16 re-opened under the Chapter 2 Black Diamond donor wall:
    `AndreVto/proton-wine`, `MaxsTechReview/WinNative`,
    `utkarshdalal/GameNative`, `palazos/winlatorCmod`.
  - transferred the first reflected owner-class union into live source:
    `controller contamination` + `content/runtime install forensic gap`.
  - centralized controller classifier now rejects
    `uinput-fpc` and `uinput-xiaomi` fingerprint/uinput impostors before slot
    assignment and emits rejected-device forensic rows.
  - centralized content/runtime diagnostics now surface
    `expected/resolved/runtime roots`, `profile.json`, payload presence,
    alias-resolution state, and broken-reason truth for `Broken install` and
    related import/install failures.
- `2026-04-25` state:
  - Round 16 remains `active`.
  - remaining frontier is no longer the blind app-side install/control fog;
    it is the wider donor/source runtime wall still open in `freewine11` and
    the next post-fix device proof wave.
- `2026-04-26` native C/C++ pass:
  - `MaxsTechReview/WinNative` Android-host native donor surface is now
    active-source closed for transferable app-layer code:
    - `process_lifecycle.c` accepted as an adapted `libwinlator.so` JNI owner
      with `PR_SET_CHILD_SUBREAPER`, explicit windowed reaping, and Java
      process-tree shutdown integration;
    - `xz/native_xz_stream.c` plus embedded XZ decoder accepted as
      `libaero_native_xz.so`, exposed through
      `NativeXzInputStream`, and wired into file-backed `.txz/.wcp.xz`
      extraction with Java decoder fallback for non-file streams;
    - Vulkan OEM ICD dependency preload accepted into the local
      Adrenotools-aware `vulkan.c` path rather than replacing the stronger
      local driver-routing contract;
    - `fakeinput` / `evshim` remain merged through the local
      `aero_fakeinput` and `aero_evshim` library owners.
  - `utkarshdalal/GameNative` native app-layer deltas are resolved by owner
    class:
    - `xconnectorpatch` accepted into local `xconnector_epoll.c` as
      EINTR-safe socket/epoll/fd-ownership hardening;
    - split `extras/gpu_image.c` and `extras/vulkan.c` are
      `preserve-local` because local `winlator/gpu_image.c` and `vulkan.c`
      already carry the stronger package namespace, fd/native-handle path,
      Adrenotools guard, API-level Vulkan version handling, and OEM crypto
      preload;
    - `arraytools.c` is `reject-app-native` for this APK pass because it is a
      decompiled generic container helper with no Java/JNI caller and unsafe
      element-free ownership semantics for a shared app `.so`.
  - `palazos/winlatorCmod` native app-layer deltas are resolved by owner
    class:
    - `fakeinput.cpp` is merged through the current `aero_fakeinput` owner,
      which also carries poll/select/ppoll coverage and force-feedback routing;
    - `winhandler.c` is `defer-guest-runtime-owner`, not an Android-host NDK
      import: it is Windows guest code and belongs to the FreeWine/prefix
      runtime lane if accepted.
  - verification evidence:
    - `./gradlew :app:testDebugUnitTest --tests com.winlator.cmod.core.TarCompressorUtilsTest --tests com.winlator.cmod.core.ProcessHelperSplitCommandTest --tests com.winlator.cmod.contents.ContentProfileParserTest --tests com.winlator.cmod.contents.ImportedContentHeuristicsTest --tests com.winlator.cmod.contents.RemoteFeedPayloadLoaderTest --tests com.winlator.cmod.inputcontrols.InputDeviceHeuristicsTest :app:assembleDebug --stacktrace --no-build-cache`
      succeeded;
    - emitted APK contains `libaero_native_xz.so`, `libwinlator.so`,
      `libaero_fakeinput.so`, `libaero_evshim.so`, and
      `libaero_android_sysvshm.so`;
    - `llvm-readelf` shows `NativeXzInputStream` JNI exports plus
      `xz_dec_catrun`, `xz_crc32`, and `xz_crc64` in `libaero_native_xz.so`.
