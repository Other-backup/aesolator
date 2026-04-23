# Repo Split Topology

Final delivery split for Ae.solator.

## Repositories

- `kosoymiki/aesolator`
  - Android app source-of-truth.
- `kosoymiki/freewine11`
  - Native FreeWine source tree.
- `kosoymiki/wcp-runtime-lanes` (**WCP Archive**)
  - Archive release host for:
    - `aesolator-latest` (APK lane)
    - `freewine11-arm64ec-latest`
    - `dxvk-gplasync-latest`
    - `dxvk-gplasync-arm64ec-latest`
    - `vkd3d-proton-latest`
    - `vkd3d-proton-arm64ec-latest`
    - `dgvoodoo-x86_64-latest`
    - `dgvoodoo-arm64ec-latest`
- `kosoymiki/wcp-graphics-lanes`
  - Graphics build/control + release host for:
    - `aeturnip-arm64-latest`
    - `aeopengl-driver-arm64-latest`
  - Build owner for archive lane:
    - `dgvoodoo-x86_64-latest` / `dgvoodoo-arm64ec-latest` (published to `wcp-runtime-lanes`)
- `kosoymiki/winlator-wine-proton-arm64ec-wcp`
  - Legacy monorepo, archived-only history.

## Contract Rules

1. `contents/contents.json` and artifact maps must use the real release owner per lane.
2. Aesolator APK release lane is owned by `wcp-runtime-lanes` (`aesolator-latest`).
3. DXVK/VKD3D WCP lanes must route to `wcp-runtime-lanes`.
4. Turnip/OpenGL lanes route to `wcp-graphics-lanes`; dgVoodoo WCP routes to `wcp-runtime-lanes`.
5. Legacy monorepo is excluded from active release routing.

## Status

- Split ownership is active.
- Remaining work is app/device behavioral QA, not repo routing.
