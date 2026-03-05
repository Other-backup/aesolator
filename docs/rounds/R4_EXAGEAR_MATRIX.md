# Round 4 Matrix: `khanhduytran0/ExagearAndroidX11Server`

Date: `2026-03-05`  
Round state: `closed`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/ExagearAndroidX11Server`

Primary code surfaces:
- `com/eltechs/axs/GestureStateMachine/**`
- `com/eltechs/axs/TouchEventMultiplexor.java`
- `com/eltechs/axs/widgets/viewOfXServer/**`
- `com/eltechs/axs/xserver/**`
- `com/eltechs/ed/controls/**`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| Gesture FSM transition discipline | `GestureStateMachine/*` | `TouchpadView`, input gesture policy lane | `integrated` | `integrate` |
| Multi-listener touch fan-out | `TouchEventMultiplexor.java` | `InputControlsView`, `TouchpadView`, dispatch order | `integrated` | `integrate` |
| View/surface coordinate transform guardrails | `widgets/viewOfXServer/*` | `XServerDisplayActivity`, `XServerView`, transform updates | `integrated` | `integrate` |
| X11 lock/focus/window lifecycle hardening | `axs/xserver/*` | `xserver/*`, window/focus handling lanes | `integrated` | `integrate` |
| Legacy container/install recipe semantics | `ed/*`, `InstallRecipe`, container layers | `ContentsManager`, container lane | `rejected` | `reject_with_rationale` |
| Touch controls presets/overlays model | `ed/controls/*` | `InputControlsView`, profile/preset mapping | `integrated` | `integrate` |

## Closure Criteria For Round 4

Round 4 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. Regression gates pass for touched lanes (`x11_input`, `gesture_fsm`, `surface_transform`, `focus_window`).
3. Round summary is committed with explicit donor coverage report.

## Progress Log

### 2026-03-05 / Pass 1

- Round 4 opened after Round 3 closure.
- Matrix initialized from local donor mirror and mapped to Aeolator touch/X11 lanes.

### 2026-03-05 / Pass 2

- Donor sweep detail fixed to concrete anchors:
  - touch fan-out: `axs/TouchEventMultiplexor.java` -> `InputControlsView` / `TouchpadView` dispatch consistency lane.
  - transform guardrails: `widgets/viewOfXServer/TransformationHelpers.java`, `TransformationDescription.java` -> `TouchpadView.updateXform()` and display transform lane.
  - gesture state lattice: `axs/GestureStateMachine/*` (30 files) -> strict gesture FSM path in `TouchpadView`.
  - X11 lifecycle candidate set: `xserver/FocusManager*`, `LocksManager*`, `Window*`, `XServer.java` -> Aeolator `xserver/*` focus/lock/window lanes.
- Round remains `active`; no forced transfer before per-lane no-regression checks.

### 2026-03-05 / Pass 3

- X11 lifecycle hardening landed in Aeolator xserver lane:
  - canonical lock ordering in `XServer.MultiXLock` to reduce lock-order deadlock risk;
  - focus revert safety in `WindowManager.revertFocus()` for null/parent-missing paths;
  - focus assignment normalization in `WindowManager.setFocus()` (mapped-window guard + null-safe revert mode);
  - `DesktopHelper.setFocusedWindow()` now guards null `WinHandler` before `bringToFront`.
- Transfer decisions finalized:
  - `ed/*` container/install recipe lane rejected for Aeolator because runtime/package pipeline is Contents-driven and already has strict install/update contract; donor recipe layer would duplicate and conflict with current package lifecycle.

### 2026-03-05 / Closure

- Round 4 moved to `closed`.
- Closure result: all signals finalized as `integrated` or `rejected_with_rationale`.
