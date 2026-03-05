# Winlator Patch Stack Reflective Audit

Generated: `2026-03-04T21:10:02Z`

## Snapshot

- Patch count: `9`
- Unique touched source files: `312`
- Diff volume across stack: `+20382 / -4877`
- Mean files touched per patch: `36.44`

## Numbering Contract

- Numbering contract is clean (`NNNN-` unique prefixes).

## High-Overlap Hotspots

- `app/src/main/java/com/winlator/cmod/ContentsFragment.java` touched by `3` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`, `0007-graphics-center-color-polish.patch`
- `app/src/main/res/values/colors.xml` touched by `3` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0007-graphics-center-color-polish.patch`, `0009-mainline-graphics-center-dark-selector-polish.patch`
- `app/src/main/java/com/winlator/cmod/AdrenotoolsFragment.java` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0009-mainline-graphics-center-dark-selector-polish.patch`
- `app/src/main/java/com/winlator/cmod/MainActivity.java` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0005-mainline-ui-forensic-nav-hotfix.patch`
- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/src/main/java/com/winlator/cmod/contentdialog/DXVKConfigDialog.java` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/src/main/java/com/winlator/cmod/contentdialog/DgVoodooConfigDialog.java` touched by `2` patches
  - `0002-mainline-restore-forensic-runtime-core.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/src/main/java/com/winlator/cmod/contents/ContentProfile.java` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/src/main/java/com/winlator/cmod/contents/DgVoodooManager.java` touched by `2` patches
  - `0003-mainline-add-missing-runtime-bridge-classes.patch`, `0004-mainline-dgvoodoo-wcp-dev64-bridge.patch`
- `app/src/main/java/com/winlator/cmod/core/ForensicConfig.java` touched by `2` patches
  - `0002-mainline-restore-forensic-runtime-core.patch`, `0008-forensic-capture-command-compat.patch`
- `app/src/main/res/layout/content_list_item.xml` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0007-graphics-center-color-polish.patch`
- `app/src/main/res/layout/dxvk_config_dialog.xml` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/src/main/res/values/strings.xml` touched by `2` patches
  - `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `app/build.gradle` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`

## Risk Buckets

- `critical`: none
- `high`: none
- `medium` (2 files):
  - `app/src/main/java/com/winlator/cmod/ContentsFragment.java` (3 patches)
  - `app/src/main/res/values/colors.xml` (3 patches)

## Key Runtime Integration Coverage

- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` -> touched: `yes` (2 patches)
- `app/src/main/java/com/winlator/cmod/xenvironment/components/GuestProgramLauncherComponent.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/container/Container.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/ContainerDetailFragment.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/ContentsFragment.java` -> touched: `yes` (3 patches)
- `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java` -> touched: `yes` (2 patches)
- `app/src/main/java/com/winlator/cmod/AdrenotoolsFragment.java` -> touched: `yes` (2 patches)
- `app/src/main/java/com/winlator/cmod/contents/AdrenotoolsManager.java` -> touched: `yes` (1 patches)

## Action Rules

- Keep runtime launch flow changes in smallest possible follow-up patches.
- For files in `critical` bucket, run `ci/winlator/check-patch-stack.sh` before push.
- Any new patch touching `XServerDisplayActivity` or `GuestProgramLauncherComponent` must include forensic markers and fallback reason codes.

