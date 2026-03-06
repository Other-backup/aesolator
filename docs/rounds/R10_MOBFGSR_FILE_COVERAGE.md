# Round 10 File Coverage: `Mob-FGSR/MobFGSR`

Date: `2026-03-05`  
Round state: `gate_hold`

## Donor Coverage Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/MobFGSR`

Reviewed donor files (control set):
- `README.md`
- `src/offscreen_renderer.h`
- `src/offscreen_renderer.cpp`
- `resources/SuperResolution/*`
- `resources/FrameGeneration/*`

## App-Tree Integration Targets

- `app/src/main/res/layout/shortcut_settings_dialog.xml`
- `app/src/main/res/values/arrays.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

## Coverage Decisions

Integrated:
1. SR/FG mode controls and generated-frames contract.
2. Render-scale control.
3. Threshold env placeholders for MobFGSR runtime lane.
4. Frame-generation guard semantics in runtime signal/forensic layer.

Rejected (by boundary):
1. Standalone OpenGL compute renderer application architecture.
2. Offline image-sequence IO pipeline (`load/save` PNG loop).
3. Direct shader file import from donor without runtime package lane.

## Gate Notes

- Java/resource compile gate passed after integration (`:app:compileDebugJavaWithJavac` with explicit SDK env).
- Full runtime consumption gate remains open until `wcp archive` runtime lane starts consuming `AERO_MOBFGSR_*`.
