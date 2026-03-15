# ImageFS Hybrid Plan

Updated: `2026-03-15`

## Goal

Replace the current stale `Ae.solator` `imagefs` upgrade story with a
donor-rootfs-first hybrid plan:

- take `GameNative` rootfs archives as the canonical base for both variants
- keep local ownership for `Contents`, forensics, runtime overlays, and package
  install logic
- produce one `Ae.solator` rootfs story that is variant-aware, traceable, and
  easier to evolve than the old monolithic local `imagefs.txz`

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
- donor download service also exposes:
  - `imagefs_patches_gamenative.tzst`
- donor preserves imported `Wine` / `Proton` installs under `opt/`
- donor preserves `home/`
- donor strings explicitly describe the base as
  `Ubuntu RootFs - releases.ubuntu.com/focal`
- donor `SteamService.fetchFileWithFallback()` resolves rootfs artifacts from:
  - primary: `https://downloads.gamenative.app/<fileName>`
  - fallback: `https://pub-9fcd5294bd0d4b85a9d73615bf98f3b5.r2.dev/<fileName>`

Observed live on `2026-03-15` from the primary host:

- `imagefs_gamenative.txz`
  - `content-length: 166,439,388`
  - `last-modified: 2025-09-17`
- `imagefs_bionic.txz`
  - `content-length: 183,506,500`
  - `last-modified: 2025-10-12`
- `imagefs_patches_gamenative.tzst`
  - `content-length: 187,234,448`
  - `last-modified: 2025-10-29`
- `extras.tzst`
  - `content-length: 84,503,365`
  - `last-modified: 2025-12-10`

Observed from streamed archive inventory on `2026-03-15`:

- donor `imagefs_gamenative.txz` is not a thin base rootfs; it carries a large
  runtime/tooling surface including `opt/wine`, `opt/apps`, `opt/system32`,
  `Steamless`, `7-Zip`, and `generate_interfaces_file.exe`
- donor `imagefs_gamenative.txz` also carries foreign build-host metadata such
  as `.DS_Store` / `._*`, which should be treated as archive noise rather than
  product payload
- donor `imagefs_bionic.txz` is now the canonical `bionic` base reference
- donor `imagefs_gamenative.txz` is now the canonical `glibc` base reference
- the hybrid split now happens above those base archives:
  overlays, runtime payloads, compatibility bridges, and local package logic

## Delta Against Current `Ae.solator`

Current local baseline:

- `ImageFsInstaller.LATEST_VERSION = 21`
- single archive path: `imagefs.txz`
- bundled `Wine` and bundled graphics-driver extraction after base rootfs
- no variant-specific base archive split
- less explicit separation between:
  base rootfs, post-extract overlays, and imported runtime preservation

Local transfer status as of `2026-03-15`:

- `Ae.solator` now has the donor-style installer foundation in-tree:
  variant-aware archive selection, overlay deployment hooks, preserved imported
  runtimes in `opt/`, and persisted `containerVariant` / `imagefs` markers
- local `ImageFsInstaller` now also mirrors donor-style remote delivery for the
  base rootfs lane:
  `downloads.gamenative.app/<archive>` with R2 fallback when the variant
  archive is not bundled locally
- glibc rootfs support now explicitly includes staged handling for
  `imagefs_patches_gamenative.tzst` instead of pretending the base archive is
  the whole donor contract
- local main-runtime compatibility now bridges both layouts:
  donor-style `/opt/wine` and legacy/local `/opt/<main-runtime-id>`
- donor `container_pattern_gamenative.tzst` and
  `pulseaudio-gamenative.tzst` are now staged locally so glibc/main-runtime
  containers can use the donor pattern/audio overlays instead of falling back
  to the older generic path
- donor/local layer ownership is now explicit in
  [IMAGEFS_LAYER_OWNERSHIP_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md),
  so the hybrid lane no longer treats every archive as a candidate base swap
- per-library adoption is now explicit too in
  [IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md),
  including donor-base decisions for Vulkan/OpenAL/Pulse/sysvshm and donor
  overlay decisions for XAudio/XACT/utility payloads
- local container fallback now accepts both `prefixPack.tzst` and
  `prefixPack.txz`, matching donor custom-runtime intake more closely
- local runtime-package placement no longer assumes install dir == runtime root:
  `Wine` / `Proton` now resolve the effective runtime root from the shared
  parent of `wineBinPath` / `wineLibPath` / `winePrefixPack`, and installed
  runtimes are post-processed to normalize `lib/wine` plus executable bits
- local `ImageFsInstaller` now also carries a donor-derived
  `generateCompactContainerPattern()` helper adapted to the bridged main runtime
  path, so donor `container_pattern_gamenative` can be re-derived without
  assuming only one historical layout
- donor overlays `redirect.tzst` and `extras.tzst` are now staged in local
  assets for later diffing and controlled deployment
- local `imagefs.txz.02` is now explicitly classified as orphan baggage:
  it is unreferenced in source and fails `xz -t`, so it is not a valid live
  shard in the current install contract
- local build/runtime scaffolding now assumes donor-rootfs-first:
  `ubuntufs` module scaffold is in tree, `:app` now declares the dynamic
  feature, and rootfs download tasks now target donor `GameNative` archives

## Archive Diff Snapshot

