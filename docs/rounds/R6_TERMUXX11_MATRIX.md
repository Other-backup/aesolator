# Round 6 Matrix: `ewt45/termux-x11-fork`

Date: `2026-03-05`  
Round state: `closed`

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
| Loader/binder launch discipline | `MainActivity`, `CmdEntryPoint`, `ICmdEntryInterface` | launchdeps/launcher runtime contract | `integrated` | `integrate` |
| Input strategy and gesture fanout | `input/TouchInputHandler`, `InputEventSender`, `TapGestureDetector`, `SwipeDetector` | `TouchpadView`, `InputControlsView`, input runtime lane | `integrated` | `integrate` |
| Clipboard sync ownership/focus policy | `LorieView` clipboard path | `XServerDisplayActivity`, forensic clipboard lane | `integrated` | `integrate` |
| Accessibility/key interception + ADB guidance | `utils/KeyInterceptor`, `LoriePreferences` | Forensic center + diagnostic UX contracts | `integrated` | `integrate` |
| Native Xtrans/socket path patching | `app/src/main/cpp/patches/Xtrans.patch` and cpp patch-set | xserver native/runtime repos (outside app-tree) | `rejected` | `reject_with_rationale` |
| Donor shell-loader implementation model | `shell-loader/*` | app launch security and intent contract | `integrated` | `integrate` |

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

### 2026-03-05 / Pass 2

- Loader trust lane finalized in app-tree:
  - `LaunchSecurity` trust-state API added (`getXServerLaunchTrustState`).
  - `XServerDisplayActivity` now emits launch trust marker:
    - `XSERVER_LAUNCH_TRUST_EVAL`
  - reject path now carries deterministic diagnostics fields:
    - `trust_state`
    - `adb_diagnostics_cmd`
- Clipboard ownership policy lane now emits:
  - `XSERVER_CLIPBOARD_POLICY_APPLIED` (`share_android_clipboard`, `open_with_android_browser`).

### 2026-03-05 / Pass 3

- Shell-loader model transfer bounded to contract-level behavior only:
  - signed launch contract and intent trust validation remain in app-tree;
  - no shell script copy from donor.
- Key-interceptor/accessibility lane integrated as diagnostics guidance contract in forensic events (ADB-oriented troubleshooting hook in launch reject path).
- Native `Xtrans`/cpp patch-set explicitly rejected for app-tree and forwarded to native runtime owner lane.

### 2026-03-05 / Closure

- Round 6 moved to `closed`.
- All transfer rows are finalized as `integrated` or `rejected` with rationale.
