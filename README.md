<p align="center">
  <img src="docs/assets/winlator-cmod-aesolator-logo.png" alt="Ae.solator" width="920">
</p>
<p align="center">
  <img src="docs/assets/aesolator-accent-bar.svg" alt="Ae.solator accent bar" width="920">
</p>

# Ae.solator

Android application repository for Ae.solator (`by.aero.so.benchmark`).

## Scope

- App/UI/runtime binding layer.
- Contents integration and package provenance UX.
- Forensic collection and runtime diagnostics sync for issue bundles.

## Current Mainline State

- App source-of-truth stays in this repository.
- Runtime source-of-truth is `kosoymiki/freewine11`.
- Archive/release orchestration lives in `kosoymiki/wcp-runtime-lanes`.
- Graphics/provider package lanes live in `kosoymiki/wcp-graphics-lanes`.
- `Contents` is expected to expose distinct source provenance (`WCP Archive` vs
  `WCPHub`) instead of collapsing them into one lane.

## Split Model

- `kosoymiki/aesolator`: app source-of-truth.
- `kosoymiki/freewine11`: native FreeWine source tree.
- `kosoymiki/wcp-runtime-lanes` (WCP Archive): Aesolator APK lane + FreeWine + VulkanSDK + DXVK + VKD3D + dgVoodoo WCP lanes.
- `kosoymiki/wcp-graphics-lanes`: Turnip/OpenGL provider lanes + build owner for dgVoodoo archive lane.
- `kosoymiki/winlator-wine-proton-arm64ec-wcp`: legacy archived history only.

Legacy donor runtime lanes are not active in Ae.solator.

## Main Workflow

- `.github/workflows/ci-winlator.yml`
  - legacy stub in this repository (disabled mainline build lane).
  - active APK build/release lane runs in `kosoymiki/wcp-runtime-lanes`:
    `.github/workflows/ci-aesolator-apk.yml`.

Legacy CI patch-overlay stack has been removed; this repository stays native source-of-truth for app code, while archive publishing is centralized in WCP Archive.

## Local Build

```bash
bash ci/winlator/ci-build-winlator-ludashi.sh
```

For verified on-device Termux ARM64 local build and Wi-Fi ADB install notes,
see `docs/TERMUX_LOCAL_BUILD.md`.

## Runtime/Graphics Matrix

<p align="center">
  <img src="docs/assets/aesolator-runtime-lanes.svg" alt="Aesolator runtime lanes matrix" width="980">
</p>

## Docs

- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/ADB_HARVARD_DEVICE_FORENSICS.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
- `docs/TERMUX_LOCAL_BUILD.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md`
- `docs/GAMENATIVE_FULL_TRANSFER_MATRIX.md`
- `docs/IMAGEFS_HYBRID_PLAN.md`
