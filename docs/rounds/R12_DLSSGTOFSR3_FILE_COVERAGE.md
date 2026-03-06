# Round 12 File Coverage: `Nukem9/dlssg-to-fsr3`

Date: `2026-03-05`  
Round state: `gate_hold`

## Donor Coverage Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/dlssg-to-fsr3`

Reviewed donor files (control set):
- `README.md`
- `resources/dlssg_to_fsr3.ini`
- `source/maindll/Util.cpp`
- `source/maindll/FFFrameInterpolator.cpp`

## App-Tree Integration Targets

- `app/src/main/res/layout/shortcut_settings_dialog.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

## Coverage Decisions

Integrated:
1. Debug overlay flag in shortcut/runtime/env contracts.
2. Debug tear-lines flag in shortcut/runtime/env contracts.
3. Interpolated-only flag in shortcut/runtime/env contracts.
4. Translator-compatible `DLSSGTOFSR3_*` env bridge in runtime lane.

Rejected (by boundary):
1. Donor-specific DLL interposer binaries and hook loaders.
2. Direct NGX wrapper code transplantation to Android app layer.
3. Direct donor binary/runtime replacement in app tree.

## Gate Notes

- Java/resource compile gate passed after integration (`:app:compileDebugJavaWithJavac`).
- Runtime consumer gate remains open until `wcp archive` wrappers consume bridge env vars end-to-end.
