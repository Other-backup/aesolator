# Second Developer Reflective Journal

## 2026-03-14

### Entry 1: Repo intake and contract alignment

- Goal: establish operating context inside `aesolator`.
- Context: repository split and documentation define strict provenance and
  runtime/forensic ownership boundaries.
- Decision: treat `README.md`, `docs/README.md`, `docs/REPO_SPLIT_TOPOLOGY.md`,
  `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`, and
  `docs/CONTENTS_QA_CHECKLIST.md` as the active contract layer.
- Tradeoff: delayed code edits until contract shape was clear.
- Verification: documentation reviewed locally.
- Next step: audit `Contents` code against the documented contract.

### Entry 2: Contents source contract mismatch

- Goal: align `Contents` source behavior with checklist requirements.
- Context: `Wine/Proton` source selection was restricted to `WCPHub`, and the
  archive overlay URL pointed outside `aesolator`.
- Decision: open `WCP Archive` for `Wine/Proton` and repoint
  `REMOTE_WINE_PROTON_OVERLAY` to `aesolator/contents/contents.json`.
- Tradeoff: minimal code change first, deeper behavior audit deferred.
- Verification: code paths reviewed and patched; no build run yet.
- Next step: continue through UI readability and install-state UX.

### Entry 3: Contents top-card UI pass

- Goal: reduce friction in the top `Contents` cards.
- Context: top cards were visually uneven, selector row felt too tall/wide, and
  install action text needed controlled wrapping.
- Decision: move the top cards to a `60/40` split, tighten compact spinner row
  metrics, and allow the install button label to wrap by words within the card.
- Tradeoff: localized layout edits instead of broad style-system changes.
- Verification: XML/layout pass complete; runtime visual check still pending.
- Next step: inspect the screen in-app and adjust if any label still clips.

### Entry 4: Whole-card rhythm pass for Contents

- Goal: review the full `Contents` card stack as one surface instead of fixing
  isolated controls.
- Context: even after the first pass, the screen still risked feeling uneven if
  the top cards, source card, and list card used different spacing rhythm.
- Decision: normalize vertical cadence in `contents_fragment.xml`, force equal
  minimum height for the top cards, tighten outer spacing, and give the source
  card a slightly cleaner selector-to-scope-to-filter flow.
- Tradeoff: still kept the fix local to `Contents` instead of rewriting shared
  `FieldSet` styles used by the wider app.
- Verification: structural XML pass completed; visual/runtime validation still
  pending.
- Next step: compare the screen in-app and decide whether the shared
  `FieldSetCompact` style itself now needs a second-stage cleanup.

### Entry 5: Package-row card refinement

- Goal: bring the list-row cards into the same visual system as the rebuilt
  top section.
- Context: after the container-level rhythm pass, package rows still risked
  feeling denser and more vertically compressed than the surrounding cards.
- Decision: relax row padding slightly, align content to the top, give the
  icon/title/badge stack clearer breathing room, and let the description use a
  second line before truncation.
- Tradeoff: rows may become marginally taller, but the screen should read more
  coherently as a single surface.
- Verification: XML/layout pass complete; no runtime screenshot or device check
  yet.
- Next step: inspect real content rows and decide whether action buttons need a
  dedicated vertical action column in a later pass.

### Entry 6: Termux local build environment closure

- Goal: move from XML/code-only edits to a verified local APK build path.
- Context: the initial Termux build failed on desktop-host assumptions in SDK
  `cmake`, AGP `aapt2`, and NDK host compiler/runtime resolution.
- Decision: switch local build execution to Termux-native `cmake`, `ninja`, and
  `aapt2`, then add local-only SDK/NDK compatibility shims so Termux-hosted
  `clang`/`clang++` could consume Android sysroot and NDK runtime libraries.
- Tradeoff: build success now depends on local environment shims outside tracked
  repository files.
- Verification: `assembleDebug` completed successfully and produced
  `app/build/outputs/apk/debug/app-debug.apk`.
