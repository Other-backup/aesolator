<p align="center">
  <img src="docs/assets/winlator-cmod-aesolator-logo.png" alt="Aesolator" width="920">
</p>
<p align="center">
  <img src="docs/assets/aesolator-accent-bar.svg" alt="Aesolator accent bar" width="920">
</p>

# Aesolator

Android application repository for Ae.solator (`by.aero.so.benchmark`).

## Scope

- App/UI/runtime binding layer.
- Contents integration and package provenance UX.
- Forensic collection and runtime diagnostics sync for issue bundles.

## Split Model

- `kosoymiki/aesolator`: app source + APK lane.
- `kosoymiki/freewine11`: native FreeWine source tree.
- `kosoymiki/wcp-runtime-lanes` (WCP Archive): FreeWine + VulkanSDK + DXVK + VKD3D + dgVoodoo WCP lanes.
- `kosoymiki/wcp-graphics-lanes`: Turnip/OpenGL provider lanes + build owner for dgVoodoo archive lane.
- `kosoymiki/winlator-wine-proton-arm64ec-wcp`: legacy archived history only.

## Main Workflow

- `.github/workflows/ci-winlator.yml`
  - pulls current Winlator-Ludashi upstream,
  - applies app patch-base from `ci/winlator/patches`,
  - publishes APK release lane in this repo.

## Local Build

```bash
bash ci/winlator/ci-build-winlator-ludashi.sh
```

## Runtime/Graphics Matrix

<p align="center">
  <img src="docs/assets/aesolator-runtime-lanes.svg" alt="Aesolator runtime lanes matrix" width="980">
</p>

## Docs

- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/ADB_HARVARD_DEVICE_FORENSICS.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
