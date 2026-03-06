# Round 15 File Coverage: `Eden-Android-9d2341eaea-standard.apk`

Date: `2026-03-05`  
Round state: `closed`

## Donor Coverage Inventory

Donor artifact:
- `/home/mikhail/Загрузки/Eden-Android-9d2341eaea-standard.apk`

Reviewed artifact slices (control set):
- APK manifest metadata (`aapt dump badging`)
- APK file list (`unzip -l`)
- native libs list (focus on `libVkLayer_khronos_validation.so`)

## App-Tree Integration Targets

- `app/src/main/res/layout/shortcut_settings_dialog.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java`

## Coverage Decisions

Integrated:
1. Vulkan validation layer toggle in shortcut contract.
2. Runtime env bridge for validation layer activation.
3. Forensic field for validation layer state.

Rejected (by boundary):
1. Donor emulator-native core libraries and services.
2. Donor APK internal UI/runtime architecture transplantation.
3. Direct reuse of donor binary payloads in app tree.

## Gate Notes

- Java/resource compile gate passed after integration (`:app:compileDebugJavaWithJavac`).
- Additional compile gate revalidated after env-merge hardening (`2026-03-06`, same compile target).
- Runtime guard hardening pass applied (`2026-03-06`):
  - validation request requires installed Vulkan SDK lane;
  - guard marker exported as `AERO_VK_VALIDATION_GUARD`.
- App-tree R15 gate is closed; device-level soak remains out-of-band.
