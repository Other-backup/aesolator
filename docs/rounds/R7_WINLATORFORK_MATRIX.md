# Round 7 Matrix: `ewt45/winlator-fork`

Date: `2026-03-05`  
Round state: `active`

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
| Selective runtime diagnostics/log surface | `E03_Logcat`, `ExtraFeatures`, `ForeGroundService` | forensic center + runtime diagnostics contracts | `pending` | `integrate` |
| Extra navigation/menu injection model | `XserverNavMenuControl`, `ExtraFeatures` | existing Aeolator nav contracts | `pending` | `reject_with_rationale` |
| Android storage/OBB helper logic | `OBBFinder`, `OBBSelectFragment`, `E11_ManageStorage` | container/storage UX contracts | `pending` | `integrate` |
| Unicode key injection and key input hooks | `E02_KeyInput` | input path (`InputControlsView`, XServer input) | `pending` | `integrate` |
| XServer extension deltas (DRI3/Present/Sync) | `xserver/extensions/*`, request handlers | `cmod/xserver/*` | `pending` | `integrate` |
| Native cpp patch-set and low-level runtime glue | `app/src/main/cpp/*` | native/runtime owner repos (outside app-tree) | `pending` | `reject_with_rationale` |

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
