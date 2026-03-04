# Aesolator

Application repository for the Ae.solator Winlator fork (`by.aero.so.benchmark`).

## Scope

- Winlator app CI and patch stack.
- Forensic/runtime diagnostics integration for the app layer.
- Publication of APK lane `winlator-latest` to `kosoymiki/aesolator`.

## Upstream Split

- FreeWine source tree: `kosoymiki/freewine11`.
- Runtime packages: `kosoymiki/wcp-runtime-lanes`.
- Graphics/Vulkan packages: `kosoymiki/wcp-graphics-lanes`.

## Main Workflow

- `.github/workflows/ci-winlator.yml`
  - builds APK from Winlator-Ludashi upstream,
  - applies local patch stack from `ci/winlator/patches`,
  - publishes release assets into this repository.

## Local Run

```bash
bash ci/winlator/ci-build-winlator-ludashi.sh
```

## Docs

- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/ADB_HARVARD_DEVICE_FORENSICS.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
