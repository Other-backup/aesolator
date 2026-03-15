# ImageFS Reverse Map

Updated: `2026-03-15`

## Scope

Runtime-oriented reverse map of the shipped `imagefs` rootfs as observed from:

- source code in `ImageFs.java`, `ImageFsInstaller.java`, and
  `XServerDisplayActivity.applyGeneralPatches()`
- a live installed build on device `10.0.0.1:42363`

This document separates:

- base rootfs facts shipped by `imagefs.txz`
- installer behavior
- runtime overlays and mutations that happen after base extraction

The explicit layer classification now lives in:

- [IMAGEFS_LAYER_OWNERSHIP_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md)

## Base Install Contract

Current local contract after the donor-rootfs foundation pass:

- `ImageFsInstaller.LATEST_VERSION = 26`
- installer is now variant-aware at the foundation layer:
  `imagefs_gamenative.txz` for `glibc`,
  `imagefs_bionic.txz` for `bionic`,
  and donor rootfs is now the intended mainline base
- when a donor variant archive is not bundled locally, installer code now
  attempts donor-style remote delivery:
  primary `downloads.gamenative.app`, then donor R2 fallback
- local main-runtime compatibility now bridges both layouts:
  donor-style `/opt/wine` and legacy/local `/opt/<main-runtime-id>`
- installer writes `.winlator/.img_version`
- launch/runtime flow now also writes `.winlator/.variant` and `.winlator/.arch`
- `clearRootDir()` preserves `home/` across reinstalls
- `clearOptDir()` now preserves imported `Wine` / `Proton` payloads in `opt/`
  more like the donor contract instead of clearing every runtime payload
- donor overlay archives are now part of the local installer surface:
  `redirect.tzst`, `extras.tzst`
- glibc support now also tracks donor `imagefs_patches_gamenative.tzst` as a
  distinct support artifact instead of pretending the base archive is enough
- local runtime/profile/container helpers now also accept both
  `prefixPack.tzst` and `prefixPack.txz`
- local runtime/profile/container helpers now also derive the effective custom
  runtime root from the shared parent of `wineBinPath` / `wineLibPath` /
  `winePrefixPack`, instead of assuming every imported runtime is rooted at the
  install directory itself
- local `Wine` / `Proton` install roots are now donor-rootfs aware too:
  they belong under `imagefs/opt`, with legacy `files/contents/Wine` /
  `files/contents/Proton` treated as migration sources rather than the
  canonical runtime surface
- local rootfs contract now carries explicit provider/layout markers:
  `.winlator/.provider` and `.winlator/.layout`
- donor-rootfs compatibility bridges are now part of the install contract too:
  canonical `/tmp`, compatibility `/usr/tmp`, canonical `usr/local/bin/box64`,
  compatibility `usr/bin/box64`

Archive-diff snapshot from streamed inspection:

- local `imagefs.txz` and donor `imagefs_bionic.txz` both expose the broad
  userland surface we care about most:
  GStreamer plugins, PulseAudio, DBus, fonts/fontconfig, Vulkan, OpenAL,
  `curl`, `openssl`, `xmllint`, `ffmpeg`, and `winetricksfolder`
- donor `imagefs_gamenative.txz` is visibly more `opt/`-centric:
  it carries `./opt/wine/...` and also obvious macOS packaging noise
  (`._*`, `.DS_Store`, xattrs)
- donor `imagefs_patches_gamenative.tzst` owns the extra tool/runtime overlay:
  `./opt/system32` XAudio/XACT DLLs, `./opt/apps`, bundled `7-Zip`,
  `Steamless`, and `generate_interfaces_file.exe`
- local `imagefs.txz` did not expose those donor `opt/system32` /
  `opt/apps` extras in the same query; it only exposed `opt/winetricks`
- donor staged overlays are now classified one step deeper too:
  `extras.tzst` is the utility overlay (`Steamless`, `7-Zip`, `GPUInfo.exe`,
  `TestD3D.exe`, `generate_interfaces_file.exe`, `wine-mono` payload), while
  `redirect.tzst` is the redirect-hook layer
  (`usr/lib/libredirect.so`, `usr/lib/libredirect-bionic.so`)
