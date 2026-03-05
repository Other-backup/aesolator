# Donor Reflective Roadmap (Aeolator)

Generated: `2026-03-05`

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
  - `XServerDisplayActivity` now attaches lane-scoped runtime log callbacks (`FileDebugLogger`) based on forensic toggles (`wine_loader`, `box64`, `fex_runtime`, `turnip_mesa`, `vulkan_api_dump`, `vulkan_loader`, `dxvk`, `vkd3d`, `pulse`, `alsa`).
  - Runtime hook activation is emitted as `FORENSIC_STREAM_HOOKS_READY` in forensic JSONL for traceability.
- `coffincolors/winlator` cmod-bionic deltas were folded into runtime/env and issue metadata:
  - `composeLaunchEnvVars()` now exports explicit bionic/runtime markers (`AERO_RUNTIME_LIBC`, `AERO_RUNTIME_ANDROID_BIONIC_ONLY`, SDK/release, ABI list, wow-route).
  - `ForensicIssueComposer` now stores these compatibility markers in `issue-metadata.json` (`runtimeLibc`, `runtimeBionicOnly`, `supportedAbis`, `hostArch`).

## One Source = One Task Ledger

| Source | Task | State |
|---|---|---|
| `GameHub-Lite-5.3.3-RC2.apk` | WinMonitor task-manager contract (`process/path/thread/runtime`) | `done` |
| `GameHub-Lite-5.3.3-RC2.apk` | `trans_layer` env schema parity for Box64/FEX | `done` |
| `Open-Wine-Components/umu-launcher` | Resume-safe package download protocol | `done` |
| `KreitinnSoftware/MiceWine-Application` | X11 input stabilization import (pointer/touch state closure) | `done` |
| `khanhduytran0/ExagearAndroidX11Server` | Gesture FSM feature-gated adoption | `done` |
| `ewt45/termux-x11-fork` | External bridge signature + loader hardening | `done` |
| `utkarshdalal/GameNative` | Contents UX and runtime orchestration convergence | `done` |
| `olegos2/mobox` | Bootstrap/path normalization for package/runtime lanes | `done` |
| `ewt45/winlator-fork` | selective forensic/runtime hooks (no donor regressions) | `done` |
| `coffincolors/winlator` | cmod-bionic compatibility deltas triage | `done` |

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
- `in_progress` (jadx partial complete; process ended with OOM at ~65%, targeted namespaces extracted).

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

1. Finalize donor code index artifacts for all repos (module + signal maps).
2. Produce per-donor API surface contract (what can be adopted without semantic break).
3. Open `runtime-orchestration` branch and port umu-style resume-safe runtime fetch protocol.
4. Port prefix bootstrap invariants with Aeolator path model.
5. Port MiceWine touch/input stabilization into Aeolator input lane.
6. Port selected Exagear gesture FSM fragments behind feature flags.
7. Integrate secure loader pattern from termux-x11-fork for external bridge entrypoints.
8. Normalize contents manifest + package update flow with explicit replace semantics.
9. Lift GameNative manager UX patterns into Aeolator Graphics/Contents where compatible.
10. Unify forensic markers and route all runtime diagnostics through forensic center.
11. Complete APK donor lane and map findings to runtime/X11/content lanes.
12. Extract and adapt `com/winemu/core/server/winmonitor` contract for task manager real-time process model.
13. Extract and adapt `com/winemu/core/trans_layer` contract for Box64/FEX profile schema.
14. Extract and adapt `com/winemu/openapi` graphics contract for DRI state/UI sync.
15. Run no-regression matrix before promoting each lane.

## No-Regression Contract

A lane cannot be marked complete until all checks pass:
- container lifecycle: create/update/start
- launch lifecycle: Wine + Proton
- wrappers lifecycle: DXVK + VKD3D (+ fallback path)
- graphics lifecycle: Turnip/OpenGL lane selection
- x11 lifecycle: input, cursor, clipboard, orientation
- forensic lifecycle: expected markers visible in live diagnostics + issue bundle
