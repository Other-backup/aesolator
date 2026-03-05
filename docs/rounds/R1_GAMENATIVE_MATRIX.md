# Round 1 Matrix: `utkarshdalal/GameNative`

Date: `2026-03-05`  
Round state: `gate_hold`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/GameNative`

Primary code surfaces:
- `app/src/main/java/app/gamenative/**` (UI/runtime/manager contracts)
- `app/src/main/java/com/winlator/**` (runtime/content/X11 bridge contracts used by donor)
- `app/src/main/assets/**` (runtime presets, wrapper/driver metadata)

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| Gesture profile matrix | `TouchGestureSettingsDialog.kt`, `TouchGestureConfig.kt` | `ShortcutSettingsDialog`, `TouchpadView`, `XServerDisplayActivity` | `baseline_integrated` | `revalidate_full_sweep` |
| Crash/log UX | `CrashLogDialog.kt`, `CrashHandler.kt` | `ForensicCenterFragment`, `ForensicLogger`, issue-bundle lane | `baseline_integrated` | `revalidate_full_sweep` |
| Contents mismatch taxonomy | `ContentsManagerDialog.kt`, `WineProtonManagerDialog.kt` | `ContentsFragment`, `ContentsManager` | `baseline_integrated` | `revalidate_full_sweep` |
| Wine/Proton manager orchestration | `WineProtonManagerDialog.kt` | runtime route/env policy in launcher path | `integrated` | `integrate` |
| Driver manager semantics | `DriverManagerDialog.kt`, `GraphicsTab.kt` | graphics center + driver lane UX/contracts | `integrated` | `integrate` |
| Process/game monitor model | `GameProcessInfo.kt`, monitor-related flow | `TaskManagerDialog`, runtime monitor contract | `integrated` | `integrate` |
| Launch dependency contracts | `utils/launchdependencies/**` | launch preflight and package dependency checks | `integrated` | `integrate` |
| GOG-specific dependency steps | `GogScriptInterpreterDependency.kt`, `GogScriptInterpreterPreLaunchStep.kt` | (no GOG lane in Aeolator) | `rejected` | `reject_with_rationale` |
| Box64/FEX preset UX linkage | `Box64PresetsDialog.kt`, `FEXCorePresetsDialog.kt` | `RuntimeProfileManager`, preset dialogs/fragments | `integrated` | `integrate` |
| DRI3 / graphics bridge details | `com/winlator/xserver/extensions/DRI3Extension.java` | DRI3 controls + forensic mapping | `integrated` | `integrate` |
| Runtime content profile metadata | `com/winlator/contents/ContentProfile.java` | `ContentProfile`, contents UI badges/filters | `integrated` | `integrate` |
| Dialog behavior parity (settings) | `ContainerConfigDialog.kt`, `AdvancedTab.kt`, `WineTab.kt` | container settings UX + runtime option exposure | `integrated` | `integrate` |
| Asset-driven defaults and presets | `app/src/main/assets/**` | default profiles/assets in Aeolator runtime lanes | `rejected` | `reject_with_rationale` |

## Closure Criteria For Round 1

Round 1 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` (with rationale).
2. Regression gates pass for affected lanes.
3. Round summary is committed into docs with explicit donor coverage report.

## Progress Log

### 2026-03-05 / Pass 1

- Runtime launch-env assembly in `GuestProgramLauncherComponent.execGuestProgram()` hardened:
  - removed field/local `envVars` shadowing conflict;
  - removed null-unsafe `this.envVars.has(...)` access path;
  - merged external env overlays into a single `launchEnv` contract;
  - preserved overlay precedence while enforcing browser/clipboard toggles and MangoHUD sanitization.
- Impact lane:
  - `Wine/Proton manager orchestration` moved from `partial` to `in_progress`.

### 2026-03-05 / Pass 2

- Donor-style launch dependency framework landed:
  - added native contracts `LaunchDependency`, `PreLaunchStep`, `LaunchDependencyCallbacks`;
  - added `LaunchDependencyRegistry` runner with forensic event emission and runtime registration API;
  - bound dependency + prelaunch runners into `GuestProgramLauncherComponent.start()`.
- Impact lane:
  - `Launch dependency contracts` moved from `missing` to `in_progress`.
- Rationale decisions:
  - `GogScriptInterpreter*` logic is donor-specific to GOG manifests/download manager and is explicitly rejected for Aeolator core until a GOG runtime lane exists.

### 2026-03-05 / Pass 3

- Wine/Proton runtime orchestration hardening:
  - `WineInfo.fromIdentifier()` now resolves `Proton` profiles via `ContentsManager` (not only `Wine`) and keeps parsed type aligned with selected content profile.
  - launch dependency runner switched from passive logging to gating mode (`runDependencies()` returns `boolean`); launcher aborts start when required runtime dependencies fail.
- Donor launch dependency pattern made operational:
  - added built-in dependency `WineRuntimePresenceDependency` (validates selected runtime package/profile layout before launch).
- Impact lane:
  - `Wine/Proton manager orchestration` remains `in_progress` (major runtime path integrated; manager-screen parity still open).
  - `Launch dependency contracts` remains `in_progress` (framework + first concrete dependency landed).

### 2026-03-05 / Pass 4

- Wine/Proton selection parity uplift:
  - container Wine-version spinner now includes both locally installed `Wine` and `Proton` entries (deduplicated) rather than Wine-only list.
  - this closes a major donor parity gap from `WineProtonManagerDialog` behavior (single manager surface for both lanes).
- Runtime identifier resolution uplift:
  - `WineInfo.fromIdentifier()` now correctly resolves and preserves Proton type when profile source is `CONTENT_TYPE_PROTON`.

### 2026-03-05 / Pass 5

- Launch dependency contracts completed to donor-grade preflight:
  - launcher dependency runner now returns a structured result (`success + dependency_id + message`) instead of blind boolean;
  - start path now fails fast with explicit user-facing reason instead of silent skip;
  - new mandatory dependency `emulator_runtime_presence` validates `box64/wowbox64/fexcore` payload availability before launch.
- Contents profile parity uplift for Wine/Proton packages:
  - `ContentProfile` now supports `proton` profile key (`MARK_PROTON`);
  - `ContentsManager.readProfile()` now reads `proton` section with `wine` fallback;
  - install preflight now applies Wine layout validation to both Wine and Proton package types.
- Impact lane:
  - `Launch dependency contracts` moved to `integrated`.
  - `Runtime content profile metadata` moved from `partial` to `integrated`.

### 2026-03-05 / Pass 6

- Wrapper runtime preflight added into launch dependency lane:
  - new mandatory dependency `wrapper_runtime_presence` validates selected wrapper payloads (`DXVK`, `VKD3D`, `dgVoodoo`) before launch;
  - dependency resolves container/shortcut override config and checks installed content + embedded fallback + already staged runtime files.
- Impact lane:
  - `Driver manager semantics` moved from `partial` to `in_progress` (runtime contract parity improved; UI parity still open).

### 2026-03-05 / Pass 7

- Reflective status re-check against current tree:
  - `TaskManagerDialog` now exceeds donor monitor baseline (Windows + Linux tabs, realtime telemetry, arch lanes, per-process forensic details), therefore `Process/game monitor model` moved to `integrated`.
  - `Box64/FEX preset` lane moved from `partial` to `integrated` (S8+G1 presets and runtime profile env linkage are present in dialogs and runtime merge path).

### 2026-03-05 / Pass 8

- Runtime lane closure review:
  - `Wine/Proton manager orchestration` promoted to `integrated` (selection + launcher resolution + dependency preflight + proton-profile metadata parity are now wired end-to-end).
  - `DRI3 / graphics bridge` promoted to `integrated` (mode/present-wait/sw-wsi controls are exposed and propagated into runtime env + forensic settings surface).

### 2026-03-05 / Pass 9

- Settings dialog parity uplift:
  - container edit mode now allows runtime lane switch in Wine/Proton selector when versions are present (previously hard-disabled in edit path);
  - this removes an orchestration dead-end and aligns container settings behavior with manager-driven runtime flow.
- Impact lane:
  - `Dialog behavior parity` moved from `partial` to `in_progress`.

### 2026-03-05 / Pass 10

- Content/version UX and driver-lane parity fixes:
  - normalized installed content version presentation to use canonical `profile.verName` (without `-verCode` suffix leakage) across `FEXCore`, `Box64/WOWBox64`, `DXVK` selectors;
  - this removes mixed-version artifacts in settings dialogs and aligns runtime selection UX with manager semantics.
- Round 1 state transition:
  - `Driver manager semantics` moved to `integrated`;
  - `Dialog behavior parity` moved to `integrated`;
  - `Asset-driven defaults` marked `rejected` for R1: donor visual/default assets are intentionally not imported into Aeolator due product branding divergence and high regression risk in established UX contracts.
  - matrix moved to `gate` (waiting only for regression gates/CI confirmation before `closed`).

### 2026-03-05 / Pass 11

- Gate formalization:
  - added explicit `R1_GAMENATIVE_GATE.md` checklist with mandatory CI + runtime smoke checks for `gate -> closed`;
  - docs index updated to keep round-closure workflow discoverable and deterministic.

### 2026-03-05 / Pass 12

- Wrapper payload gate hardening:
  - `wrapper_runtime_presence` now validates DXVK/VKD3D readiness strictly via package/archive lanes only;
  - removed generic `system32` DLL fallback from readiness checks to prevent false-positive launch preflight.

### 2026-03-05 / Pass 13

- Owner override transition:
  - round execution switched to `Round 2` before final R1 closure;
  - R1 state frozen as `gate_hold` (no additional rerun in this phase).
