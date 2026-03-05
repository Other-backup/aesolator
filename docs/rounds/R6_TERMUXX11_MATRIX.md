# Round 6 Matrix: `ewt45/termux-x11-fork`

Date: `2026-03-05`  
Round state: `active`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/termux-x11-fork`

Primary code surfaces:
- `app/src/main/java/com/termux/x11/MainActivity.java`
- `app/src/main/java/com/termux/x11/CmdEntryPoint.java`
- `app/src/main/java/com/termux/x11/LorieView.java`
- `app/src/main/java/com/termux/x11/input/*`
- `app/src/main/java/com/termux/x11/utils/KeyInterceptor.java`
- `app/src/main/cpp/patches/*`
- `shell-loader/*`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| Loader/binder launch discipline | `MainActivity`, `CmdEntryPoint`, `ICmdEntryInterface` | launchdeps/launcher runtime contract | `in_progress` | `integrate` |
| Input strategy and gesture fanout | `input/TouchInputHandler`, `InputEventSender`, `TapGestureDetector`, `SwipeDetector` | `TouchpadView`, `InputControlsView`, input runtime lane | `in_progress` | `integrate` |
| Clipboard sync ownership/focus policy | `LorieView` clipboard path | `XServerDisplayActivity`, forensic clipboard lane | `in_progress` | `integrate` |
| Accessibility/key interception + ADB guidance | `utils/KeyInterceptor`, `LoriePreferences` | Forensic center + diagnostic UX contracts | `pending` | `integrate` |
| Native Xtrans/socket path patching | `app/src/main/cpp/patches/Xtrans.patch` and cpp patch-set | xserver native/runtime repos (outside app-tree) | `pending` | `reject_with_rationale` |
| Donor shell-loader implementation model | `shell-loader/*` | app launch security and intent contract | `pending` | `integrate` |

## Closure Criteria For Round 6

Round 6 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. App-tree integrations are contract-level ports (no blind donor copy).
3. Any native xserver patch rows are explicitly bounded to owning repos/layers.
4. Round summary is committed with coverage closure.

## Progress Log

### 2026-03-05 / Pass 1

- Round 6 opened after Round 5 closure.
- Base transfer lanes initialized for loader/binder discipline, touch input, clipboard/focus semantics, and native patch boundaries.
- Inventory anchors fixed from donor tree:
  - total files: `152`
  - java input lane: `7`
  - java x11 core lane: `5`
  - java utils lane: `5`
  - native cpp lane: `36`
  - shell-loader lane: `12`
