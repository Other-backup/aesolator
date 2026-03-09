# Donor Reflective Roadmap (Aeolator)

Generated: `2026-03-05`

## Documentation Sync (2026-03-10)

Repository documentation was normalized to the actual split model:

- `aeolator` is the app source-of-truth
- `freewine11` is the runtime source-of-truth
- `wcp-runtime-lanes` is the archive/release host
- `wcp-graphics-lanes` owns graphics/provider package lanes

This is important because UI and contents work now depends on explicit source
provenance (`WCP Archive` vs `WCPHub`) rather than legacy mixed-source language.

## Scope

This roadmap tracks exhaustive donor analysis coverage (pre/during/post) and controlled extraction into Aeolator runtime layers:

1. `utkarshdalal/GameNative`
2. `KreitinnSoftware/MiceWine-Application`
3. `Open-Wine-Components/umu-launcher`
4. `khanhduytran0/ExagearAndroidX11Server`
5. `olegos2/mobox`
6. `ewt45/termux-x11-fork`
7. `ewt45/winlator-fork`
8. `coffincolors/winlator`
9. APK donor lane: `GameHub-Lite-5.3.3-RC2.apk`

## Current Worklog (2026-03-06)

Closed in tree:
- Runtime/forensic env layering is now explicit and ordered: `graphics -> container -> shortcut -> runtime -> forensic -> override` (forensic no longer gets overwritten by runtime defaults).
- `Global Runtime Profile` is visually and behaviorally decoupled from forensic controls in Settings.
- DRI3 controls were removed from `Forensic Center`; DRI3 remains only in `Graphics Center` (X11 settings lane).
- Forensic policy hint text updated to reflect telemetry-only scope (without DRI3 ownership).
- Main window drawer was replaced by a card-grid home dashboard in `MainActivity` (`3 cards per row`, root dashboard fragment, action-bar back flow instead of drawer toggling).
- `Settings` section layout was migrated off overlay `FrameLayout` labels into native badge + card sections to remove label/content collisions and match the new card UI language.
- Legacy large-form surfaces were restyled to the same badge + card language without hardcoded white label backgrounds:
  - `ContainerDetailFragment`
  - `ShortcutSettingsDialog`
  - `Box64EditPresetDialog`
  - `FEXCoreEditPresetDialog`
  - `ScreenEffectDialog`
- Global `FieldSet` / `FieldSetLabel` styles were re-based onto the new panel/badge system, so remaining secondary forms inherit the same card language without local white-background overrides.
- `ContentDialog` roots now expose a top-left back/close action in the title bar and use the same themed card header surface as the rest of the app.
- The container creation/edit surface now has explicit top-level card sections (`General`, `Runtime Routing`, `Container Framegen Defaults`) instead of loose stacked controls.
- Runtime wrapper/config dialogs were re-based to the same card language:
  - `GraphicsDriverConfigDialog`
  - `DXVKConfigDialog`
  - `WineD3DConfigDialog`
  - `DgVoodooConfigDialog`
- `TaskManagerDialog` list rows and telemetry side-panels were moved to card surfaces, and process rows now render in the same visual language as the rest of the app.
- Theme asset painting now understands tagged `theme_card` / `theme_badge` surfaces and excludes launcher/app-brand icons from automatic tinting.
- Launcher icon assets were rolled back to the pre-upscale baseline so adaptive icon masking no longer inherits the accidental accent-fill variant.
- `Contents` source logic was re-based from raw URL heuristics to explicit feed provenance (`sourceFeed`, `sourceLabel`) so source filtering, labels and source-priority merges stay deterministic across package types.
- `Contents` source selector is now type-aware:
  - `DgVoodoo` / `Vulkan SDK` stay on `Ae.solator mainline` only,
  - `Wine / Proton / DXVK / VKD3D / Box64 / WoWBox64 / FEXCore` keep only the sources that actually provide those lanes.
- `Contents` package ordering is now stable and semantic:
  - installed/runtime-active packages first,
  - then newest `verCode`,
  - then source priority (`Ae.solator > WCPHub > fallback`),
  - then channel priority.
- `Contents` installed-state reporting for `dgVoodoo` is now architecture-aware instead of treating any installed runtime as “all package lanes installed”.

Still open (UI tail queue):
- Monitor-only queue: future dialogs introduced by new feature lanes must keep the same card/badge language and dialog-root back action.

