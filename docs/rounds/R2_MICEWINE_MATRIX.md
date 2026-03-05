# Round 2 Matrix: `KreitinnSoftware/MiceWine-Application`

Date: `2026-03-05`  
Round state: `closed`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/MiceWine-Application`

Primary code surfaces:
- `app/src/main/java/com/micewine/emu/input/**`
- `app/src/main/java/com/micewine/emu/LorieView.java`
- `app/src/main/java/com/micewine/emu/adapters/AdapterProcess.java`
- `app/src/main/java/com/micewine/emu/fragments/TaskManagerFragment.java`
- `app/src/main/java/com/micewine/emu/core/EnvVars.java`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| Process row operability and inline actions | `AdapterProcess.java`, `TaskManagerFragment.java` | `TaskManagerDialog`, process row layouts, telemetry actions | `integrated` | `integrate` |
| Touch stale-pointer release discipline | `input/InputEventSender.java` | `InputControlsView`, `ControlElement`, `TouchpadView` | `integrated` | `integrate` |
| Surface lifecycle and display ratio handling | `LorieView.java` | `XServerView`, `XServerDisplayActivity`, screen resize path | `integrated` | `integrate` |
| Clipboard/IME runtime bridge guards | `LorieView.java` | `XServerDisplayActivity`, input/clipboard bridge lane | `integrated` | `integrate` |
| Runtime env variable policy semantics | `core/EnvVars.java`, `fragments/Box64SettingsFragment.java` | `RuntimeProfileManager`, settings/runtime env merge | `integrated` | `integrate` |
| RAT/rootfs package manager semantics | `core/RatPackageManager.java`, `fragments/Rat*` | `ContentsManager`, package workflows | `rejected` | `reject_with_rationale` |
| Overlay/controller mapper behavior | `activities/VirtualControllerOverlayMapper.java`, `views/VirtualController*` | `InputControlsView`, shortcut/controller config lane | `integrated` | `integrate` |

## Closure Criteria For Round 2

Round 2 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. Regression gates pass for touched lanes (`task_manager`, `input`, `x11_surface`, `runtime_env`).
3. Round summary is committed with explicit donor coverage report.

## Progress Log

### 2026-03-05 / Pass 1

- Round 2 opened as active donor round by owner override.
- Transfer matrix initialized from current donor mirror and mapped to Aeolator lanes.

### 2026-03-05 / Pass 2

- Task-manager row telemetry uplift (donor-style density):
  - Windows process rows now show live `RAM + CPU` in list view, not RAM-only.
  - CPU value is sampled from Linux telemetry lane by process PID for real-time visibility.

### 2026-03-05 / Pass 3

- Task-manager refresh cadence aligned to donor baseline:
  - update timer interval switched from `1000ms` to `750ms` for tighter real-time responsiveness.

### 2026-03-05 / Pass 4

- Lorie-like surface lifecycle hardening integrated:
  - on orientation/config change, `XServerView` now performs explicit layout refresh (`requestLayout + setSizeFromLayout`).
  - input overlay lane requests relayout before redraw to keep mapping stable after rotate.

### 2026-03-05 / Pass 5

- Clipboard/runtime guard semantics integrated:
  - launch env now removes inherited clipboard/browser bridge vars when corresponding switches are off;
  - explicit forensic/runtime markers added: `AERO_RUNTIME_BROWSER_BRIDGE`, `AERO_RUNTIME_CLIPBOARD_SYNC`.

### 2026-03-05 / Closure

- Round 2 closed.
- Rejected signal rationale:
  - `RAT/rootfs package manager semantics` is donor-specific (`Rat*` workflow) and conflicts with Aeolator WCP Contents contract; port was intentionally rejected.
