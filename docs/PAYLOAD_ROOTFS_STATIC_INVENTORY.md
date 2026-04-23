# Payload RootFS Static Inventory

Date: 2026-03-15

Scope: static post-patch inventory before the first honest compile.

## Status Table

| Lane | Static status | Notes |
| --- | --- | --- |
| RootFS base selection | Closed | `ImageFsInstaller` accepts only donor `imagefs_bionic.txz` / `imagefs_gamenative.txz` as live base archives |
| Legacy rootfs baggage | Closed | old `imagefs.txz` and `imagefs.txz.02` moved out of `app/src/main/assets` into `.legacy_rootfs/` |
| RootFS marker normalization | Closed | stale `.provider` / `.layout` values normalize through live provider/layout inference, and launch/install paths no longer auto-stamp missing markers as `gamenative` / `ubuntufs` |
| `Wine` / `Proton` install roots | Closed | canonical `runtime-<model>-<family>-<version>-<verCode>` roots in shared `/opt` |
| `Wine` / `Proton` resolver | Closed | no silent cross-family fallback; `runtimeModel` now participates in resolution |
| Container launch to launcher model | Closed | `XServerDisplayActivity` enforces rootfs prep for requested runtime model before launcher creation |
| `Vulkan SDK` install metadata | Closed | synthetic profile fallback now works from extracted package structure when feed metadata exists |
| `Vulkan SDK` launch selection | Closed | launcher now chooses one coherent installed SDK version group and records layout/arch coverage |
| `dgVoodoo` install metadata | Closed | synthetic profile fallback now works from extracted package structure when feed metadata exists |
| `dgVoodoo` install placement | Closed | manager now prefers canonical `Contents` destination when package profile exists |
| `dgVoodoo` dependency validation | Closed | dependency check validates the actual stage arch, not just “some package exists” |
| Embedded runtime arrays | Open by design | `wine_entries`, `glibc_wine_entries`, `bionic_wine_entries` stay empty under the current Contents-first lane |
| Donor archive staging | Open for proof | donor rootfs archives are still staged at build time; first compile remains the packaging proof point |

## Concrete Inventory

- Live packaged rootfs path:
  donor-only, via `imagefs_bionic.txz`, `imagefs_gamenative.txz`,
  `imagefs_patches_gamenative.tzst`
- Archived local residue:
  `.legacy_rootfs/imagefs.txz`, `.legacy_rootfs/imagefs.txz.02`
- Runtime package placement:
  `Wine` / `Proton` in shared `imagefs/opt`, other payload families under
  `files/contents/<Type>` with launch-time application or staging
- Vulkan SDK contract:
  install package may be synthesized from extracted tree, launch chooses a
  coherent installed SDK group, env now records profile list, profile count,
  arch coverage, and layout
- dgVoodoo contract:
  install package may be synthesized from extracted tree, manager resolves
  canonical install destination from profile when available, stage dependency
  is arch-aware

## Honest Residuals

- No compile has been run for this batch.
- No device/runtime proof has been run for donor-rootfs-first `Vulkan SDK` and
  `dgVoodoo` after these static changes.
- Build-time donor archive staging still needs real packaging proof.
