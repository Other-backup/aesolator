# Round 4 Matrix: `khanhduytran0/ExagearAndroidX11Server`

Date: `2026-03-05`  
Round state: `active`

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
| Gesture FSM transition discipline | `GestureStateMachine/*` | `TouchpadView`, input gesture policy lane | `in_progress` | `integrate` |
| Multi-listener touch fan-out | `TouchEventMultiplexor.java` | `InputControlsView`, `TouchpadView`, dispatch order | `in_progress` | `integrate` |
| View/surface coordinate transform guardrails | `widgets/viewOfXServer/*` | `XServerDisplayActivity`, `XServerView`, transform updates | `in_progress` | `integrate` |
| X11 lock/focus/window lifecycle hardening | `axs/xserver/*` | `xserver/*`, window/focus handling lanes | `pending` | `integrate` |
| Legacy container/install recipe semantics | `ed/*`, `InstallRecipe`, container layers | `ContentsManager`, container lane | `pending` | `reject_with_rationale` |
| Touch controls presets/overlays model | `ed/controls/*` | `InputControlsView`, profile/preset mapping | `pending` | `integrate` |

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
