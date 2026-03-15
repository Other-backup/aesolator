# ImageFS Hybrid Plan

Updated: `2026-03-15`

## Goal

Replace the current stale `Ae.solator` `imagefs` upgrade story with a
deliberate hybrid rootfs plan:

- keep the parts of our current `imagefs` that are already contract-correct for
  `Contents`, forensics, and runtime overlays
- borrow the freshest usable base/runtime pieces from `GameNative`
- produce one `Ae.solator` rootfs that is variant-aware, traceable, and easier
  to evolve than the current monolithic `imagefs.txz`

## Donor Facts From `GameNative`

Observed from `GameNative` source:

- `:ubuntufs` is a dynamic feature delivery shell, not the actual rootfs
  payload owner
- the real rootfs install path is `com/winlator/xenvironment/ImageFsInstaller`
- donor installer currently tracks `LATEST_VERSION = 26`
- donor chooses between two archives:
  - `imagefs_gamenative.txz` for `glibc`
  - `imagefs_bionic.txz` for `bionic`
- donor deploys extra overlays after base extraction:
  - `redirect.tzst`
  - `extras.tzst`
- donor preserves imported `Wine` / `Proton` installs under `opt/`
- donor preserves `home/`
- donor strings explicitly describe the base as
  `Ubuntu RootFs - releases.ubuntu.com/focal`

## Delta Against Current `Ae.solator`

Current local baseline:

- `ImageFsInstaller.LATEST_VERSION = 21`
- single archive path: `imagefs.txz`
- bundled `Wine` and bundled graphics-driver extraction after base rootfs
- no variant-specific base archive split
- less explicit separation between:
  base rootfs, post-extract overlays, and imported runtime preservation

## Why A Blind Replacement Is Wrong

Blindly swapping our `imagefs.txz` for donor `imagefs_gamenative.txz` /
`imagefs_bionic.txz` would break ownership boundaries:

- `Ae.solator` already has its own `Contents` contract
- our runtime metadata and forensics are richer than the donor's
- our package placement for `Wine` / `Proton` / `DXVK` / `VKD3D` /
  `Vulkan SDK` / `dgVoodoo` is tied to local install semantics
- donor storefront/game-launch assumptions should not become hidden base-rootfs
  policy

## Hybrid Build Strategy

Build a new `Ae.solator imagefs` in layers:

1. donor/base layer
   - inspect donor `imagefs_gamenative.txz` and `imagefs_bionic.txz`
   - inventory newer system libraries, toolchain pieces, loader paths,
     multimedia stack, wrapper hooks, and compatibility shims
2. local/contract layer
   - preserve `Ae.solator` ownership for:
     `Contents`, forensic hooks, runtime metadata, container bootstrap, driver
     package routing, and package visibility rules
3. overlay layer
   - compare donor `redirect.tzst` / `extras.tzst` against our own boot-time
     overlays and classify each file as:
     `adopt`, `adapt`, `replace locally`, or `reject`
4. runtime preservation layer
   - keep donor-style preservation of imported `Wine` / `Proton` installs in
     `opt/`, but adapt it to our `Contents`-managed runtime lanes

## Required Reverse-Engineering Passes

Before building the hybrid rootfs:

- unpack donor `imagefs_gamenative.txz`
- unpack donor `imagefs_bionic.txz`
- diff them against our current `imagefs.txz`
- classify every delta by subsystem:
  libc/glibc, Vulkan loader, OpenGL stack, multimedia, input/gamepad,
  font/config, Wine helper tools, wrapper hooks, and runtime launch helpers
- map donor overlay archives:
  `redirect.tzst`, `extras.tzst`

## Output We Actually Want

Not:

- "use donor UbuntuFS"

But:

- one `Ae.solator` hybrid rootfs
- variant-aware (`bionic`, `glibc`, future runtime lanes if needed)
- newer donor libraries where they are strictly better
- our own runtime/package/forensic contracts preserved
- documented ownership for every nontrivial library or overlay we keep

## Next Concrete Steps

1. extract and inventory donor `imagefs_gamenative.txz`
2. extract and inventory donor `imagefs_bionic.txz`
3. diff both against current `Ae.solator imagefs.txz`
4. produce a per-library adoption table
5. decide whether the hybrid rootfs is:
   - one unified archive with internal variant dirs, or
   - two variant archives with a shared overlay layer