Added scope (2026-03-06, extension batch):
1. Graphics Center feed viewport overflow and non-scrollable artifact list.
2. Compact layout pass: remove shortcut/open-container quick links, reclaim vertical space for driver feeds.
3. Contents policy by package lane:
   - FEX/Box64/WoWBox64: `Release/Nightly` filter enabled.
   - Vulkan SDK: no channel split in UI (`stable-only` lane behavior).
4. DXVK/VKD3D architecture lane cleanup:
   - remove `x86_64` lane selector from DXVK/VKD3D filter scope.
   - keep `native` + `arm64ec` selectors (+ `all`).
5. Wine/Proton filter simplification:
   - channel filter disabled,
   - architecture split retained.

## Execution Contract (Updated)

Effective immediately: `1 round = 1 donor`, strict sequential closure.

Rules:
1. Round opens for one donor only; no parallel donor mixing in implementation.
2. Donor is processed end-to-end (line-manifest sweep, behavior map, transfer map, integration, regression gates).
3. Round closes only when all donor signals are either:
   - integrated into Aeolator, or
   - explicitly rejected with technical reason and owner note.
4. Next donor round starts only after previous donor round is marked `closed`.
5. Owner override is allowed only with explicit queue note (`gate_hold`) and active-round switch record.

Round queue and per-round acceptance criteria are tracked in:
- `docs/DONOR_ROUND_QUEUE.md`
- Latest closed transfer matrix:
  - `docs/rounds/R9_GAMEHUBAPK_MATRIX.md`

All donors were mirrored to local analysis workspace:
- `/home/mikhail/work/donor-analysis/src`
- `/home/mikhail/work/donor-analysis/cache`
- `/home/mikhail/work/donor-analysis/repo-signal-index.txt`
- `/home/mikhail/work/donor-analysis/line-manifest.tsv` (file-level line+hash manifest)

## Donor Snapshot

| Repo | Branch | Last Push (UTC) | Notes |
|---|---|---|---|
| `utkarshdalal/GameNative` | `master` | `2026-03-05T06:12:14Z` | Full app+runtime stack, strong settings/contents UX and test dataset |
| `KreitinnSoftware/MiceWine-Application` | `master` | `2026-01-14T16:46:11Z` | Lorie/X11 input + emulator activity flow |
| `Open-Wine-Components/umu-launcher` | `main` | `2026-03-02T13:28:51Z` | Proton/runtime orchestration, robust update/resume logic |
| `khanhduytran0/ExagearAndroidX11Server` | `master` | `2020-06-06T11:30:08Z` | Legacy but deep gesture/XServer architecture |
| `olegos2/mobox` | `main` | `2024-11-23T06:35:32Z` | Packaging/bootstrap and termux/glibc patch conventions |
| `ewt45/termux-x11-fork` | `master` | `2026-01-31T07:07:29Z` | Loader/signature model + X11 runtime wrapper |
| `ewt45/winlator-fork` | `extra2` | `2024-08-25T02:12:43Z` | Practical extra-feature hooks/logging/nav tweaks |
| `coffincolors/winlator` | `cmod_bionic` | `2025-12-09T22:36:49Z` | cmod-bionic baseline with broad app/runtime coverage |

### Coverage Inventory (Line Manifest)

| Repo | Files | Total Lines |
|---|---:|---:|
| `GameNative` | 1425 | 3900774 |
| `MiceWine-Application` | 341 | 43808 |
| `umu-launcher` | 115 | 20294 |
| `ExagearAndroidX11Server` | 833 | 280112 |
| `mobox` | 21 | 58341 |
| `termux-x11-fork` | 152 | 23831 |
| `winlator-fork` | 852 | 708464 |
| `coffincolors/winlator` | 1132 | 1390182 |

## Reflective Method (Harvard Loop)

### 1) Pre-Integration Reflection
- Build structural index: module map, launcher path, runtime bridge points, X11/input pipeline.
- Build semantic index: wine/proton/dxvk/vkd3d/turnip/box64/fex/contents signals.
- Build risk index: ownership boundary, API shape mismatch, lifecycle hazards.

### 2) In-Integration Reflection
- Port behavior as contract, not as blind file copy.
- Keep each transplant atomic by lane and rollback-ready.
- For each lane update:
  - Add forensic signal marker.
  - Add env contract marker.
  - Add user-visible fallback reason (no silent downgrade).

### 3) Post-Integration Reflection
- Regression gate:
  - container create/start
  - runtime launch path (Wine/Proton)
  - X11 input and pointer flow
  - contents install/update/replace
  - forensic bundle completeness
- Promote only lanes that pass all gates.

## Extraction Matrix (What To Harvest)

### A. Runtime Orchestration Lane

