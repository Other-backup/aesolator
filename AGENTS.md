# AGENTS

## Role

This repository is the source-of-truth for the Ae.solator Android application.

## Rules

- `Ae.solator` is the full app-owner repository. UI, Contents, runtime-binding,
  preset UX, and device/debug surfaces live here.
- The app owner is expected to work autonomously in this repo:
  - monitor logs
  - implement app-side fixes
  - update app docs
  - push app-side changes without waiting for manual dispatch
- Keep app/UI/runtime-binding logic here.
- Do not treat `wcp-runtime-lanes` as an app source repository.
- Do not collapse `WCP Archive` and `WCPHub` provenance in docs or UI logic.
- Keep roadmap and docs aligned with the actual split model and active runtime
  line (`FreeWine 11.4`).
- Preserve the mixed-mode ownership model:
  - runtime owner stays in `freewine11` + `wcp-runtime-lanes`
  - app owner stays in `aeolator`
- If a second autonomous developer is active, the expected execution model is:
  - runtime owner in the main runtime trees
  - app owner in a dedicated `aeolator` worktree
- Current app-side operating docs:
  - `docs/DUAL_DEV_OPERATING_MODEL.md`
  - `docs/ACTIVE_OWNERSHIP_MAP.md`

## Main Docs

- `README.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/DUAL_DEV_OPERATING_MODEL.md`
- `docs/ACTIVE_OWNERSHIP_MAP.md`
