# ImageFS Layer Ownership Table

Updated: `2026-03-15`

## Purpose

Explicit ownership map for the current `Ae.solator` rootfs lane after the
donor transfer foundation pass.

This document exists to stop the hybrid `imagefs` lane from collapsing into
"one more archive". Each artifact below is classified by what it actually is:

- base rootfs
- overlay
- runtime payload
- prefix skeleton
- delivery fragment / orphan baggage

## Layer Table

| Artifact | Current Role | Ownership | Key Evidence |
| --- | --- | --- | --- |
| `imagefs.txz` | Current local broad userland base | Local base rootfs | Exposes `usr/bin`, `usr/lib`, `usr/share`, Vulkan/OpenAL, PulseAudio, GStreamer, DBus, `curl`, `openssl`, `ffmpeg`, `winetricksfolder` |
| `imagefs_bionic.txz` | Donor userland freshness reference | Donor base rootfs reference | Streamed inventory overlaps strongly with local broad userland and Vulkan/OpenAL surface |
| `imagefs_gamenative.txz` | Donor runtime-heavy glibc base/reference | Donor base/runtime reference | Carries `opt/wine` and related runtime-heavy `opt/` surface; also contains macOS archive noise |
| `imagefs_patches_gamenative.tzst` | Donor runtime/tool patch overlay | Donor overlay lane | Owns `opt/system32` XAudio/XACT DLLs, `opt/apps`, `7-Zip`, `Steamless`, `generate_interfaces_file.exe` |
| `extras.tzst` | Utility overlay | Donor overlay lane | Carries `Steamless`, `7-Zip`, `GPUInfo.exe`, `TestD3D.exe`, `generate_interfaces_file.exe`, offline mono payload |
| `redirect.tzst` | Redirect-hook overlay | Donor overlay lane | Carries `usr/lib/libredirect.so` and `usr/lib/libredirect-bionic.so` |
| `container_pattern_common.tzst` | Generic prefix skeleton | Local prefix/bootstrap overlay | Used on `bionic` path at boot/runtime patch stage |
| `container_pattern_gamenative.tzst` | Donor main-runtime prefix skeleton | Donor prefix/bootstrap overlay | Staged locally for glibc/main-runtime bootstrap and compact pattern regeneration |
| `pulseaudio.tzst` | Generic audio overlay | Local audio overlay | Used by local `bionic` runtime patch path |
| `pulseaudio-gamenative.tzst` | Donor audio overlay | Donor audio overlay | Used by local glibc/main-runtime path with fallback to generic pulse overlay |
| `imagefs.txz.02` | Orphan fragment, not a live shard | Invalid/unowned baggage | Unreferenced in source, fails `xz -t`, random-looking header, no valid standalone archive contract |

## Concrete Findings

- Local `imagefs.txz` is already a broad runtime base, not a thin Wine-only
  payload. Replacing it blindly with donor `glibc` would collapse base-layer
  ownership and pollute the archive with donor-specific `opt/` runtime content.
- Donor `imagefs_bionic.txz` is the cleaner freshness reference for userland
  libraries because it overlaps with the local broad base instead of dragging
  in a heavier runtime/tool surface.
- Donor `imagefs_gamenative.txz` behaves more like a runtime-rich glibc base
  plus `opt/` content than a clean universal replacement rootfs.
- Donor `imagefs_patches_gamenative.tzst`, `extras.tzst`, and `redirect.tzst`
  are overlay artifacts. They should be evaluated as overlays, not mixed into
  base-rootfs accounting.
- Donor overlay and glibc artifacts carry foreign host metadata
  (`.DS_Store`, `._*`). Any future rebuild path needs an explicit cleanup
  filter.

## `imagefs.txz.02` Note

`imagefs.txz.02` currently has no live installer contract:

- no local source file references it
- it is not a valid xz archive under `xz -t`
- its header does not match a valid xz stream
- it currently looks like stale baggage or a broken fragment, not a usable
  split archive shard

Until a real sharding contract appears in code and verification, treat
`imagefs.txz.02` as orphan data, not as part of the hybrid rootfs plan.

## Compile-Boundary State

The rootfs lane has now crossed from intuition to explicit ownership mapping.

What is closed:

- donor archive source and delivery/fallback are documented
- donor/local overlays are classified
- donor prefix/audio overlays are classified
- local `imagefs.txz` versus donor `bionic` / `glibc` roles are separated
- `imagefs.txz.02` is explicitly classified as orphan baggage

What remains before the first honest compile:

- per-library subsystem adoption table for donor rootfs deltas
- compile/runtime proof for the staged donor transfer batch
- future hybrid rebuild decisions for which files belong in base versus
  overlay versus runtime payload lanes

See also:

- [IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md)
