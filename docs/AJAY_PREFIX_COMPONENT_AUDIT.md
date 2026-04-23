# Ajay Prefix Component Audit

Updated: `2026-03-18`

## Goal

Use `Ajay Prefix Pro` as an audited inventory source without collapsing
`Ae.solator` into Ajay's proprietary shell layout.

## Audited Inputs

- Ajay public repo README and changelog
- Ajay public wiki pages for installation, start menu, app store, and
  recommended fixes
- live `Ae.solator` device traces from `2026-03-18`
- live device tree under the active Wine prefix:
  `C:\AJAY_PREFIX_PRO`
  and
  `C:\users\xuser\Documents\Ajay_prefix\save_data`

## Hard Boundary

Do not import Ajay's proprietary AutoIt / batch shell as-is.

Ajay's own license keeps the custom scripts and save-redirection logic under
 Ajay ownership, while third-party redistributables stay under their original
 vendors' licenses.

For `Ae.solator`, Ajay is useful for:

- component inventory
- save-data and log-root discipline
- UI/menu ideas
- install-flow inspiration

Ajay is not the source-of-truth for:

- payload ownership
- package provenance
- wrapper/runtime versioning
- script redistribution

## Observed Ajay Shell Footprint

The live prefix shows these concrete Ajay-side script families:

- `AjayStartMenuPro.bat`
- `Ajay_CDrive_Backup_Restore_Tool.ajau3`
- `Ajay_Registry_Export_Import_Tool.ajau3`
- `Ajay_Start_Menu_Pro_viewer.ajau3`
- `App Paths Manager.ajau3`
- `Backup.bat`
- `timeout.ajau3`
- `URL_Status.ajau3`

The live prefix also confirms Ajay's dedicated save-data root:

- `C:\users\xuser\Documents\Ajay_prefix\save_data`

And installer residue shows a redirected Windows package cache under that
root, including VC runtime MSI payloads such as:

- `vcRuntimeMinimum_x86`
- `vcRuntimeAdditional_x86`
- `VC_Runtime_arm64`

## Ajay Component Families

Ajay advertises and/or exposes these useful component groups:

- VC Redist AIO
- Wine Mono
- DXVK
- VKD3D
- dgVoodoo / ddraw / wrapper fixes
- PhysX
- XNA
- OpenAL
- XAudio2_9
- FAudio
- Media Foundation related helpers
- VulkanRT
- LAVFilters
- SteamCMD helper
- extraction/tooling helpers such as `HLLib`, `AnyBurn`, `Path2Exe`,
  `3DAnalyzer`, `DOSBox`

## Classification For Ae.solator

### 1. Keep As Dedicated Payload / Contents Lane

Do not move these into `prefix-pack`:

- `DXVK`
- `VKD3D`
- `DgVoodoo`
- `VulkanSDK`
- graphics-driver packages
- emulator DLL/runtime wrapper payloads

Reason:

- they already have versioned payload owners in `Ae.solator`
- they need package metadata, runtime routing, and app-side activation logic
- duplicating them in `prefix-pack` would create two owners for one runtime
  surface

### 2. Valid Prefix-Pack Candidate Families

These fit the `prefix-pack` model if and only if each gets a source-backed
upstream and a dedicated install bridge:

- `VC++` redistributables
- `Wine Mono`
- `Wine Gecko`
- `DirectX June 2010`
- `DirectX SDK June 2010` tooling lane
- `XNA 3.1`
- `XNA 4.0`
- `OpenAL`
- `PhysX` legacy/system software
- `LAVFilters`

These are good candidates because they behave like prefix-local Windows
redistributables rather than app-managed runtime payloads.

### 3. Hold For Further Audit

These should not be added blindly:

- `FAudio`
- `Media Foundation` add-ons
- `XAudio2_9` as a separate lane
- `VulkanRT`
- `SteamCMD`
- `AnyBurn`
- `HLLib`
- `Path2Exe`
- `3DAnalyzer`
- `DOSBox`

Reasons vary:

- unclear official redistribution path
- overlap with existing rootfs/runtime ownership
- tooling rather than redistributable dependency
- better suited as optional standalone apps than as prefix bootstrap defaults

## Current Repository Impact

The prefix-pack contract remains:

- source-backed manifest
- repo/rootfs/Windows loader parity
- dedicated save-data/log roots
- no duplication of app-managed payload lanes

This means Ajay broadens our inventory and prioritization, but not the
ownership boundary.

## Next Safe Expansion Order

1. keep the current prefix-pack stable
2. add only source-backed redistributables that are not already owned by
   `Contents`
3. wire each new entry through:
   `catalog.tsv`
   repo helper
   rootfs shell loader
   Windows loader
   start-menu entry
   install script
4. verify on device with a clean forensic session

## Current Recommendation

Treat the next prefix-pack expansion lane as:

- `XNA 3.1` and `XNA 4.0`
- `OpenAL`
- `PhysX`
- `LAVFilters`

Keep everything else on hold until there is a clean source/provenance and
ownership decision.
