# Second Developer Roadmap

Updated: `2026-03-14`

## Mission

Second autonomous developer lane for `aesolator`:

- tighten app/UI behavior against the active split model,
- keep runtime/forensic contracts visible,
- close remaining `Contents` UX and device-validation gaps,
- report deltas clearly to the first developer.

## Execution Policy

- Convert multi-goal user messages into one ordered backlog and keep that order
  visible instead of bouncing between fresh requests ad hoc.
- Default lane order for this repo:
  requested product/UI/content work first, documentation sync second,
  debugging/forensics third.
- Break the order only when a blocker crash/build/runtime defect prevents the
  next planned task from moving.
- Every forced reorder must be logged in the reflective journal with:
  blocker, reason, scope impact, and explicit return point.

## Current Priorities

1. `New Container` device-led UX closure
   - keep container creation free of XR/native startup regressions
   - polish the runtime-missing state, footer actions, and tab readability from
     real screenshots instead of XML-only inference
   - normalize boolean controls and env-var editing so the flow stops looking
     like a mix of legacy checkbox/toggle widgets
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
  - expand GameHub release ingestion beyond a single page and sort the visible
    releases by source lane, channel, version, architecture, and package format
  - keep `Proton` and `Wine` routed through verified runtime sources only until
    a donor feed exposes complete packaged runtimes instead of raw skeleton
    payloads
  - audit `BannersComponentInjector` as a donor for full package-link harvest,
    release-tag browsing, search/sort UX, and release-notes surfaces without
    collapsing `Ae.solator` provenance rules
3. Dashboard adaptive polish
   - verify the new landscape density pass on a stable device session
   - keep the main menu readable as a control surface, not a stretched portrait
     grid
4. Shared control-system polish
   - keep selectors, switches, seekbars, preference rows, and compact toggles
     on one geometry system instead of drifting by screen
   - prefer global style/resource fixes where possible, then validate on live
     `Contents`, `Settings`, and `Graphics Center` screens
   - keep long text readable by truncation or controlled wrapping, not marquee
5. `Contents` source-of-truth enforcement
   - `REMOTE_WINE_PROTON_OVERLAY` must track `aesolator/contents/contents.json`
   - `Wine` lane must expose archive-managed `freewine11`
6. Documentation sync
  - keep `AGENTS.md`, roadmap, and reflective journal current
  - keep implementation notes aligned with repo contracts
  - keep the Termux local-build path self-contained instead of relying on
    ad-hoc CLI flags for `aapt2`
7. Handoff/reporting discipline
   - summarize each completed step for the first developer
   - call out verification gaps explicitly
8. Donor and runtime reverse-engineering
   - inspect `The412Banner/BannersComponentInjector` for safe source/feed
     improvements and document every borrowed behavior before integration
   - map `imagefs` structure, libraries, overlays, and runtime patch points in
     detail before changing rootfs-related install logic
   - keep release artifacts traceable after install:
     source label, release tag, artifact name, published date, and notes should
     survive from remote feed to local profile metadata where available

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

### Phase 4: Donor Source Harvest

- audit `BannersComponentInjector` source/release UX and package-feed model
- port only contract-safe improvements:
  full release pagination, better search/sort, richer release metadata, and
  stronger package-link normalization
- verify any donor runtime package assumptions against actual extractable
  payload structure before exposing them in `Contents`

### Phase 5: ImageFS Reverse Map

- document base `imagefs` layout, shipped libraries, runtime overlays, and
  patch application points
- separate base rootfs facts from container-time prefix/runtime mutation
- identify which parts of `imagefs` are safe to optimize in-app versus those
  owned by runtime-lane artifacts

## Current Open Risks

- `New Container` now has a verified end-to-end baseline on-device: a local
  donor runtime resolves correctly, `Create` reaches `Creating Container…`,
  and a real `/files/imagefs/home/xuser-*` container root is materialized.
  Remaining risk is now secondary runtime choice/defaulting and deeper
  post-create UX, not the old false missing-runtime gate.
- Direct cold-start validation for `selected_menu_item_id` flows is now coded
  more defensively in `MainActivity`, but shared-device foreground contention
  still limits clean screenshot proof for those routes.
- The landscape dashboard density pass is build-complete and installed, but it
  still needs one stable foreground capture to confirm the new 4-column control
  surface on the target phone.
