# ImageFS Per-Library Adoption Table

Updated: `2026-03-15`

## Base Decision

`Ae.solator` now treats `GameNative` rootfs as the canonical base lane:

- `imagefs_bionic.txz` is the canonical donor base for `bionic`
- `imagefs_gamenative.txz` is the canonical donor base for `glibc`
- `ubuntufs` is the delivery shell around that donor rootfs lane
- local `imagefs.txz` is no longer the planned mainline base; it is legacy hold
  material only

Hybrid mode now means:

- donor rootfs base archives
- donor overlays where they are clearly runtime/tool overlays
- local `Contents`, forensic, and package-placement ownership on top

## Adoption Table

| Library / Payload | Adopt From | Layer | Placement / Contract | Decision |
| --- | --- | --- | --- | --- |
| `usr/lib/libvulkan.so*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/lib` | Adopt donor base |
| `usr/share/vulkan/icd.d/*`, `implicit_layer.d/*`, `explicit_layer.d/*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/share/vulkan` | Adopt donor base |
| `usr/share/openal/*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/share/openal` | Adopt donor base |
| `usr/lib/libGL.so*`, `usr/lib/libglapi.so*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/lib` | Adopt donor base |
| `usr/lib/libpulse*`, `usr/lib/libpulseaudio*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/lib` | Adopt donor base |
| `usr/lib/libandroid-sysvshm.so` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/lib` | Adopt donor base |
| `usr/etc/fonts/*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/etc/fonts` | Adopt donor base |
| `usr/etc/tls/*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/etc/tls` | Adopt donor base |
| `usr/etc/xdg/*`, `usr/etc/dbus-1/*` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/etc` | Adopt donor base |
| `usr/bin/curl`, `openssl`, `ffmpeg`, `xmllint`, `winetricksfolder` | `imagefs_bionic.txz` | Base rootfs | `imagefs/usr/bin` | Adopt donor base |
| `opt/winetricks` | local `imagefs.txz` hold plus donor review | Utility base | `imagefs/opt/winetricks` | Keep local until donor diff proves replacement |
| `opt/wine/bin/*` | `imagefs_gamenative.txz` | Glibc runtime base | `imagefs/opt/wine/bin` | Adopt donor base |
| `opt/wine/lib/wine/*` | `imagefs_gamenative.txz` | Glibc runtime base | `imagefs/opt/wine/lib/wine` | Adopt donor base |
| `opt/wine/share/wine/fonts/*`, `nls/*`, `wine.inf` | `imagefs_gamenative.txz` | Glibc runtime base | `imagefs/opt/wine/share/wine` | Adopt donor base |
| `usr/lib/libredirect.so`, `usr/lib/libredirect-bionic.so` | `redirect.tzst` | Overlay | `imagefs/usr/lib` | Adopt donor overlay |
| `usr/lib/libevshim.so`, `usr/lib/libdummyvk.so` | APK native dir mirrored by installer | APK-to-guest helper lane | `imagefs/usr/lib` | Keep local bridge on donor base |
| `opt/system32/X3DAudio*`, `XAPOFX*`, `xactengine*`, `xaudio2_*` | `imagefs_patches_gamenative.tzst` | Runtime patch overlay | `imagefs/opt/system32` | Adopt donor overlay |
| `opt/apps/TestD3D.exe`, `GPUInfo.exe` | `imagefs_patches_gamenative.tzst` / `extras.tzst` | Utility overlay | `imagefs/opt/apps` | Adopt donor overlay |
| `Steamless/*`, `generate_interfaces_file.exe` | `extras.tzst` / `imagefs_patches_gamenative.tzst` | Utility overlay | root / `imagefs/Steamless` | Adopt donor overlay |
| `opt/7-Zip/*`, `opt/mono-gecko-offline/*` | `extras.tzst` / `imagefs_patches_gamenative.tzst` | Utility overlay | `imagefs/opt/7-Zip`, `imagefs/opt/mono-gecko-offline` | Adopt donor overlay |
| `usr/local/bin/box64` | `Contents` `Box64` package lane | Runtime payload | `imagefs/usr/local/bin/box64` | Canonical package target |
| `usr/bin/box64` | compatibility bridge | Compat shim | `imagefs/usr/bin/box64 -> ../local/bin/box64` | Keep symlink for old consumers |
| `/tmp` | donor rootfs contract | Runtime mutable state | `imagefs/tmp` | Canonical temp/socket root |
| `/usr/tmp` | compatibility bridge | Compat shim | `imagefs/usr/tmp -> ../tmp` | Keep bridge for legacy local code and old payloads |
| `imagefs.txz` | old local archive | Legacy hold | assets only | Hold, not canonical mainline |
| `imagefs.txz.02` | stale fragment | Orphan baggage | assets only | Reject from mainline |

## Package Placement Rules On Top Of Donor RootFS

- `Wine` / `Proton` install roots now belong under donor-style `imagefs/opt`,
  not under legacy `files/contents/Wine` or `files/contents/Proton`.
- `Contents` remains the source of metadata and install verification, but the
  runtime payload root for `Wine` / `Proton` is now the donor rootfs `opt`
  surface.
- `Box64` is canonical at `usr/local/bin/box64`; `usr/bin/box64` survives as a
  compatibility symlink for older launch paths.
- `Vulkan SDK`, `DXVK`, `VKD3D`, `dgVoodoo`, `Turnip`, and OpenGL-driver lanes
  stay overlay-style payloads applied on top of the donor base instead of
  becoming their own rootfs base.

## `ubuntufs` Role

`ubuntufs` is not treated as the owner of package placement or runtime policy.
It is the delivery shell for the donor rootfs lane:

- base module: `:app`
- on-demand rootfs delivery module: `:ubuntufs`
- runtime/package logic remains in the app layer:
  `ImageFs`, `ImageFsInstaller`, `ContentsManager`, `ContainerManager`,
  launcher components, and forensic hooks

## Remaining Honest Tail

This table closes the per-library adoption-writing tail.

What still remains after it:

- first honest compile of the staged donor-transfer batch
- runtime proof that the new donor-rootfs-first install paths and compat
  bridges behave as expected on device