- Next step: install the built APK on a real device and capture the remaining
  screen-level QA notes.

### Entry 7: Wi-Fi ADB install and launch verification

- Goal: close the loop from local build to real-device install.
- Context: wireless ADB pairing succeeded, but the target device already had a
  higher installed `versionCode` than the local debug build.
- Decision: connect over Wi-Fi ADB, install with `adb install -r -d`, and
  verify the package version after downgrade-style debug install.
- Tradeoff: local debug package replaced a higher-version on-device build for
  validation purposes.
- Verification: device `NTN-LX1` accepted the install, package version became
  `20`, and launcher smoke test executed successfully.
- Next step: perform in-app `Contents` QA on the installed build and record the
  remaining behavior findings.

### Entry 8: Equal-width top cards adjustment

- Goal: remove the remaining width asymmetry in the top `Contents` row.
- Context: after the earlier `60/40` split, the user confirmed that the
  `Install content` card should match `Content type` in width rather than stay
  intentionally narrower.
- Decision: set both top cards to the same horizontal weight in
  `contents_fragment.xml` and keep the rest of the spacing model unchanged.
- Tradeoff: the install action loses the narrower emphasis, but the row now
  reads as a cleaner paired control surface.
- Verification: XML updated; runtime visual check still pending.
- Next step: inspect the installed build and see whether any remaining visual
  imbalance is now in shared card styling rather than row sizing.

### Entry 9: WCP Archive bundled-feed recovery

- Goal: restore `WCP Archive` visibility in `Contents` without collapsing the
  split-source contract back into a legacy public feed assumption.
- Context: the canonical `aesolator/contents/contents.json` endpoint was
  logically correct but not physically reachable from the app at runtime, which
  caused archive packages to disappear when the remote refresh returned empty.
- Decision: keep the canonical remote endpoint, package the repo-root
  `contents/contents.json` into the APK as an asset, and use it as a runtime
  fallback only when the archive remote feed cannot be fetched.
- Tradeoff: the app now ships a snapshot fallback that can lag behind the live
  repo until the APK is updated, but `WCP Archive` no longer vanishes on feed
  outage or private/raw access issues.
- Verification: code/build config patched; rebuild and device validation still
  required.
- Next step: rebuild the APK, reinstall it on-device, and confirm `Wine` plus
  archive lanes are visible again in `Contents`.

### Entry 10: Tree closure and commit discipline

- Goal: close working-tree tails before handoff and prevent silent loss of UI
  or build changes between passes.
- Context: the user reported that earlier `Contents` UI work looked lost, while
  the repository still contained the edited XML and there were no alternative
  resource variants overriding those files.
- Decision: restore the accidental `imagefs.txz.02` build-side deletion, codify
  mandatory commit-at-pass-end discipline in `AGENTS.md`, and align the local
  Termux env helper with the actual `aapt2`/PATH build setup used in practice.
- Tradeoff: local workflow rules are now stricter, but handoff state becomes
  auditable and less fragile.
- Verification: working tree inspected, accidental asset deletion cleared, no
  alternate `Contents` layout resources found.
- Next step: commit the current `Contents`/docs/build pass as one coherent
  checkpoint and continue device-side UI QA from committed state.

### Entry 11: Card sizing scale pass

- Goal: make `Contents` card sizes feel intentionally related instead of
  slightly drifting between top controls and package rows.
- Context: the XML edits were still present, but the user called out card
  sizing again, which pointed to a remaining scale/rhythm issue rather than a
  missing resource file.
- Decision: tighten the outer `Contents` padding, equalize top-card height with
  a cleaner 100dp block, move card gaps to a 6dp rhythm, and resize package-row
  paddings/action buttons to the same scale family.
- Tradeoff: rows become a touch larger, but the screen should read as one
  consistent card system.
- Verification: layout XML updated; rebuild/visual verification still pending.
- Next step: rebuild or reopen the installed screen and check whether the new
  size rhythm finally matches the intended `Contents` surface.
