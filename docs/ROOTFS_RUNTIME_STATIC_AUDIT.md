# RootFS Runtime Static Audit

Date: 2026-03-15

## Scope

This audit is a static, pre-compile inventory of the donor-rootfs-first batch.
It answers four questions:

- does the build still carry legacy `imagefs`
- is the app actually split between `glibc` and `bionic`
- will `Contents` place/install runtime packages into the expected paths
- is there enough wiring to claim `glibc Wine` and `bionic native` both work

## Findings

### Finding 1: `glibc` / `bionic` rootfs is now enforced per container launch

Status: closed

Facts:

- `ImageFsInstaller` now exposes `isInstallRequired(context, container,
  requestedRuntimeModel)` and the same explicit-model overload for
  `installIfNeededFuture(...)`.
- `XServerDisplayActivity` now resolves the requested runtime model before
  `WineInfo`/launcher creation, blocks launch on `ensureLaunchRootfsReady(...)`,
  and restarts only after variant-correct rootfs preparation succeeds.
- `GuestProgramLauncherFactory.create(...)` now takes an explicit requested
  runtime model and prefers that over ambient rootfs state.

Impact:

- container launch no longer relies on stale `imagefs` state alone
- selected runtime package metadata now participates in choosing both rootfs
  install path and launcher model

Conclusion:

The repo remains a single active `files/imagefs` rootfs model, but it is now a
runtime-model-enforced one. Launch-time drift between selected runtime package,
rootfs variant, and launcher model is closed.

## Finding 2: legacy `imagefs` is removed from the packaged mainline path

Status: closed

Facts:

- `ImageFsInstaller` no longer falls back to legacy `imagefs` from the live
  install path; donor `imagefs_bionic.txz` / `imagefs_gamenative.txz` are now
  the only accepted base archives.
- local `imagefs.txz` and `imagefs.txz.02` have been moved out of
  `app/src/main/assets` into `.legacy_rootfs/`, so they are no longer part of
  the packaged APK asset tree.
- `ImageFs` now normalizes old `.provider` / `.layout` values to
  `gamenative` / `ubuntufs`, and `XServerDisplayActivity` writes donor rootfs
  markers unconditionally instead of branching back to `legacy`.
- `downloadImageFS` still strips old asset names if they are reintroduced, so
  the build path now has both source-tree removal and asset-prep protection.

Impact:

- packaged mainline rootfs is donor-only
- old local archives are now archived residue, not live APK input
- stale container metadata can no longer silently flip live launch code back
  into a legacy rootfs marker path

Conclusion:

Legacy `imagefs` is now out of the packaged mainline path both logically and
physically. Archived local copies may still exist for reverse-reference, but
they are no longer part of live APK assets or live installer fallback logic.

## Finding 3: `Contents` runtime resolution is now `glibc` / `bionic` aware

Status: closed

Facts:

- `ContentProfile` now carries an explicit `runtimeModel` field plus inference
  helpers for donor feeds and legacy entries.
- `ContentsManager` now parses, persists, writes, and resolves
  `runtimeModel` across remote feeds, local profiles, synthetic profiles, and
  installed runtime roots.
- runtime candidate scoring now includes exact runtime-model match.
- runtime resolution no longer silently cross-falls between `Wine` and
  `Proton` when the requested family is missing.

Impact:

- `Wine` and `Proton` no longer collapse into each other during runtime
  resolution
- matching `verName` / arch pairs across libc models can now be disambiguated

Conclusion:

Current `Contents` runtime resolution is family-aware, arch-aware, and
runtime-model-aware.

## Finding 4: current `Contents` metadata now declares runtime model

Status: closed

Facts:

- local `contents/contents.json` now exposes explicit `runtimeModel` for the
  stable `Wine` lane.
- `GamehubFeedNormalizer` now emits `runtimeModel` for donor `Wine` /
  `Proton` release feeds and raw component feeds.
- parser logic now consumes and persists the dedicated field.

Impact:

