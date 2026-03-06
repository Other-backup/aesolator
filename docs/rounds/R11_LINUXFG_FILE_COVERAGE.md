# Round 11 File Coverage: `xXJSONDeruloXx/linux-fg`

Date: `2026-03-05`  
Round state: `gate_hold`

## Donor Coverage Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/linux-fg`

Reviewed donor files (control set):
- `readme.md`
- `src/main.cpp`
- `src/scaler.hpp`
- `src/frame_manager.hpp`

## App-Tree Integration Targets

- `app/src/main/res/layout/shortcut_settings_dialog.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

## Coverage Decisions

Integrated:
1. Target FPS shortcut parameter and runtime env export.
2. Interpolation factor shortcut parameter and runtime env export.
3. Forensic visibility for both values in runtime route event.

Rejected (by boundary):
1. Standalone donor application architecture (X11 capture + Vulkan compute renderer).
2. Direct donor shader embedding to app tree without dedicated runtime package lane.
3. Native pipeline copy of `window_capture`/`vulkan_context` in Java app layer.

## Gate Notes

- Compile gate passed after integration (`:app:compileDebugJavaWithJavac` with explicit SDK env).
- Runtime consumer gate remains open until `wcp archive` packages consume new env contract.
