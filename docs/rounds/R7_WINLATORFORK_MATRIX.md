# Round 7 Matrix: `ewt45/winlator-fork`

Date: `2026-03-05`  
Round state: `closed`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/winlator-fork`

Primary code surfaces:
- `app/src/main/java/com/ewt45/winlator/*`
- `app/src/main/java/com/winlator/MainActivity.java`
- `app/src/main/java/com/winlator/XServerDisplayActivity.java`
- `app/src/main/java/com/winlator/ContainerDetailFragment.java`
- `app/src/main/java/com/winlator/SettingsFragment.java`
- `app/src/main/java/com/winlator/winhandler/*`
- `app/src/main/java/com/winlator/contentdialog/*`
- `app/src/main/java/com/winlator/xserver/extensions/*`
- `app/src/main/cpp/*`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| XServer lifecycle + PiP continuity semantics | `XServerDisplayActivity` (`onStart/onStop`, PiP flow) | `cmod/XServerDisplayActivity` | `integrated` | `integrate` |
| Selective runtime diagnostics/log surface | `E03_Logcat`, `ExtraFeatures`, `ForeGroundService` | forensic center + runtime diagnostics contracts | `integrated` | `integrate` |
| Extra navigation/menu injection model | `XserverNavMenuControl`, `ExtraFeatures` | existing Aeolator nav contracts | `rejected` | `reject_with_rationale` |
| Android storage/OBB helper logic | `OBBFinder`, `OBBSelectFragment`, `E11_ManageStorage` | container/storage UX contracts | `integrated` | `integrate` |
| Unicode key injection and key input hooks | `E02_KeyInput` | input path (`InputControlsView`, XServer input) | `integrated` | `integrate` |
| XServer extension deltas (DRI3/Present/Sync) | `xserver/extensions/*`, request handlers | `cmod/xserver/*` | `integrated` | `integrate` |
| Native cpp patch-set and low-level runtime glue | `app/src/main/cpp/*` | native/runtime owner repos (outside app-tree) | `rejected` | `reject_with_rationale` |

## Closure Criteria For Round 7

Round 7 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. App-tree integrations are contract-level ports (no blind donor copy).
3. Native runtime rows are explicitly bounded to owning repos/layers.
4. Round summary is committed with coverage closure.

## Progress Log

### 2026-03-05 / Pass 1

- Round 7 opened after Round 6 closure.
- Base transfer lanes initialized for lifecycle/PiP continuity, diagnostics hooks, OBB/storage helpers, key-input lane, and xserver extension deltas.
- Inventory anchors fixed from donor tree:
  - total files: `852`
  - policy/code files: `548`
  - `com/ewt45/winlator` lane: `29`
  - `com/winlator` java lane: `211`
  - native cpp lane: `285`
  - res/manifest lane: `172`

### 2026-03-05 / Pass 2

- Lifecycle continuity lane integrated in app-tree:
  - `XServerDisplayActivity.onPause()` now keeps runtime active when entering PiP.
  - runtime pause path (`savePlaytimeData`, timer stop, `pauseAllWineProcesses`) is applied only for non-PiP pause.
  - forensic marker added for PiP continuity:
    - `XSERVER_PIP_CONTINUITY` (`wine_paused=false`, `playtime_timer_kept=true`).

### 2026-03-05 / Pass 3

- Key-input lane moved to explicit multi-handler fanout:
  - `dispatchKeyEvent()` now returns OR-composed handling result for:
    - `InputControlsView`
    - `WinHandler`
    - `XServer.keyboard`
    - `super.dispatchKeyEvent`
- This removes handler-order loss where controller-handled events could be dropped by chained negative logic.

### 2026-03-05 / Pass 4

- Diagnostics/log-surface row closed as integrated:
  - existing lane-scoped forensic runtime callbacks and stream marker contract already cover donor intent (`FORENSIC_STREAM_HOOKS_READY`).
- Extra navigation/menu injection row rejected:
  - donor submenu overlay is not ported to avoid UI contract drift and duplicated controls in Aeolator.
- XServer extension row closed as integrated:
  - donor `DRI3/Present/Sync` baseline is already covered/superseded in current `cmod/xserver/extensions` lane.
- Native cpp row rejected for app-tree:
  - low-level cpp glue is explicitly bounded to native/runtime owner repos, not Java app layer.

### 2026-03-05 / Pass 5

- Storage helper lane moved to in-progress integration:
  - `MainActivity` all-files-access prompt now emits forensic markers:
    - `STORAGE_ALL_FILES_ACCESS_PROMPT`
    - `STORAGE_ALL_FILES_ACCESS_OPEN_SETTINGS`
    - `STORAGE_ALL_FILES_ACCESS_DECLINED`
- This ports donor `E11_ManageStorage` intent (permission orchestration visibility) into Aeolator forensic-first contract without cloning donor UI fragments.

### 2026-03-05 / Pass 6

- Key-input lane continued in xserver keyboard path:
  - `Keyboard.onKeyEvent()` no longer consumes `ACTION_MULTIPLE` events blindly.
  - non-handled key actions now return `false` instead of unconditional `true`.
- This removes silent event swallowing and keeps composition events available to framework/IME path while preserving explicit down/up injection.

### 2026-03-05 / Pass 7

- Storage/OBB lane finalized as integrated with bounded scope:
  - donor storage-permission intent is covered by Aeolator all-files-access flow (`MainActivity`) plus forensic visibility.
  - donor direct OBB fragment flow is intentionally not copied as-is, because Aeolator uses contents/package contract as primary distribution path.
- Key-input lane finalized as integrated with bounded scope:
  - stable fanout and keyboard-consumption fixes were applied.
  - donor stub-keycode unicode remap hack was not copied due high regression risk in keymap semantics.

### 2026-03-05 / Closure

- Round 7 moved to `closed`.
- All transfer rows are finalized as `integrated` or `rejected` with rationale.