Primary donors:
- `umu-launcher`: `umu/umu_run.py`, `umu/umu_proton.py`, `umu/umu_runtime.py`
- `GameNative`: `WineProtonManagerDialog.kt`, runtime-oriented contents UX

Target Aeolator:
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`
- `app/src/main/java/com/winlator/cmod/xenvironment/components/GuestProgramLauncherComponent.java`
- `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java`

Planned harvest:
- Resume-safe download/install protocol.
- Runtime codename routing (`umu-*`, Proton variants) as explicit contract.
- Prefix bootstrap invariants (symlink/user mapping) as deterministic setup step.

### B. X11/Input/Gesture Lane

Primary donors:
- `MiceWine-Application`: `LorieView.java`, `EmulationActivity.java`, `InputEventSender.java`
- `ExagearAndroidX11Server`: gesture FSM + xserver internals
- `termux-x11-fork`: loader and runtime invocation semantics

Target Aeolator:
- `app/src/main/java/com/winlator/cmod/xserver/*`
- `app/src/main/java/com/winlator/cmod/xconnector/*`
- `app/src/main/java/com/winlator/cmod/widget/TouchpadView.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

Planned harvest:
- Better multi-touch state closure (prevent stuck pointers).
- Clipboard sync hardening.
- Gesture state transitions with explicit mode guards.
- Secure third-party loader pattern (signature checked bridge).

### C. Contents/Packaging Lane

Primary donors:
- `GameNative`: modern Contents and Driver manager flows.
- `mobox`: install/bootstrap and path adaptation patterns.
- `winlator-fork`, `coffincolors/winlator`: practical content UX/compat paths.

Target Aeolator:
- `app/src/main/java/com/winlator/cmod/ContentsFragment.java`
- `app/src/main/java/com/winlator/cmod/contents/ContentProfile.java`
- `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java`
- `app/src/main/java/com/winlator/cmod/AdrenotoolsFragment.java`

Planned harvest:
- Robust manifest-driven package listing.
- Predictable install/update/replace lifecycle.
- Strict trust boundaries for content targets.
- Better failure reason taxonomy surfaced to UI.

### D. Forensic/Diagnostics Lane

Primary donors:
- `winlator-fork`: practical logcat hooks and UI hooks.
- `GameNative`: crash/log dialog and debug surface patterns.

Target Aeolator:
- `app/src/main/java/com/winlator/cmod/ForensicCenterFragment.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`
- `app/src/main/java/com/winlator/cmod/core/ForensicConfig.java`

Planned harvest:
- Runtime log stream normalization and source tags.
- Live forensic toggle-to-env mapping validation.
- Issue-bundle provenance completeness.

Applied in current tree (incremental):
- `TaskManagerDialog` now emits forensic events for lifecycle/actions:
  - `TASKMGR_OPEN`
  - `TASKMGR_CLOSE`
  - `TASKMGR_ACTION` (`bring_to_front`, `kill_process`, `set_affinity_*`)
  - `TASKMGR_DETAILS_OPEN` (details panel open with pid/path/arch/runtime sample fields)
- `TaskManagerDialog` now emits throttled refresh-cycle markers:
  - `TASKMGR_REFRESH` (lane + per-tab visible/total counters + path support capability signal)
- Windows process details now include live runtime metrics sampled from `/proc` by PID (`cpu`, `threads`, `io`, `state`, `cmd`) and a thread preview (`TID/name/state` from `/proc/<pid>/task`), aligned with forensic timeline.
- Windows thread preview now includes scheduling signal (`priority/nice`) to approximate donor `ThreadInfo(priority)` semantics.
- Runtime profile lane now applies SoC-aware `AUTO` resolution (`RuntimeProfileManager.resolveEffectiveProfileId`) using GPU renderer + build chipset hints, and exports forensic env markers:
  - `AERO_RUNTIME_PROFILE_REQUESTED`
  - `AERO_RUNTIME_PROFILE_EFFECTIVE`
  - `AERO_RUNTIME_SOC_CLASS`
- APK `trans_layer` contract diff was applied to runtime env surfaces:
  - `box64_env_vars.json` extended with missing donor keys (`BOX64_DYNACACHE*`, `BOX64_DYNAREC`, `BOX64_DYNAREC_DIV0`, `BOX64_DYNAREC_VOLATILE_METADATA`, `BOX64_CPUNAME/CPUTYPE`, `BOX64_IGNOREINT3`, `BOX64_RDTSC_1GHZ`, `BOX64_UNITY`).
  - `fexcore_env_vars.json` extended with missing donor keys (`FEX_FORCESVEWIDTH`, `FEX_OUTPUTLOG`, `FEX_PROFILESTATS`, `FEX_SILENTLOG`, `FEX_SMCCHECKS`).
  - Preset managers now keep alias compatibility (`BOX64_UNITY` with `BOX64_UNITYPLAYER`, `FEX_SMCCHECKS` with `FEX_SMC_CHECKS`).
- `umu-launcher` reliability pattern was integrated into content delivery:
  - `Downloader.downloadFile()` now supports resume-safe transfer (`Range` + `If-Range` with `ETag/Last-Modified`), `.part` + `.part.meta`, retry loop, and atomic finalization.
  - `ContentsFragment` now blocks install flow on failed download (no invalid URI handoff to installer).
- `MiceWine-Application` touch-state closure pattern was integrated:
  - `TouchpadView` now hard-resets gesture state on `ACTION_CANCEL` and force-releases mouse buttons, preventing stuck L/R button states after pointer cancellation.
  - End-of-gesture cleanup now clears scroll state (`scrolling`, `scrollAccumY`) when the last finger is released.
- `GameNative`-style multi-feed contents selection was strengthened:
  - `ContentsFragment.mergeJsonArrays()` now resolves duplicate entries by merge-key and selects the best candidate using `verCode`, source priority (`Ae.solator > WCPHub > fallback`), and channel priority.
  - This reduces stale/duplicate package rows when multiple donor feeds publish the same lane.
  - `ContentsFragment` now persists a `last-good` merged remote feed cache and uses it as fallback when all live sources fail/unavailable.
- `termux-x11-fork` inspired trust hardening started in feed ingest:
  - `ContentsManager.readRemoteUrl()` now accepts only valid `http/https` URLs with host and without embedded credentials.
  - Non-URL / unsafe scheme feed entries are dropped at parse stage instead of reaching install flow.
  - `ContentsFragment.addRemoteFeed()` now validates feed source URLs with the same policy before any network fetch.
- `ExagearAndroidX11Server` gesture FSM pattern (feature-gated) was integrated into touch input:
  - Added strict two-finger gesture mode FSM in `TouchpadView` (`scroll` / `drag` / `pointer`) behind `touchpad_strict_gesture_fsm`.
  - In strict mode, two-finger processing is single-owner per frame to avoid duplicate dispatch jitter.
  - Mode is reset on pointer-up/cancel boundaries to prevent stale transitions.
- `termux-x11-fork` loader hardening patterns were integrated into launch/content trust gates:
  - Added signed-intent gate for sensitive `XServerDisplayActivity` launches (`shortcut_path` / `shortcut_name` / `disableXinput`) using HMAC signature (`LaunchSecurity`).
  - Internal launch entrypoints now sign intents (`ContainersFragment`, `ShortcutsFragment`, `BigPictureActivity`, and foreground notification resume intent).
  - Remote feed/package ingest hardened to `https` by default (`http` only for localhost), archive-suffix allowlist, and optional `sha256` verification before install.
- `mobox` bootstrap/path normalization pattern was integrated into runtime launcher lane:
  - `GuestProgramLauncherComponent.setBindingPaths()` now canonicalizes and de-duplicates bind paths, dropping invalid/non-directory entries.
  - Normalized bind-set is exported into runtime env markers (`AESO_BIND_PATHS`, `AESO_BIND_PATH_COUNT`) for deterministic bootstrap and forensic correlation.
- `winlator-fork` selective forensic/runtime hooks were integrated into live launcher stream:
  - `XServerDisplayActivity` now attaches lane-scoped runtime log callbacks (`FileDebugLogger`) based on forensic toggles (`wine_loader`, `box64`, `fex_runtime`, `graphics_mesa`, `vulkan_api_dump`, `vulkan_loader`, `dxvk`, `vkd3d`, `pulse`, `alsa`), while issue bundles still accept legacy `turnip_mesa` logs for backward compatibility.
  - Runtime hook activation is emitted as `FORENSIC_STREAM_HOOKS_READY` in forensic JSONL for traceability.
- `coffincolors/winlator` cmod-bionic deltas were folded into runtime/env and issue metadata:
  - `composeLaunchEnvVars()` now exports explicit bionic/runtime markers (`AERO_RUNTIME_LIBC`, `AERO_RUNTIME_ANDROID_BIONIC_ONLY`, SDK/release, ABI list, wow-route).
  - `ForensicIssueComposer` now stores these compatibility markers in `issue-metadata.json` (`runtimeLibc`, `runtimeBionicOnly`, `supportedAbis`, `hostArch`).
- `GameHub-Lite-5.3.3-RC2.apk` winmonitor/perf donor signals were folded into forensic capture contract:
  - `ForensicRuntimeSnapshot` captures host/process runtime state from `/proc` with deterministic contract id (`apk_gamehub_winmonitor_perf_lane_v1`).
  - `ForensicIssueComposer` now packs `runtime-snapshot.json` into issue bundles and emits explicit capture outcome events.

## One Round = One Donor Ledger

Previous mixed-source task ledger is archived as baseline only; execution now follows strict donor rounds.

| Round | Donor | Current State | Closure Rule |
|---:|---|---|---|
| 1 | `utkarshdalal/GameNative` | `gate_hold` | owner override; closure deferred |
| 2 | `KreitinnSoftware/MiceWine-Application` | `closed` | donor sweep completed |
| 3 | `Open-Wine-Components/umu-launcher` | `closed` | donor sweep completed |
| 4 | `khanhduytran0/ExagearAndroidX11Server` | `closed` | donor sweep completed |
| 5 | `olegos2/mobox` | `closed` | donor sweep completed |
| 6 | `ewt45/termux-x11-fork` | `closed` | donor sweep completed |
| 7 | `ewt45/winlator-fork` | `closed` | donor sweep completed |
| 8 | `coffincolors/winlator` | `closed` | donor sweep completed |
| 9 | `GameHub-Lite-5.3.3-RC2.apk` | `closed` | donor sweep completed |

## Reflective Re-Run (Round 2 Gap Matrix, Historical Baseline)

Second-pass reflective read was executed against high-signal donor files (runtime/input/diagnostics/package managers) and compared with current Aeolator tree state.

Re-read set:
- `GameNative`: `TouchGestureSettingsDialog.kt`, `CrashLogDialog.kt`, `WineProtonManagerDialog.kt`, `ContentsManagerDialog.kt`, `DriverManagerDialog.kt`
- `MiceWine-Application`: `AdapterProcess.java`, `InputEventSender.java`, `LorieView.java`, `core/EnvVars.java`
- `umu-launcher`: `umu_run.py`, `umu_runtime.py`, `umu_proton.py`
- `termux-x11-fork`: `MainActivity.java`
- `ExagearAndroidX11Server`: `ViewOfXServer.java`, `TouchEventMultiplexor.java`, `GestureContext.java`
- `mobox`: `README.md` operational runtime guidance lane

### Round 2 Closure Snapshot

Status: `closed` (historical baseline on `2026-03-05`; execution model superseded by `1 round = 1 donor` contract).

| Donor | Capability Signal | Aeolator State | Required Action | Primary Targets |
|---|---|---|---|---|
| `GameNative` | Per-game touch gesture profile matrix (actions, delays, toggles) | `done` | Integrated persisted per-shortcut gesture profile contract and runtime binding (profile + strict FSM + timing thresholds) | `app/src/main/java/com/winlator/cmod/widget/TouchpadView.java`, `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`, `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` |
| `GameNative` | Full-screen crash log viewer with explicit save/export flow | `done` | Forensic log viewer dialog integrated with copy/export path and latest JSONL tail view | `app/src/main/java/com/winlator/cmod/ForensicCenterFragment.java`, `app/src/main/java/com/winlator/cmod/core/ForensicLogger.java`, `app/src/main/res/layout/forensic_log_viewer_dialog.xml` |
| `GameNative` | Import/install mismatch UX (type, variant, reason taxonomy) | `done` | Contents import now surfaces reasoned rejects (`type_mismatch`, `arch_mismatch`, `glibc_variant_unsupported`, trusted-file failures) with forensic event mapping | `app/src/main/java/com/winlator/cmod/ContentsFragment.java`, `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java` |
| `MiceWine-Application` | Process row operability (icon fallback, metrics, inline affinity controls) | `done` | Inline row actions/menu + icon fallback + compact per-process details path are active in task manager list | `app/src/main/java/com/winlator/cmod/winhandler/TaskManagerDialog.java` |
| `MiceWine-Application` | Low-level stale touch-pointer cleanup in event sender path | `done` | Added stale pointer-id release discipline in lower input dispatch path (`ACTION_MOVE` missing-pointer cleanup + `ACTION_CANCEL` full reset) | `app/src/main/java/com/winlator/cmod/widget/InputControlsView.java`, `app/src/main/java/com/winlator/cmod/inputcontrols/ControlElement.java`, `app/src/main/java/com/winlator/cmod/widget/TouchpadView.java` |
| `MiceWine-Application` + `termux-x11-fork` | Input latency and helper overlay stability (`requestUnbufferedDispatch`, helper bounds control) | `done` | Added unbuffered touch dispatch and bounds clamp for overlay controls after layout/orientation changes | `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`, `app/src/main/java/com/winlator/cmod/widget/InputControlsView.java` |
| `umu-launcher` | Install-marker discipline + restore paths when runtime layout is partial/corrupted | `done` | Install-stage marker + interrupted install restore is generalized in `ContentsManager.finishInstallContent()` for updatable content lanes | `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java` |
| `umu-launcher` | Explicit no-proton/passthrough runtime route contract | `done` | Runtime contract ingestion now exports route hints/env markers (`AERO_RUNTIME_ROUTE`, `AERO_RUNTIME_NO_PROTON`, `AERO_RUNTIME_PASSTHROUGH_TOOL`) | `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` |
| `ExagearAndroidX11Server` | Multi-listener touch multiplexing contract and zoom-transform synchronization | `done` | Event fan-out fallback and transformation guardrails are enforced in touch/input dispatch path | `app/src/main/java/com/winlator/cmod/widget/TouchpadView.java`, `app/src/main/java/com/winlator/cmod/widget/InputControlsView.java`, `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` |
| `mobox` | SoC/runtime operational defaults (OOM, dynarec, DRI fallback) surfaced as first-class profiles | `done` | SoC-class runtime policy matrix promoted to first-class profiles and exposed in settings/global runtime lane | `app/src/main/java/com/winlator/cmod/runtimeprofile/RuntimeProfileManager.java`, `app/src/main/java/com/winlator/cmod/SettingsFragment.java` |

### Round 2 Prioritized Execution Order (No-Regression)

1. Gesture profile contract (data model + UI + TouchpadView binding). (`done`)
2. Forensic crash/log viewer (read/copy/export path). (`done`)
3. Install mismatch taxonomy and variant/arch guardrails. (`done`)
4. Task manager inline process actions. (`done`)
5. Input low-level stale-pointer cleanup. (`done`)
6. Unbuffered dispatch + helper overlay bounds controls. (`done`)
7. Runtime/content install-marker and restore contract extension. (`done`)
8. Passthrough/native runtime route visibility. (`done`)
9. X11 zoom/transform synchronization guardrails. (`done`)
10. SoC operational profile promotion. (`done`)

## APK Donor Lane (GameHub-Lite-5.3.3-RC2)

Source:
- `/home/mikhail/Загрузки/GameHub-Lite-5.3.3-RC2.apk`

Workspace:
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2`

Stages:
1. Raw unpack inventory (`raw/`, ABI, DEX, SO map).
2. Full jadx decompile (`jadx/`) and source index.
3. Native ELF symbol and strings inventory for each `.so`.
4. Hex-level sampling:
   - DEX headers and section boundaries
   - native library headers and exported symbol tables
5. Convert useful findings into lane-ready contracts, not raw copy.

Status:
- `closed` (APK donor lanes finalized in strict round mode with integrate/reject decisions).

Artifacts already generated:
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/inventory.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/analysis-summary.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/dex-files.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/dex-headers.hex`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/so-files.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/elf-summary.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/elf-dynamic-symbols.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/native-strings-scan.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/jadx-class-index.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/jadx-package-frequency.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/jadx-keyword-hotspots.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/jadx-relevant-class-candidates.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/dex-winmonitor-string-signals.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/jadx-single-winmonitor/sources/com/winemu/core/server/winmonitor/WinMonitorClient.java`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/jadx-single-winmonitor-embedded/sources/com/winemu/core/embedded/WinMonitorEmbedded.java`

### APK Interim Deep Findings (Current Snapshot)

`jadx` is still in progress, but current source snapshot is already usable for lane planning:

- Decompiled class index currently contains `20905` Java sources.
- Strong app-specific namespace detected: `com/winemu/*` (`171` classes), with major clusters:
  - `com/winemu/core/server` (`49`)
  - `com/winemu/core/utils` (`18`)
  - `com/winemu/core/input` (`17`)
  - `com/winemu/core/regedit` (`15`)
  - `com/winemu/core/gamepad` (`14`)
  - `com/winemu/core/trans_layer` (`9`)

High-value candidates mapped to Aeolator lanes:
- Task Manager lane:
  - `com/winemu/core/server/winmonitor/*` (`ProcessListResponse`, `ThreadListResponse`, `KillProcessResponse`).
  - `com/winemu/core/embedded/WinMonitorEmbedded` + `winmonitor/WinMonitorClient` (embedded `winmonitor.exe`, UDP+JSON client, 5s timeout).
  - Adoption target: real-time Windows-process tabs and action model in Aeolator task manager UI.
  - In-tree stabilization applied: `WinHandler` now resets datagram max length before receive and applies `ByteBuffer.limit(receivedLength)` to prevent truncated/stale `GET_PROCESS` parsing.
- Runtime profile lane:
  - `com/winemu/core/trans_layer/*` (`Box64Config`, `FEXConfig`, `Template*Config`).
  - Adoption target: richer Box64/FEX profile matrix and preset serialization contract.
- X11/input lane:
  - `com/winemu/ui/X11View.java`, `com/winemu/core/input/*`, `com/winemu/core/server/XServer.java`.
  - Adoption target: pointer/touch routing stabilization and view-to-server event bridge.
- Graphics/DRI lane:
  - `com/winemu/openapi/GPUConfig.java`, `DirectRenderingMode.java`, `core/DirectRenderingStateListener.java`.
  - Adoption target: explicit DRI mode contract and UI-state synchronization in graphics center.
- Forensic lane:
  - `com/winemu/core/server/environment/plugins/PerfPlugin.java`, `RenderDocPlugin.java`, `ProcessRun.java`.
  - Adoption target: structured runtime telemetry markers routed into forensic center.

## Backlog (Actionable, Ordered)

Phase 1 (already integrated in-tree):
1. Finalize donor code index artifacts (module + signal maps) across mirrored sources.
2. Port resume-safe runtime/content transfer protocol.
3. Port baseline touch/input stabilization and gesture FSM feature gate.
4. Integrate signed launch bridge and feed trust hardening.
5. Merge baseline forensic markers and runtime stream hooks.

Phase 2 (Round 2 closure queue, all closed):
6. Ship persisted per-game touch gesture profile schema and UI.
7. Add full forensic crash/log viewer with export path.
8. Extend install mismatch taxonomy (type/arch/variant/trust) in Contents UX.
9. Add inline process actions in task manager list rows.
10. Add lower-level stale touch-pointer cleanup in input dispatch path.
11. Add unbuffered dispatch and helper-overlay bounds control for X11 surface interactions.
12. Generalize install-marker + restore protocol to all runtime/content package lanes.
13. Add explicit no-proton/passthrough runtime route contract visibility.
14. Harden X11 event fan-out and zoom/transformation synchronization guardrails.
15. Promote SoC operational presets (OOM/dynarec/DRI fallback) to first-class runtime profiles.
16. Complete APK donor lane extraction beyond current targeted namespaces.
17. Map remaining APK trans-layer contracts into runtime profile and forensic env surfaces.
18. Run lane-by-lane no-regression matrix and move each promoted lane out of queue.

## Current UI Closure Pass (Ae.solator Mainline)

Closed in-tree during the current card-migration round:
- Removed remaining active legacy `AlertDialog` surfaces from main app flows and Big Picture flows; active dialog routing is now `ContentDialog`-based.
- Removed the last active runtime `DrawerLayout` / `NavigationView` shell from `XServerDisplayActivity`; the running-container control surface is now a native card-based side drawer triggered from runtime back/gesture paths.
- Migrated secondary dialogs to card/badge layout surfaces:
  - environment variable editor
  - container selector
  - storage info
  - content info / untrusted content review
  - CPU affinity picker
  - debug log viewer shell
  - download / preloader overlays
  - input controls session dialog
  - shortcut activity dialog
  - Wine install options
  - analog stick / gyroscope tuning dialogs
- Reworked helper surfaces to match the same visual contract:
  - content file rows
  - CPU list rows
  - image picker card
  - About header/body cards
  - runtime side drawer for live containers
  - terminal activity shell
  - input-controls fragment sections
- Re-skinned task manager container/process telemetry surfaces off legacy `bordered_panel` shells into the same card treatment used by forensic and graphics center.
- Removed dead legacy `GamepadConfiguratorDialog` + `dialog_gamepad_configurator.xml` to avoid keeping an unused parallel UI surface in-tree.
- Normalized dialog copy/hints and removed remaining hardcoded dialog texts from active layout surfaces.
- Fixed dialog-adjacent logic bugs found during migration:
  - storage usage progress no longer uses truncated integer division
  - gyro preview now unregisters its sensor listener on dismiss and reflects live slider values
  - input controls session dialog no longer depends on recursive manual text recolor
  - running-container menu actions no longer depend on legacy drawer state and remain callable through the new in-layout runtime drawer
  - task-manager host telemetry strings moved to resources instead of hardcoded labels
- Final polish pass closed remaining active legacy UI seams:
  - controls editor toolbar is now a card-based runtime editor surface with consistent action buttons and helper copy
  - control-element popup settings were rebuilt into sectioned card groups for general settings, bindings and appearance
  - container, shortcut, controller-binding, external-controller, drive and env-var rows now render as card-style list surfaces instead of flat legacy rows
  - file-picker and extra-keys editor content were rewritten into clearer, friendlier task-specific forms
  - Big Picture landscape settings and details now use resource-backed labels instead of hardcoded strings
  - runtime notification text, frame-rating copy and several fallback placeholders moved to string resources
  - generic card and badge assets were redrawn into a unified surface-driven visual system used across dialogs, cards, selectors and helper surfaces
  - remaining generic UI copy in About, Graphics Center, X11, task manager, file-picker, control editor and upscaler screens was rewritten for clearer first-use guidance
  - fallback “not installed / not set” surfaces now use an honest dash marker instead of misleading placeholder text
  - generic screens were detached from temporary forensic visual aliases; forensic assets are now left only in the forensic center and forensic log viewer
  - the dead legacy comment tail in `styles.xml` and stale commented UI code paths were removed to keep the tree logically clean
  - final static UI sweep removed the remaining menu hardcodes, right/left-only layout anchors and hardcoded layout colors from active app surfaces
  - static verification is now clean for active app UI: no hardcoded layout/menu strings, no raw layout hex backgrounds, no `left/right` layout anchors outside legacy preference templates, no active `DrawerLayout` / `NavigationView` / `AlertDialog` paths
  - the only remaining UI validation step is a live device-pass for the running container shell and overlays when an ADB device is attached
  - safe code-cleanup pass removed orphan helper/resources with no in-tree references: `PatchElf`, `ContainerDiscovery`, `main_menu_header`, `wine_install_options_dialog`, `installed_wine_list_item`, `checkbox_spinner`
  - `main_menu.xml` and `xserver_menu.xml` were intentionally retained as ID-contract resources because active code still depends on their generated `R.id.main_menu_*` constants
  - `ContentsManager.applyContent()` no longer carries an empty wine-only branch, `ProcessHelper` no longer mirrors every debug line into stdout by default, and a small Big Picture comment/noise tail was removed without touching runtime behavior
  - a clean upstream baseline was parked in `/home/mikhail/wcp-sources/winlator-bionic-upstream` and used to confirm/fix the original `MidiHandler`, `WinHandler` and `GuestProgramLauncherComponent` problem points rather than patching blindly against the dirty fork tree
  - `MidiHandler` now has a real `MIDI_LONG` path, safe reset/close handling, larger datagram intake, defensive short-packet guards and explicit synth-init failure logging
  - `WinHandler.bringToFront()` no longer relies on a fragile CJK-overflow workaround; the process name is now UTF-8 packed into the fixed packet budget safely
  - fullscreen/XInput/sim-touch shortcut overrides are now parsed consistently through `Shortcut.getExtraBoolean()` across settings, launch-time env construction and runtime container shell
  - toolbar title color on main shell surfaces was aligned with the shared surface palette instead of raw platform white constants
  - final graphics-provider pass split runtime handling into real provider lanes: `turnip-vulkan` for Vulkan route and `freedreno-opengl` for GL route, with package-driven companion selection instead of filename heuristics
  - source-built `AeTurnip` and `AeOpenGLDriver` archives now publish a shared adrenotools-compatible metadata contract (`providerLane`, `driverRoute`, `artifactName`, forensic hooks, archive format/layout), and `AeOpenGLDriver` now ships a real `meta.json` so Graphics Center can install it through the same ZIP path as Turnip
  - installed Graphics Center drivers now expose provider/source/route metadata from package contracts, not only a guessed architecture string
  - runtime startup now restores then reapplies managed GL overlays from installed graphics ZIPs, so `freedreno-opengl` works as the actual GL fallback lane while `turnip-vulkan` remains the Vulkan lane
  - forensic runtime capture and issue bundles now use the neutral `graphics_mesa` prefix for Mesa/Turnip/freedreno/zink activity while still keeping backward-compatible `turnip_mesa` bundle pickup

## No-Regression Contract

A lane cannot be marked complete until all checks pass:
- container lifecycle: create/update/start
- launch lifecycle: Wine + Proton
- wrappers lifecycle: DXVK + VKD3D (+ fallback path)
- graphics lifecycle: Turnip/OpenGL lane selection
- x11 lifecycle: input, cursor, clipboard, orientation
- forensic lifecycle: expected markers visible in live diagnostics + issue bundle