- `Contents` no longer loses archive provenance on feed failure, but live
  `WCPHub` plus `WCP Archive` behavior still needs explicit on-device review
  across source switching and install actions. The `Install Runtime` path now
  lands on a populated `WCP Archive` / `Wine` view again.
- GameHub feed ingestion now includes paginated release polling and stronger
  visible ordering, but it still needs a live `Contents` device pass to confirm
  that nightly/stable and architecture variants render as intended in the UI.
- `Nightlies by The412Banner` is now integrated as a first-class `Contents`
  source lane for `Proton`, `DXVK`, `VKD3D`, `Box64`, `WOWBox64`, and
  `FEXCore`, but device-led validation of source switching and installation
  across those donor packages is still pending. Ordering now prefers
  `publishedAt`/`verCode`, and donor `Wine/Proton` intake now prefers
  compressed `.wcp.xz/.wcp.zst` artifacts because that is where the usable
  prefix-bearing payloads live.
- `Nightlies` source discovery is still vulnerable to unauthenticated GitHub
  API rate limiting on first load. Device logs now confirm `HTTP 403` from
  `api.github.com`, so the next closure step is a non-API or cached fallback
  path rather than more UI-only tuning.
- `Vulkan SDK` had a false installed-state in `Contents` because the base
  `imagefs` already ships `usr/share/vulkan`; package visibility now needs to
  stay tied to real `Contents` installs plus explicit `vulkanApiMin/max` /
  `vulkanSdkVersion` metadata so runtime pickup and UI state do not drift.
- `dgVoodoo` still had a split-brain contract: `Contents` packages lived in
  `contents/DgVoodoo/*`, while runtime stage/dependency checks looked only at
  `contents/dgvoodoo/current`. That bridge now needs to stay aligned so
  package installs, wrapper presence checks, and runtime staging all see the
  same installed payload set.
- `BannersComponentInjector` still has deeper donor logic not yet harvested
  locally, especially around richer source discovery, release-tag browsing, and
  download-management UX.
- A cross-repo handoff is now open for the `freewine11` build lane:
  review Valve Wine commit
  `6ccff11d0e7d620cd958b56b0904fcbd9a9bfb26` in the dedicated handoff note
  before the next runtime build churn.
- That handoff has widened into a full upstream compare task:
  `ValveSoftware/wine:proton_10.0...GameNative/proton-wine:proton_10.0`
  is a 3-commit Android/Winlator downstream layer, not just one isolated fix.
- `WCPHub` source parsing no longer drops overlapping families at ingest time,
  but the integrated device pass still needs one more live confirmation for
  list rendering and install actions after the parser fix.
- The shared selector/switch/preference geometry pass is now built and
  installed, but many secondary dialogs still have only code-level validation
  rather than screenshot proof on the target device.
- `Graphics Driver Configuration` no longer crashes on open and no longer
  renders an empty extensions line: it now uses a safe catalog-backed
  extension source on device instead of the old native probe path that could
  crash inside `libwinlator.so`. Remaining risk is quality of the extension
  catalog itself, not dialog stability or selector presence.
- The `New Container` boolean-control/env-var cleanup is build-complete and
  installed, but stable screenshot proof of the deeper tabs is still limited by
  shared-device foreground hijacking during ADB capture.
- Device-side inspection now confirms local `Wine` and `Proton` package roots
  under `files/contents`, and the old `New Container` failure was a runtime-id
  resolution bug caused by `Contents` entry names carrying `-verCode` suffixes.
  The next pass should therefore focus on runtime selection quality and other
  downstream consumers, not on re-proving basic package presence.
- The repo-side `Contents` workflow contract is now aligned with the static
  checklist gate, so the remaining `Contents` risk is device behavior rather
  than source-of-truth drift inside `.github/workflows/ci-winlator.yml`.
- Termux local build currently depends on local-only SDK/NDK compatibility
  shims; this is an environment workaround, not a committed repository fix.
- `llvm-strip` from the desktop NDK host bundle is still incompatible with
  Termux ARM64, so debug packaging currently proceeds with unstripped native
  libraries.
- `imagefs` now has a documented reverse map, so the remaining rootfs risk is
  no longer “unknown structure” but future ownership mistakes between base
  image, Wine payloads, and boot-time overlays.