- both staged donor overlays still carry macOS packaging noise (`._*`,
  `.DS_Store`), so any future rebuild path needs a cleanup filter, not a blind
  repack
- local `imagefs.txz.02` is currently not part of the live contract:
  it is unreferenced in source, fails `xz -t`, and looks like orphan baggage
  rather than a valid split-archive shard

## Top-Level Layout

Observed under `/data/user/0/com.winlator.cmod/files/imagefs`:

- `bin`
- `etc`
- `home`
- `lib`
- `opt`
- `share`
- `storage`
- `tmp`
- `usr`
- `var`
- `.winlator/.img_version`

Interpretation:

- `usr/` is the main runtime payload surface
- `home/` is intentionally preserved because user/container state evolves there
- `.winlator/.img_version` is the image validity/version marker
- main runtime ownership is now explicitly two-layout compatible:
  donor `/opt/wine` and local `/opt/<main-runtime-id>`

## User-State Paths

From `ImageFs.java`:

- runtime user: `xuser`
- home: `/home/xuser`
- cache: `/home/xuser/.cache`
- config: `/home/xuser/.config`
- wineprefix: `/home/xuser/.wine`

Important observation:

- base `imagefs/home` is not the final user state; container lifecycle and
  runtime boot paths populate `xuser` and the effective prefix later

## Core Runtime Directories

Observed under `imagefs/usr`:

- `bin`
- `etc`
- `lib`
- `libexec`
- `sbin`
- `share`
- `tmp`
- `var`

Important code-level accessors:

- `getTmpDir()` -> canonical `/tmp`
- `getCompatTmpDir()` -> compatibility `/usr/tmp`
- `getLibDir()` -> `/usr/lib`
- `getBinDir()` -> `/usr/bin`
- `getShareDir()` -> `/usr/share`
- `getEtcDir()` -> `/usr/etc`

## Notable Binaries

Observed under `imagefs/usr/bin`:

- `curl`
- `tar`
- `grep`
- `openssl`
- `zstd`
- `ffmpeg`
- `xmllint`
- `gio`
- `gst-device-monitor-1.0`
- `gst-discoverer-1.0`
- `gst-inspect-1.0`
- `gst-launch-1.0`
- `gst-play-1.0`
- `gst-stats-1.0`
- `gst-transcoder-1.0`
- `gst-typefind-1.0`
- `winetricksfolder`

Interpretation:

- the base rootfs already ships network, archive, XML, multimedia, and
  GStreamer tooling
- `winetricksfolder` confirms the rootfs is prepared for Wine-adjacent helper
  workflows, not only a minimal process sandbox

## Notable Libraries

Observed under `imagefs/usr/lib`:

- X11/XCB stack:
  `libX11*`, `libxcb-*`
- Android compatibility shims:
  `libandroid-*`
- audio stack:
  `libpulse.so`, `libpulseaudio.so`, `libpulsecommon-13.0.so`,
  `libpulsecore-13.0.so`
- Vulkan loader:
  `libvulkan.so`, `libvulkan.so.1`, `libvulkan.so.1.4.315`
- multimedia:
  `libavformat*`
- GStreamer:
  `libgstreamer-1.0*`, `libgst*`
- Winlator-specific hook:
  `libfile_redirect_hook.so`

Interpretation:

- this is a broad userland runtime, not a thin Wine-only package
- `libfile_redirect_hook.so` is a Winlator-specific integration point worth
  treating as contract-sensitive

## Config and Shared Data

Observed under `imagefs/usr/etc`:

- `alsa`
- `dbus-1`
- `fonts`
- `hosts`
- `host.conf`
- `pulse`
- `resolv.conf`
- `rpc`
- `tls`
- `unbound`
- `xattr.conf`
- `xdg`

Observed under `imagefs/usr/share/wine`:

- `fonts`
- `nls`

Interpretation:

- network, font, pulse, dbus, and xdg assumptions are already baked into the
  base image
- Wine language/font payload is partly base-image owned, not entirely
  container-owned

## Opt Surface

From code:

- `getInstalledWineDir()` -> `/opt/installed-wine`
- `ImageFs.winePath` now resolves to donor `/opt/wine` when present,
  otherwise to local `/opt/<main-wine-version>`