- runtime/rootfs routing no longer depends only on version-name folklore
- donor feed integration has an explicit libc-model contract surface

Conclusion:

The metadata plane now provides explicit runtime-model ownership for
`Wine/Proton`.

## Finding 5: shared `/opt` runtime placement is now collision-aware

Status: closed

Facts:

- `Wine` and `Proton` now install into shared `imagefs/opt`
- runtime root resolution now normalizes shared roots, `lib/wine`, and
  `prefixPack.*`
- runtime install roots are now canonicalized as
  `runtime-<model>-<family>-<version>-<verCode>`
- legacy and old-name installs are migrated into the canonical root during
  `repairInstalledRuntimeOverlays()`

Impact:

- shared `/opt` keeps one common runtime surface without type/runtime-model
  collisions

Conclusion:

The shared `/opt` model is now type- and runtime-model-qualified, so the
previous collision risk is closed.

## What is already solid

- installer variant selection does distinguish donor `imagefs_gamenative.txz`
  vs donor `imagefs_bionic.txz`
- glibc support prefetches `imagefs_patches_gamenative.tzst`
- provider/layout markers are written on install
- `Contents` runtime migration from legacy `files/contents/Wine` and
  `files/contents/Proton` into shared `imagefs/opt` is now wired
- launcher env vars for `glibc` and `bionic` are clearly different once the
  correct launcher is chosen
- `Vulkan SDK` selection is now version-group-aware instead of “best profile
  per arch even if versions drift”
- `dgVoodoo` dependency validation is now architecture-aware instead of “any
  package exists”
- synthetic-profile fallback now covers `Vulkan SDK` and `dgVoodoo` payloads,
  so remote package installs are less dependent on bundled `profile.json`

## Finding 6: payload families outside `Wine/Proton` now participate in the new rootfs contract

Status: closed at the static-contract level

Facts:

- `Vulkan SDK` payload selection now chooses one coherent installed version
  group and records layout/arch coverage instead of mixing unrelated SDK lanes.
- `ContentsManager` can now synthesize install profiles for `Vulkan SDK` and
  `dgVoodoo` payloads from extracted package structure when feed metadata is
  present but local `profile.json` is missing.
- `DgVoodooManager` now resolves canonical install destinations from
  `Contents` metadata and validates installed architecture coverage against the
  requested stage arch.

Impact:

- payload families beyond `Wine/Proton` are now less likely to drift between
  feed metadata, install roots, and launch-time consumers
- the remaining risk for `Vulkan SDK` / `dgVoodoo` is now runtime proof, not a
  missing static contract

Conclusion:

`Vulkan SDK` and `dgVoodoo` are now wired into the donor-rootfs-first model as
first-class payload lanes rather than side contracts.

## Honest answer

- Is the build still tied to old `imagefs`:
  No in the live APK path. Old archives now live only in `.legacy_rootfs/` as
  archived residue, outside `app/src/main/assets`.
- Is the app already correctly split between `glibc` and `bionic`:
  Functionally, yes at the static-contract level. Install-time, launch-time,
  resolver-time, and launcher-time all now share the same runtime-model lane.
- Will all `Contents` packages install correctly:
  `Wine` / `Proton` placement is aligned with the new rootfs layout, and
  `Vulkan SDK` / `dgVoodoo` now have static install/stage contracts too.
  Runtime proof is still required later, but the previous static holes are no
  longer open.
- Will `glibc Wine` and `bionic native` both work:
  Static code now guarantees the intended pairing logic. Runtime proof is still
  deferred until compile/testing is reopened.

## Remaining pre-compile observations

1. `wine_entries`, `glibc_wine_entries`, and `bionic_wine_entries` remain
   intentionally empty under the current Contents-first lane. This is not a
   runtime-model blocker anymore, but it does mean embedded-runtime extraction
   is not the active path.
2. donor rootfs archives are still staged at build time rather than committed
   into the tree, so the first honest compile remains the real proof point for
   asset-prep and packaging behavior.
