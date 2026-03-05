# Donor Reflective Round 2 Gaps (Aeolator)

Date: `2026-03-05`

This document captures the second-pass reflective delta after re-reading donor sources against the current Aeolator tree.

Note:
- This file is kept as a historical Round 2 snapshot.
- Active execution now follows strict donor rounds (`1 round = 1 donor`) tracked in `docs/DONOR_ROUND_QUEUE.md`.

## Method

Compared:
- donor source behavior contracts (`GameNative`, `MiceWine-Application`, `umu-launcher`, `termux-x11-fork`, `ExagearAndroidX11Server`, `mobox`)
- current Aeolator implementation state in `/home/mikhail/wcp-sources/aeolator/app/src/main/java`

Outcome model:
- `done`: already integrated and verified in tree
- `partial`: integrated fragment exists, but contract is incomplete
- `missing`: no equivalent implementation in tree

## Gap Ledger

### 1) GameNative: per-game gesture profiles
- Donor signal:
  - `app/gamenative/ui/component/dialog/TouchGestureSettingsDialog.kt`
  - `app/gamenative/data/TouchGestureConfig.kt`
- Aeolator state: `done`
  - persisted per-shortcut gesture profile matrix is integrated (`profile + strict FSM + tap/scroll timing thresholds`) and bound at launch.
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/widget/TouchpadView.java`
  - `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
  - `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

### 2) GameNative: crash log viewer UX
- Donor signal:
  - `app/gamenative/ui/component/dialog/CrashLogDialog.kt`
- Aeolator state: `done`
  - dedicated forensic log viewer is integrated with copy/export flow and latest JSONL tail rendering.
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/ForensicCenterFragment.java`
  - `app/src/main/java/com/winlator/cmod/core/ForensicLogger.java`
  - `app/src/main/java/com/winlator/cmod/core/ForensicIssueComposer.java`

### 3) GameNative: install mismatch taxonomy
- Donor signal:
  - `app/gamenative/ui/screen/settings/WineProtonManagerDialog.kt`
  - strict type checks and mismatch surface in manager flow
- Aeolator state: `done`
  - explicit mismatch reason mapping is active in Contents import flow (`type`, `arch`, `glibc variant`, trust/profile failures).
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/ContentsFragment.java`
  - `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java`

### 4) MiceWine: process row operability
- Donor signal:
  - `com/micewine/emu/adapters/AdapterProcess.java`
  - compact process row with icon, cpu/ram and inline action menu.
- Aeolator state: `done`
  - list-row operability is dense and interactive (`quick-end`, inline menu, icon fallback, compact metrics/details).
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/winhandler/TaskManagerDialog.java`

### 5) MiceWine: low-level stale pointer cleanup
- Donor signal:
  - `com/micewine/emu/input/InputEventSender.java`
  - explicit stale touch-pointer release behavior.
- Aeolator state: `done`
  - stale pointer-id release now exists in lower input dispatch lane (`InputControlsView` + `ControlElement`), including `ACTION_CANCEL` full cleanup.
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/widget/InputControlsView.java`
  - `app/src/main/java/com/winlator/cmod/inputcontrols/ControlElement.java`
  - `app/src/main/java/com/winlator/cmod/widget/TouchpadView.java`

### 6) termux-x11 + MiceWine: unbuffered dispatch and helper bounds
- Donor signal:
  - `termux-x11-fork/app/.../MainActivity.java` (`requestUnbufferedDispatch`, helper bounds lifecycle patterns)
- Aeolator state: `done`
- Required transfer:
  - unbuffered dispatch on initial touch;
  - keep helper overlays in screen bounds after layout/orientation transitions.
- Target files:
  - `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`
  - `app/src/main/java/com/winlator/cmod/widget/InputControlsView.java`

### 7) umu-launcher: install marker + restore discipline
- Donor signal:
  - `umu/umu_runtime.py`, `umu/umu_run.py` (marker, restore, fallback, recovery)
- Aeolator state: `done`
  - install-stage marker + interrupted-install recovery is generalized in `ContentsManager.finishInstallContent()`.
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java`
  - `app/src/main/java/com/winlator/cmod/contents/*Manager.java`

