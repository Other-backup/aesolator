# Round 14 File Coverage: `optiscaler/OptiScaler`

Date: `2026-03-05`  
Round state: `gate_hold`

## Donor Coverage Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/OptiScaler`

Reviewed donor files (control set):
- `README.md`
- `Config.md`
- `Features.md`
- `OptiScaler.ini`

## App-Tree Integration Targets

- `app/src/main/res/layout/shortcut_settings_dialog.xml`
- `app/src/main/res/values/arrays.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

## Coverage Decisions

Integrated:
1. FG source/output split in user profile contract.
2. Runtime env routing for FG source/output.
3. `dlssg_to_fsr3` route bridge flag for runtime packages.
4. Forensic visibility for resolved FG routing.

Rejected (by boundary):
1. Donor hook/wrapper DLL implementations.
2. Native API interception/hook subsystems.
3. Direct OptiScaler binary/runtime architecture merge into app tree.

## Gate Notes

- Java/resource compile gate passed after integration (`:app:compileDebugJavaWithJavac`).
- Runtime routing-consumer gate remains open until `wcp archive` confirms end-to-end behavior.
