# Winlator Patch Stack Notes

This directory contains the ordered patch stack applied to the upstream Winlator
source checkout by `ci/winlator/apply-repo-patches.sh`.

## Ordering rule

Patches are applied lexicographically (`0001` -> `NNNN`). Do not renumber in the
middle of the stack.

## Current baseline

The patch base is currently eight-patch mainline:

- `0001-mainline-full-stack-consolidated.patch`
  - base fork/runtime/FEX contract and Ae.solator branding
  - contents/WCPHub routing, Adrenotools source policy, and runtime profile UI
  - X11-first launch contracts, upscaler/DX policy matrix, Vulkan diagnostics, and forensic runtime tracing
  - Forensic Center control plane, issue-bundle export, and runtime signal contract helper
  - rebuilt Winlator Task Manager with realtime X11 correlation, Linux `/proc` telemetry, live FPS/GPU/renderer state, rate-based IO/network summaries, process-tree controls, socket filtering, and telemetry JSON/issue-bundle export
- `0002-mainline-restore-forensic-runtime-core.patch`
  - restores missing forensic/runtime support classes required by latest upstream
    (`ForensicConfig`, `ForensicLogger`, `LinuxTelemetrySampler`,
    `RuntimeProfile*`, `DgVoodooConfigDialog`, `VulkanVersionInfo`, and related helpers)
  - keeps compile contract stable while `0001` stays unchanged
- `0003-mainline-add-missing-runtime-bridge-classes.patch`
  - adds compile-safe bridge classes referenced by `0001` runtime/graphics integrations
    (`ContainerDiscovery`, `ContainerNormalizer`, `RuntimeSignalContract`,
    `FileDebugLogger`, `DriverProbeResult`, `DgVoodooManager`)
  - closes current upstream drift where `0001` references these classes but upstream does not ship them yet
- `0004-mainline-dgvoodoo-wcp-dev64-bridge.patch`
  - extends `DgVoodooManager` import path to accept `ZIP` and `WCP` archives (`.wcp/.wcp.xz/.wcp.zst`)
  - keeps dgVoodoo Contents lane manageable when artifact source is `dgvoodoo.wcp` from WCP Archive
  - adds runtime directory probing for `Release/arm64`, `Release/arm64ec`, and `Release/x64` layouts (`dgVoodoo2_*_dev64.zip`)
- `0005-mainline-ui-forensic-nav-hotfix.patch`
  - fixes side navigation regression by restoring deterministic drawer behavior for dialog-only items
    (`about` and `diagnostics`) and keeping checked state stable
  - makes `Forensic Center` explicitly clickable via a dedicated in-app forensic dialog with
    runtime/capture summary and one-tap copy for ADB capture/browse commands
  - replaces the zero-height transparent drawer header with a compact branded header to prevent
    sidebar title drift
- `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
  - adds `DgVoodoo` to Contents overlay parser/category routing so remote rows are no longer dropped
  - restores architecture split visibility (`x86_64`/`arm64ec`) for dgVoodoo in Contents filters
  - removes legacy DDraw selector from DXVK config and fixes DXVK runtime path to keep DDraw on system/WineD3D
  - extends dgVoodoo config dialog with arch/route toggles and explicit legacy API routing hints
- `0007-graphics-center-color-polish.patch`
  - improves Graphics Center visual polish by adding per-lane accent colors and a category badge in Contents rows
  - fixes icon rendering path (`setImageResource` + tint) to avoid inconsistent list item paint in light/dark themes
- `0008-forensic-capture-command-compat.patch`
  - restores `ForensicConfig.buildCaptureCommand(Context, Snapshot)` compatibility for MainActivity forensic dialog
  - maps legacy call path to current issue-capture command builder to keep forensic UX and CI compile stable

Historical review slices `0002..0029` were folded back into `0001` on March 1,
2026 after apply/build verification. Current `0002`, `0003`, and `0004` are bounded restore
slices for upstream drift and can be folded back into `0001` after the next stable cycle.

## Patch-base rule

- Mainline stays consolidated by default (`0001`).
- New work can land as `0002+` slices when isolated review/debug windows are
  needed.
- Once a slice is stable, fold it back into
  `0001-mainline-full-stack-consolidated.patch` and restore the one-patch
  baseline.

## Phase map

`ci/winlator/patch-batch-plan.tsv` maps phases to active patch windows (`0001`
and optional `0002+` slices) so batch tooling stays deterministic during patch
base expansion.

## Known high-overlap files (intentional)

These files appear in multiple patches and require extra review when adding new
patches to avoid accidental regressions:

- `XServerDisplayActivity.java` (runtime integration point)
- `GuestProgramLauncherComponent.java` (launch submit/final env normalization)
- `Container.java` / `ContainerDetailFragment.java` (schema + UI persistence)
- `ContentsFragment.java` / `ContentsManager.java` (source routing and display policy)
- `AdrenotoolsFragment.java` / `AdrenotoolsManager.java` (driver browser and install flow)

## Audit workflow

Before pushing a new patch touching high-overlap files:

```bash
bash ci/winlator/validate-patch-sequence.sh
bash ci/winlator/run-reflective-audits.sh
bash ci/winlator/check-patch-stack.sh /path/to/upstream/winlator/checkout
```

This creates a clean temporary clone, applies the full stack, and reports file
overlaps touched by multiple patches.

For Winlator CI mainline, keep `WINLATOR_PATCH_PREFLIGHT=1` so the same
apply-check runs before Gradle. This turns patch drift into an early patch-stack
failure instead of a late APK build failure.

`ci/winlator/apply-repo-patches.sh` also contains a narrow reject-heal path for
the contents-branding block inside `0001-mainline-full-stack-consolidated.patch`.
It only applies when upstream drifts in
`app/src/main/res/values/strings.xml`; do not generalize this pattern without
adding an explicit bounded self-heal condition.

## Fast local patch-base flow

When the goal is to keep moving through the patch base instead of running the
full audit loop every time, use the lighter batch runner:

```bash
bash ci/winlator/check-patch-batches.sh /path/to/upstream/winlator/checkout
```

Useful modes:

- `WINLATOR_PATCH_BATCH_SIZE=5` - apply in blocks of 5 patches (default)
- `WINLATOR_PATCH_BATCH_SIZE=7` - apply in blocks of 7 patches
- `WINLATOR_PATCH_BATCH_MODE=single` - apply strictly one patch at a time
- `WINLATOR_PATCH_BATCH_PROFILE=standard|wide|single` - convenience aliases for 5, 7 or 1 patch windows
- `WINLATOR_PATCH_BATCH_FIRST=1 WINLATOR_PATCH_BATCH_LAST=1` - focus only the
  current consolidated mainline window

`ci/winlator/apply-repo-patches.sh` supports the same selective window via
`WINLATOR_PATCH_FROM=NNNN` and `WINLATOR_PATCH_TO=NNNN`.

The heavier full audit can also be scoped to a contiguous window:

```bash
WINLATOR_PATCH_FROM=0001 WINLATOR_PATCH_TO=0001 \
  bash ci/winlator/check-patch-stack.sh /path/to/upstream/winlator/checkout

