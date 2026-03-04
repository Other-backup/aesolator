# Repo Split Topology

Final delivery split for Ae.solator.

## Repositories

- `kosoymiki/aesolator`
  - Android app source + APK release lane (`winlator-latest`).
- `kosoymiki/freewine11`
  - Native FreeWine source tree.
- `kosoymiki/wcp-runtime-lanes` (**WCP Archive**)
  - WCP release host for:
    - `freewine11-arm64ec-latest`
    - `vulkan-sdk-arm64-latest`
    - `vulkan-sdk-x86_64-latest`
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
2. DXVK/VKD3D/VulkanSDK must route to `wcp-runtime-lanes`.
3. Turnip/OpenGL lanes route to `wcp-graphics-lanes`; dgVoodoo WCP routes to `wcp-runtime-lanes`.
4. Legacy monorepo is excluded from active release routing.

## Status

- Split ownership is active.
- Remaining work is app/device behavioral QA, not repo routing.
