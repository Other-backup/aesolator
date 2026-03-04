# Winlator Patch Stack Reflective Audit

Generated: `2026-03-01T18:17:49Z`

## Snapshot

- Patch count: `1`
- Unique touched source files: `311`
- Diff volume across stack: `+18887 / -4314`
- Mean files touched per patch: `311.00`

## Numbering Contract

- Numbering contract is clean (`NNNN-` unique prefixes).

## High-Overlap Hotspots

- `app/build.gradle` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/AndroidManifest.xml` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/assets/box64_env_vars.json` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/assets/fexcore_env_vars.json` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/assets/wowbox64_env_vars.json` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/cpp/winlator/vulkan.c` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/cpp/xr/main.c` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/AdrenotoolsFragment.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/AeSolatorApplication.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/ContainerDetailFragment.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/ContainersFragment.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/ContentsFragment.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/DiagnosticsFragment.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/MainActivity.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`
- `app/src/main/java/com/winlator/cmod/SettingsFragment.java` touched by `1` patches
  - `0001-mainline-full-stack-consolidated.patch`

## Risk Buckets

- `critical`: none
- `high`: none
- `medium`: none

## Key Runtime Integration Coverage

- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/xenvironment/components/GuestProgramLauncherComponent.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/container/Container.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/ContainerDetailFragment.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/ContentsFragment.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/contents/ContentsManager.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/AdrenotoolsFragment.java` -> touched: `yes` (1 patches)
- `app/src/main/java/com/winlator/cmod/contents/AdrenotoolsManager.java` -> touched: `yes` (1 patches)

## Action Rules

- Keep runtime launch flow changes in smallest possible follow-up patches.
- For files in `critical` bucket, run `ci/winlator/check-patch-stack.sh` before push.
- Any new patch touching `XServerDisplayActivity` or `GuestProgramLauncherComponent` must include forensic markers and fallback reason codes.