- `installWineFromAssets()` extracts each bundled Wine runtime into `opt/<ver>`

Interpretation:

- `opt/` is the Wine runtime ownership surface
- app-side Wine updates should respect `installed-wine` preservation rules

## Runtime Patch Layer

At container boot, `XServerDisplayActivity.applyGeneralPatches()` now branches
by runtime variant:

- `bionic`
  - `container_pattern_common.tzst` into the rootfs root
  - `pulseaudio.tzst` into app files dir `pulseaudio`
- `glibc`
  - `imagefs_patches_gamenative.tzst` into the rootfs root when available
  - donor `pulseaudio-gamenative.tzst` into app files dir `pulseaudio`
    with fallback to `pulseaudio.tzst`

Then it applies system tweaks and clears stale container-side graphics/theme
extras.

Interpretation:

- not everything visible at runtime belongs to base `imagefs.txz`
- there is a second mutation layer applied at container/session start

## Safe vs Sensitive Change Zones

Safer app-side investigation zones:

- metadata and documentation around `imagefs` structure
- verification of shipped tools/libraries
- installer diagnostics and version tracking

Contract-sensitive zones:

- `opt/` Wine runtime ownership
- `container_pattern_common.tzst` overlay behavior
- `container_pattern_gamenative.tzst` main-runtime bootstrap behavior
- `pulseaudio.tzst` extraction path
- `pulseaudio-gamenative.tzst` extraction path
- `imagefs_patches_gamenative.tzst` glibc patch behavior
- any change touching `libfile_redirect_hook.so`
- any change assuming `home/` is disposable

## Working Conclusions

- `imagefs` is a layered runtime:
  base rootfs + bundled Wine/assets + boot-time overlay patches
- `home/` and container-user state must be treated as mutable/preserved state,
  not as part of a stateless base image
- any future optimization or artifact migration for `imagefs` should first be
  classified as:
  base image content, Wine runtime payload, or boot-time overlay content

## Donor Delta: `GameNative` / `UbuntuFS`

Additional donor findings from `GameNative`:

- donor `ImageFsInstaller.LATEST_VERSION = 26` versus our current `21`
- donor rootfs is variant-aware:
  `imagefs_gamenative.txz` for `glibc`,
  `imagefs_bionic.txz` for `bionic`
- donor deploys `redirect.tzst` and `extras.tzst` after base extraction
- donor download service also exposes `imagefs_patches_gamenative.tzst`
- donor main runtime also uses `container_pattern_gamenative.tzst`
  plus `pulseaudio-gamenative.tzst`
- donor preserves imported `Wine` / `Proton` installations under `opt/`
- donor strings identify the base as `Ubuntu RootFs - releases.ubuntu.com/focal`
- donor `:ubuntufs` module is only a delivery shell; actual rootfs ownership
  still sits in `ImageFsInstaller`
- donor artifact download contract currently resolves through
  `SteamService.fetchFileWithFallback()`:
  primary `https://downloads.gamenative.app/<fileName>`,
  fallback `https://pub-9fcd5294bd0d4b85a9d73615bf98f3b5.r2.dev/<fileName>`
- live `HEAD` checks on `2026-03-15` confirmed:
  - `imagefs_gamenative.txz` `166,439,388` bytes
  - `imagefs_bionic.txz` `183,506,500` bytes
  - `imagefs_patches_gamenative.tzst` `187,234,448` bytes
  - `extras.tzst` `84,503,365` bytes

Interpretation:

- the right next step is not a blind donor swap
- `Ae.solator` now has the donor installer/overlay foundation locally, but not
  the donor base archives themselves
- streamed inventory already shows a policy split:
  donor `glibc` archive carries a heavy `opt/` tooling/runtime layer plus
  archive noise like `.DS_Store` / `._*`, while donor `bionic` archive looks
  closer to a clean userland with Vulkan/OpenAL surfaces that overlap
  significantly with the current local `imagefs`
- the next hard requirement is archive-level diffing and library classification,
  not more speculative UI/runtime guesses around rootfs behavior

See also:

- [IMAGEFS_HYBRID_PLAN.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_HYBRID_PLAN.md)
- [IMAGEFS_LAYER_OWNERSHIP_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md)
- [IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md)
