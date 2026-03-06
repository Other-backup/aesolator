# Round 13 File Coverage: `proqaz2-design/Frame-generation-`

Date: `2026-03-05`  
Round state: `gate_hold`

## Donor Coverage Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/Frame-generation-`

Reviewed donor files (control set):
- `README.md`
- `app/src/main/java/com/framegen/app/MainActivity.kt`
- `app/src/main/java/com/framegen/app/engine/FrameGenEngine.kt`
- `app/src/main/cpp/framegen_types.h`
- `app/src/main/cpp/pipeline/timing_controller.h`
- `app/src/main/cpp/pipeline/timing_controller.cpp`

## App-Tree Integration Targets

- `app/src/main/res/layout/shortcut_settings_dialog.xml`
- `app/src/main/res/values/arrays.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

## Coverage Decisions

Integrated:
1. Framegen mode selection contract.
2. Thermal guard contract.
3. Mode-driven model scale / quality / frame-budget policy env hints for runtime lane.

Rejected (by boundary):
1. Donor standalone Android app flow (Shizuku/ADB service model).
2. Native Vulkan layer interception engine direct transplantation.
3. Donor-specific JNI/service runtime architecture.

## Gate Notes

- Java/resource compile gate passed after integration (`:app:compileDebugJavaWithJavac`).
- Runtime policy consumer gate remains open until `wcp archive` lane confirms end-to-end use.
