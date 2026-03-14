# Second Developer Roadmap

Updated: `2026-03-14`

## Mission

Second autonomous developer lane for `aesolator`:

- tighten app/UI behavior against the active split model,
- keep runtime/forensic contracts visible,
- close remaining `Contents` UX and device-validation gaps,
- report deltas clearly to the first developer.

## Current Priorities

1. `New Container` device-led UX closure
   - keep container creation free of XR/native startup regressions
   - polish the runtime-missing state, footer actions, and tab readability from
     real screenshots instead of XML-only inference
   - run top-to-bottom behavioral QA on the full create flow
   - confirm the new cold-start routing after deferred prompts on a stable
     foreground session
2. `Contents` integrated source closure
  - keep `WCP Archive` and `WCPHub` visible together without provenance drift
  - preserve top-card sizing/alignment and selector readability
  - verify install/update/remove behavior on the current build
  - keep overlapping `WCPHub` families (`Wine`, `DXVK`, `VKD3D`) visible when
    the source selector is explicitly switched away from archive
  - keep content badges semantically aligned with the selected family instead
    of leaking `Proton` naming into non-Wine package rows like `VKD3D`
3. Dashboard adaptive polish
   - verify the new landscape density pass on a stable device session
   - keep the main menu readable as a control surface, not a stretched portrait
     grid
4. `Contents` source-of-truth enforcement
   - `REMOTE_WINE_PROTON_OVERLAY` must track `aesolator/contents/contents.json`
   - `Wine` lane must expose archive-managed `freewine11`
5. Documentation sync
  - keep `AGENTS.md`, roadmap, and reflective journal current
  - keep implementation notes aligned with repo contracts
  - keep the Termux local-build path self-contained instead of relying on
    ad-hoc CLI flags for `aapt2`
6. Handoff/reporting discipline
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

- `New Container` now opens on-device after the XR lazy-load split, but the
  entire create flow still needs end-to-end QA beyond initial screen entry and
  footer/layout checks.
- Direct cold-start validation for `selected_menu_item_id` flows is now coded
  more defensively in `MainActivity`, but shared-device foreground contention
  still limits clean screenshot proof for those routes.
- The landscape dashboard density pass is build-complete and installed, but it
  still needs one stable foreground capture to confirm the new 4-column control
  surface on the target phone.
- `Contents` no longer loses archive provenance on feed failure, but live
  `WCPHub` plus `WCP Archive` behavior still needs explicit on-device review
  across source switching and install actions.
- `WCPHub` source parsing no longer drops overlapping families at ingest time,
  but the integrated device pass still needs one more live confirmation for
  list rendering and install actions after the parser fix.
- The repo-side `Contents` workflow contract is now aligned with the static
  checklist gate, so the remaining `Contents` risk is device behavior rather
  than source-of-truth drift inside `.github/workflows/ci-winlator.yml`.
- Termux local build currently depends on local-only SDK/NDK compatibility
  shims; this is an environment workaround, not a committed repository fix.
- `llvm-strip` from the desktop NDK host bundle is still incompatible with
  Termux ARM64, so debug packaging currently proceeds with unstripped native
  libraries.