Streamed archive inspection now supports a more concrete split strategy:

- local `imagefs.txz` and donor `imagefs_bionic.txz` both expose a broad
  userland surface:
  `curl`, `openssl`, `xmllint`, `ffmpeg`, `winetricksfolder`, large
  `gstreamer-1.0` plugin trees, PulseAudio, DBus, fontconfig/fonts, Vulkan, and
  OpenAL data
- donor `imagefs_gamenative.txz` clearly behaves more like a runtime-specific
  `opt/` layer:
  `./opt/wine/...` plus macOS packaging noise (`._*`, `.DS_Store`, xattrs)
- donor `imagefs_patches_gamenative.tzst` owns the extra utility/runtime
  overlay surface:
  `./opt/system32` XAudio/XACT DLLs, `./opt/apps/TestD3D.exe`,
  `./opt/apps/GPUInfo.exe`, bundled `7-Zip`, `Steamless`, and
  `generate_interfaces_file.exe`
- local `imagefs.txz` does not currently expose that donor `opt/system32` /
  `opt/apps` toolchain layer; among the queried `opt/*` extras it only showed
  `opt/winetricks`

Implication:

- treat donor `bionic` as a compatibility reference for userland freshness, not
  as proof that the whole local base rootfs should be replaced
- treat donor `glibc` plus `imagefs_patches_gamenative.tzst` as the utility and
  runtime-tool overlay lane that can be adopted surgically after metadata
  cleanup and ownership mapping
- treat donor `extras.tzst` as the utility overlay lane:
  `Steamless`, `7-Zip`, `GPUInfo.exe`, `TestD3D.exe`,
  `generate_interfaces_file.exe`, and `wine-mono`
- treat donor `redirect.tzst` as the redirect-hook lane:
  `usr/lib/libredirect.so` and `usr/lib/libredirect-bionic.so`
- both overlays still need metadata cleanup (`._*`, `.DS_Store`) before any
  future hybrid rebuild step

## Why A Blind Replacement Is Wrong

Blindly copying donor product policy on top of the base archives would still
break ownership boundaries:

- `Ae.solator` already has its own `Contents` contract
- our runtime metadata and forensics are richer than the donor's
- our package placement for `Wine` / `Proton` / `DXVK` / `VKD3D` /
  `Vulkan SDK` / `dgVoodoo` is tied to local install semantics
- donor storefront/game-launch assumptions should not become hidden base-rootfs
  policy

## Hybrid Build Strategy

Build the new `Ae.solator imagefs` in layers on top of donor base archives:

1. donor/base layer
   - use donor `imagefs_bionic.txz` and `imagefs_gamenative.txz` as canonical
     base archives
   - keep the per-library adoption map explicit for every nontrivial base
     library or tool we rely on
2. local/contract layer
   - preserve `Ae.solator` ownership for:
     `Contents`, forensic hooks, runtime metadata, container bootstrap, driver
     package routing, and package visibility rules
3. overlay layer
- compare donor `redirect.tzst` / `extras.tzst` against our own boot-time
  overlays and classify each file as:
  `adopt`, `adapt`, `replace locally`, or `reject`
- inspect whether `imagefs_patches_gamenative.tzst` is a post-base delta we
  should absorb into overlays, or whether it belongs to a future patch lane
4. runtime preservation layer
   - keep donor-style preservation of imported `Wine` / `Proton` installs in
     `opt/`, but adapt it to our `Contents`-managed runtime lanes
5. app-native helper layer
   - track helper libraries that live in the APK native dir but may also need
     guest placement, especially `libevshim.so` and `libdummyvk.so`
   - keep renderer-side APK libs such as `libvirglrenderer.so` and
     `libvortekrenderer.so` classified separately from guest `usr/lib` payloads

## Required Reverse-Engineering Passes

Before building the hybrid rootfs:

- unpack donor `imagefs_gamenative.txz`
- unpack donor `imagefs_bionic.txz`
- diff them against our current `imagefs.txz`
- classify every delta by subsystem:
  libc/glibc, Vulkan loader, OpenGL stack, multimedia, input/gamepad,
  font/config, Wine helper tools, wrapper hooks, and runtime launch helpers
- map donor overlay archives:
  `redirect.tzst`, `extras.tzst`, `imagefs_patches_gamenative.tzst`
- compare donor `container_pattern_gamenative.tzst` against the current
  container bootstrap assumptions and record which files belong to rootfs
  baseline versus per-runtime prefix content
- classify donor archive noise separately from useful payload:
  `.DS_Store`, Apple xattrs, and any other foreign host metadata should be
  explicitly excluded from any future rebuilt `Ae.solator` rootfs

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

1. keep the per-library adoption table current as donor base evolves
2. carry donor-rootfs-first package placement through runtime verification
3. decide whether the hybrid rootfs is:
   - one unified archive with internal variant dirs, or
   - two variant archives with a shared overlay layer
4. keep donor helper-lib ownership explicit:
   - APK-only:
     `libvirglrenderer.so`, `libvortekrenderer.so`
   - guest `usr/lib` mirror:
     `libevshim.so`, `libdummyvk.so`, `libredirect*.so`

See also:

- [IMAGEFS_LAYER_OWNERSHIP_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md)
- [IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md)