WINLATOR_PATCH_PHASE=runtime_policy \
  bash ci/winlator/check-patch-stack.sh /path/to/upstream/winlator/checkout
```

For multi-window patch-base work, use the phase runner:

```bash
bash ci/winlator/run-patch-base-cycle.sh /path/to/upstream/winlator/checkout
WINLATOR_PATCH_BASE_PROFILE=wide bash ci/winlator/run-patch-base-cycle.sh /path/to/upstream/winlator/checkout
WINLATOR_PATCH_BASE_PHASE=runtime_policy bash ci/winlator/run-patch-base-cycle.sh /path/to/upstream/winlator/checkout
```

To inspect the exact local 5/7/1 windows before running anything:

```bash
bash ci/winlator/list-patch-batches.sh
WINLATOR_PATCH_BATCH_PROFILE=wide bash ci/winlator/list-patch-batches.sh
WINLATOR_PATCH_BATCH_FIRST=1 WINLATOR_PATCH_BATCH_LAST=1 bash ci/winlator/list-patch-batches.sh
WINLATOR_PATCH_BATCH_PHASE=runtime_policy bash ci/winlator/list-patch-batches.sh
```

To inspect the named phases themselves:

```bash
bash ci/winlator/list-patch-phases.sh
bash ci/winlator/resolve-patch-phase.sh runtime_policy
```

To reserve the next patch number safely:

```bash
bash ci/winlator/next-patch-number.sh
bash ci/winlator/next-patch-number.sh ci/winlator/patches my-new-patch-slug
```

To create a temporary `0002+` slice patch from a modified Winlator source tree
relative to the current consolidated base:

```bash
bash ci/winlator/create-slice-patch.sh /path/to/upstream/winlator/checkout my-slice
```

To step through the next local batch window deterministically:

```bash
bash ci/winlator/next-patch-batch.sh
WINLATOR_PATCH_BATCH_CURSOR=1 bash ci/winlator/next-patch-batch.sh
WINLATOR_PATCH_BATCH_PHASE=runtime_policy bash ci/winlator/next-patch-batch.sh
```

## Ae.solator asset overlay import

To import a `res/*` overlay zip (safezone/allskins pack), generate a slice, and
fold it back into consolidated mainline:

```bash
bash ci/winlator/import-aesolator-assets.sh \
  "/path/to/res_custom_AE_SOLATOR_mipmap_safezone_allskins.zip" \
  "/path/to/winlator-src-git"
```

Useful modes:

- `WINLATOR_ASSET_IMPORT_MODE=slice` - keep a temporary `0002-...` review slice
- `WINLATOR_ASSET_IMPORT_MODE=fold` - default, folds back into `0001` baseline
- `WINLATOR_ASSET_VALIDATE_ONLY=1` - inspect zip metadata without mutating source

## Folding slices back into mainline

When temporary slice patches (`0002+`) are done, fold them back into the
single-patch base:

```bash
bash ci/winlator/fold-into-mainline.sh /path/to/upstream/winlator/checkout
```

By default it drops folded `0002+` patch files and keeps only
`0001-mainline-full-stack-consolidated.patch`.