### 8) umu-launcher: passthrough/no-proton contract
- Donor signal:
  - `umu/umu_proton.py`, `umu/umu_run.py` (native/passthrough runtime route semantics)
- Aeolator state: `done`
  - wrapper contract ingestion exports explicit passthrough/no-proton route markers (`AERO_RUNTIME_ROUTE`, `AERO_RUNTIME_NO_PROTON`, `AERO_RUNTIME_PASSTHROUGH_TOOL`).
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`
  - `app/src/main/java/com/winlator/cmod/core/ForensicConfig.java`

### 9) Exagear: touch fan-out and zoom-transform guardrails
- Donor signal:
  - `.../TouchEventMultiplexor.java`
  - `.../widgets/viewOfXServer/ViewOfXServer.java`
  - `.../GestureStateMachine/GestureContext.java`
- Aeolator state: `done`
- Required transfer:
  - fan-out fallback order and zoom/transform guardrails are integrated in touch/input path.
- Target files:
  - `app/src/main/java/com/winlator/cmod/xserver/*`
  - `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

### 10) mobox: operational SoC presets
- Donor signal:
  - `mobox/README.md` (OOM, dynarec, DRI fallback operational matrix)
- Aeolator state: `done`
  - runtime operational defaults are promoted to first-class SoC profile matrix with runtime provenance env markers.
- Required transfer:
  - closed in current pass.
- Target files:
  - `app/src/main/java/com/winlator/cmod/core/RuntimeProfileManager.java`
  - `app/src/main/java/com/winlator/cmod/SettingsFragment.java`

## Execution Rule

No donor transfer is promoted as `done` until lane regression gates pass:
- container create/update/start
- Wine/Proton launch
- X11 input + orientation
- graphics route selection
- forensic trace + issue bundle provenance completeness

## One Donor = One Point (Execution Queue)

1. `GameNative` -> Forensic crash/log viewer with copy/export (`done`)
2. `MiceWine-Application` -> Task manager inline row operations (`done`)
3. `umu-launcher` -> Generic install-marker + interrupted install recovery (`done`)
4. `termux-x11-fork` -> Unbuffered touch dispatch path (`done`)
5. `ExagearAndroidX11Server` -> X11 touch event fan-out and transform guardrails (`done`)
6. `mobox` -> SoC operational runtime presets surfaced as first-class profile policy (`done`)
7. `GameNative` -> Import/install mismatch taxonomy (type/variant validation) (`done`)
8. `GameNative` -> Per-game gesture profile matrix (`done`)
9. `MiceWine-Application` -> Stale pointer-id cleanup in lower input dispatch (`done`)
10. `termux-x11-fork` -> Unbuffered touch dispatch + overlay bounds clamp (`done`)

Current pass implementation anchors:
- `GameNative`: `ForensicCenterFragment` + `ForensicLogger` + `forensic_log_viewer_dialog.xml`.
- `GameNative`: `ContentsFragment` import taxonomy (`type mismatch` + `arch mismatch` + `glibc` guard + forensic reject reasons).
- `umu-launcher`: `ContentsManager.finishInstallContent()` marker/recovery lane.
- `termux-x11-fork`: `XServerDisplayActivity.startTouchscreenTimeout()` unbuffered touch dispatch.
- `ExagearAndroidX11Server`: `TouchpadView.updateXform()` transformation guardrails.
- `mobox`: `RuntimeProfileManager.getEnvVars()` operational OOM/dynarec/DRI policy matrix.
- `MiceWine-Application`: `TaskManagerDialog` long-press menu + quick-end row action (`BTQuickEnd`).
- `GameNative`: per-shortcut gesture matrix (`ShortcutSettingsDialog` + `TouchpadView` + `XServerDisplayActivity`).
- `MiceWine-Application`: lower-lane stale pointer release (`InputControlsView` + `ControlElement.releaseIfPointerMissing/resetTouchState`).
- `termux-x11-fork`: touch unbuffered dispatch + overlay bounds clamp (`XServerDisplayActivity` + `InputControlsView`).
- `ExagearAndroidX11Server`: move-event fan-out order hardened in `InputControlsView` (single-pass fallback to `TouchpadView`).
