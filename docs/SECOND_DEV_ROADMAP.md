# Second Developer Roadmap

Updated: `2026-03-14`

## Mission

Second autonomous developer lane for `aesolator`:

- tighten app/UI behavior against the active split model,
- keep runtime/forensic contracts visible,
- close remaining `Contents` UX and device-validation gaps,
- report deltas clearly to the first developer.

## Current Priorities

1. `Contents` UI contract closure
   - top-card sizing/alignment
   - selector readability and lane visibility
   - source provenance clarity (`WCP Archive` vs `WCPHub`)
   - install/update/remove UX checks
2. `Containers` action-surface cleanup
   - expose `New Container` and `Big Picture` as first-class dashboard cards
     in the main menu grid
   - keep the `Containers` screen focused on the container list itself
   - keep container creation on the canonical `ContainerDetailFragment` path
   - reduce icon-only ambiguity before further crash/device passes
3. `Contents` source-of-truth enforcement
   - `REMOTE_WINE_PROTON_OVERLAY` must track `aesolator/contents/contents.json`
   - `Wine` lane must expose archive-managed `freewine11`
4. Documentation sync
   - keep `AGENTS.md`, roadmap, and reflective journal current
   - keep implementation notes aligned with repo contracts
5. Handoff/reporting discipline
   - summarize each completed step for the first developer
   - call out verification gaps explicitly

## Phase Plan

### Phase 1: Contents Surface

- close layout friction in top cards and selectors
- ensure visible titles remain readable without forced wrapping in selectors
- preserve intentional wrapping only where action labels need it

### Phase 2: Contents Behavior

- verify source filter logic against `docs/CONTENTS_QA_CHECKLIST.md`
- verify archive/hub provenance rendering in list rows
- audit install-state transitions and duplicate/update behavior

### Phase 3: Device Validation Prep

- prepare ADB-oriented verification checklist from existing docs
- identify which items remain code-only vs device-only

## Current Open Risks

- `Contents` layout changes are now build-validated and installed locally, but
  screen-level behavioral QA still needs explicit in-app review.
- native runtime loading for container-adjacent flows now has an explicit
  `libc++_shared.so` packaging path in the APK, but the actual create-container
  and graphics-driver dialog path still needs one fresh device retest after the
  native packaging fix.
- Termux local build currently depends on local-only SDK/NDK compatibility
  shims; this is an environment workaround, not a committed repository fix.
- `llvm-strip` from the desktop NDK host bundle is still incompatible with
  Termux ARM64, so debug packaging currently proceeds with unstripped native
  libraries.
