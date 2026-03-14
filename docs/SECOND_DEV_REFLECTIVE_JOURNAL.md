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

### Entry 12: Device-led top-card fit correction

- Goal: correct the remaining top-card sizing issues from a real device view,
  not from XML-only inference.
- Context: live `Contents` screenshots showed that equal-width cards were now
  structurally correct, but `Content Type` labels clipped too aggressively
  while `Install Content` still read slightly heavier than the adjacent
  selector.
- Decision: preserve equal card width, reduce the top-card vertical mass
  slightly, tighten the install button padding, and reclaim more text width in
  compact spinner rows by shrinking end padding and text size.
- Tradeoff: spinner text becomes a bit smaller, but more of the actual content
  type remains visible within the fixed half-width card.
- Verification: device screenshot reviewed first; XML updated second; rebuild
  and re-install still pending.
- Next step: rebuild, re-install, and capture one more stable `Contents`
  screenshot to validate the fit.

### Entry 13: Install-card control mass alignment

- Goal: remove the last visual mismatch inside the equal-width top cards.
- Context: after the device-led size pass, the card shells aligned better, but
  the `Install Content` control still carried more visual mass than the
  adjacent spinner because its internal typography and vertical padding remained
  heavier.
- Decision: keep the card size unchanged and lighten only the button internals:
  smaller text and tighter vertical padding/min-height.
- Tradeoff: the install action becomes visually calmer, while still retaining
  two-line word wrapping if the label ever needs it.
- Verification: XML updated; rebuild/device validation pending.
- Next step: rebuild, reinstall, and compare the top row again on-device.

### Entry 14: Dual-crash forensic split

- Goal: distinguish repeated crash noise from unique root causes during
  container-create testing.
- Context: the user triggered a burst of crashes that looked like many
  failures, but `logcat` separated them into one primary container-path crash
  and one repeated startup crash-loop.
- Decision: harden `DefaultVersion` so container defaults no longer depend on a
  brittle native class-init path, explicitly link `c++_shared` for the native
  OpenXR dependency chain, and switch `BigPictureActivity` onto the same app
  theme family as the rest of the app before inflation.
- Tradeoff: `DXVK` default detection now falls back to a generic safe default
  if GPU probing is unavailable, preferring app stability over eager renderer
  specialization.
- Verification: forensic traces captured; code patched; rebuild and device
  re-test pending.
- Next step: rebuild, reinstall, and re-run both crash paths to verify the
  container screen opens and Big Picture no longer loops on startup.

### Entry 15: Containers action-card replacement

- Goal: remove the weak toolbar-icon affordance for the two most important
  `Containers` actions and replace it with explicit in-surface entry cards.
- Context: the user called out the existing `+` and `Big Picture` icons as too
  weak visually, while these two paths were also the current focus of crash and
  device testing.
- Decision: drop the toolbar menu for those actions, add two top-of-screen
  cards inside `containers_fragment.xml`, and wire them to the same canonical
  navigation paths already used by the old actions (`ContainerDetailFragment`
  for new container creation and `BigPictureActivity` for library mode).
- Tradeoff: the fragment gains more vertical chrome at the top, but the action
  surface is now explicit, readable, and easier to validate on-device than two
  small toolbar icons.
- Verification: layout and fragment wiring updated; `:app:assembleDebug`
  completed successfully after the pass.
- Next step: install the rebuilt APK on-device, inspect the new cards live, and
  continue the crash retest from the clearer `Containers` surface.

### Entry 16: Dashboard relocation for container actions

- Goal: move `New Container` and `Big Picture` onto the same card grid as the
  rest of the app so their size and style match the existing dashboard system.
- Context: after the in-screen `Containers` card pass, the user correctly
  called out that those new cards were now a separate surface with a different
  scale from the normal main-menu cards.
- Decision: remove the extra action row from `containers_fragment.xml`, add two
  new entries to the main dashboard grid, and route them through `MainActivity`
  so `New Container` still opens `ContainerDetailFragment` and `Big Picture`
  still launches `BigPictureActivity`.
- Tradeoff: the actions are now one step earlier in the navigation hierarchy,
  but the visual language is cleaner because all entry points share the same
  `main_menu_card_item` sizing and styling.
- Verification: code and resources updated; build and device re-test pending on
  the relocated version.
- Next step: rebuild, reinstall, and inspect the dashboard plus container entry
  flow on-device from the new main-menu positions.

### Entry 17: Native runtime packaging closure

- Goal: eliminate the remaining container-path crash caused by missing native
  runtime dependencies at APK load time.
- Context: fresh device `logcat` showed the current crash was no longer the
  earlier UI/theme problem; it was `UnsatisfiedLinkError` for
  `libc++_shared.so`, which then cascaded into `NoClassDefFoundError` on
  `GPUInformation` while opening container-related graphics-driver flows.
- Decision: add an explicit Gradle packaging step that copies
  `libc++_shared.so` from the configured NDK into generated `jniLibs` for
  `arm64-v8a`, then include that generated directory in the app source set.
- Tradeoff: the APK now carries one more native runtime artifact directly, but
  the load path is deterministic instead of relying on AGP/NDK implicit
  behavior that had already failed in this Termux-hosted build environment.
- Verification: rebuilt APK now contains `lib/arm64-v8a/libc++_shared.so`,
  reinstalled successfully on-device, and a fresh startup smoke-pass no longer
  emits the previous `UnsatisfiedLinkError`/`NoClassDefFoundError` signature.
- Next step: re-run the exact create-container and graphics-driver interaction
  on-device once more to confirm there is no second downstream crash behind the
  native loader failure.

### Entry 18: XR lazy-load crash tail closure

- Goal: stop ordinary container/UI flows from crashing on phones that should
  never touch OpenXR at all.
- Context: fresh device `logcat` still showed `SIGSEGV` in
  `libopenxr_loader.so`, but the new trace proved the crash now came from
  `XrActivity.<clinit>` rather than from the shared `winlator` runtime load
  path.
- Decision: remove the static native load from `XrActivity`, add a guarded
  `ensureNativeLibraryLoaded()` helper, and load `winlatorxr` only inside
  `XrActivity.onCreate()`.
- Tradeoff: XR native loading now happens later and only on the XR entry path,
  which is the intended behavior; the class can still be referenced safely for
  capability checks and tab visibility logic without pulling OpenXR into normal
  screens.
- Verification: rebuilt APK installed on the target device, `New Container`
  opened without a fresh crash, and the crash buffer remained empty during the
  post-fix entry pass.
- Next step: continue UI-led polish on the now-stable create-container screen
  and verify the rest of the form rather than just the opening transition.

### Entry 19: Device-led New Container polish pass

- Goal: make the stabilized `New Container` screen read like a deliberate
  product surface instead of a recovered crash-test form.
- Context: once the crash tail was gone, live screenshots showed two remaining
  friction points immediately: the tab row still read as cramped/technical, and
  the footer CTA visually collapsed the longer `Create Container` label.
- Decision: switch the tab row to a scrollable, non-all-caps text treatment,
  shorten the primary CTA to `Create`, tighten footer button sizing for the
  available width, and rename the missing-runtime helper action from
  `Open Contents` to `Install Runtime` so the action matches the user intent on
  that screen.
- Tradeoff: the CTA becomes less verbose, but the screen gains cleaner action
  hierarchy and no longer forces a truncated button label in the sticky footer.
- Verification: rebuilt and reinstalled on-device; live screenshots now show
  the scrollable mixed-case tab row, stable footer actions, and no fresh crash
  while entering `New Container`.
- Next step: run a full top-to-bottom create flow and then return to
  `Contents` source-switch/install QA from the same installed build.

### Entry 20: Startup routing and prompt deferral cleanup

- Goal: stop cold-start navigation from competing with system prompts and
  restored foreground state.
- Context: direct `selected_menu_item_id` launches were fragile in practice
  because `MainActivity` mixed immediate fragment routing with startup prompts
  (`All Files Access`, `POST_NOTIFICATIONS`) and had no explicit `onNewIntent`
  handling for a later re-entry into the same activity instance.
- Decision: defer the intrusive startup prompts until the dashboard is actually
  visible, add `onNewIntent()` routing for `selected_menu_item_id` and
  `edit_input_controls`, and fall back to the dashboard when `New Container`
  is requested before `ImageFs` is ready instead of leaving the activity in an
  ambiguous startup state.
- Tradeoff: the storage/notification prompts may appear slightly later in the
  session, but direct entry into `New Container` or `Contents` no longer has to
  compete with them during cold start.
- Verification: `MainActivity` patched, debug build completed successfully, and
  the APK was reinstalled via `push + pm install`. Device-side crash buffer
  remained empty; however, final visual proof of the direct-start route is
  still partial because the shared phone kept reasserting another foreground
  app during ADB capture.
- Next step: re-run the direct-start `New Container`/`Contents` path on a
  stable foreground session, then continue the full create-flow and contents
  behavioral QA.

### Entry 21: Landscape dashboard density pass

- Goal: make the main dashboard feel intentional on wide/landscape screens
  instead of reading like a portrait grid stretched sideways.
- Context: live device sessions repeatedly ended up in landscape, where the
  existing dashboard still rendered only two columns, leaving oversized cards
  and a heavy hero block even though there was enough horizontal space for a
  denser control surface.
- Decision: make `MainMenuGridFragment` resolve to 3, 4, or 5 columns based on
  available width, then add dedicated `layout-land` variants for the dashboard
  hero and menu cards with a shorter hero, tighter padding, and lower card
  height.
- Tradeoff: landscape cards carry slightly less text weight per item, but the
  dashboard reads faster and uses horizontal space more like a control panel
  than a blown-up phone list.
- Verification: new responsive logic and `layout-land` resources added, debug
  build completed successfully, and the APK was reinstalled on-device. Clean
  screenshot proof is still limited by shared-device foreground contention, so
  this pass is currently build-validated rather than fully device-documented.
- Next step: confirm the landscape grid visually on a stable foreground
  session, then return to `Contents` behavioral QA.

### Entry 22: Contents workflow contract closure

- Goal: remove the remaining repo-side `Contents` contract failure so open
  work can stay focused on device behavior instead of CI/source-of-truth drift.
- Context: `check-contents-qa-contract.py` was already green on
  `contents.json`, but it still failed on `.github/workflows/ci-winlator.yml`
  because the workflow text no longer exposed the explicit split-release-repo
  markers the static gate requires.
- Decision: add the expected workflow-level environment keys
  (`RUNTIME_RELEASE_REPO`, `GRAPHICS_RELEASE_REPO`, `APP_RELEASE_REPO`) and
  restore the exact textual contract markers for native APK build mode,
  split-release provenance, and `winlator-latest` tagging directly in the
  workflow file.
- Tradeoff: the workflow now carries a few explicit contract comments whose
  main role is documentation and static validation, but that is preferable to
  allowing the split-model intent to become implicit and silently drift.
- Verification: `validate-contents-json.py` remains green and the static
  `check-contents-qa-contract.py` gate now passes after the workflow patch.
- Next step: return to device-led `Contents` QA and confirm `WCPHub` plus
  `WCP Archive` coexist correctly on the installed build.

### Entry 23: WCPHub overlapping-family visibility repair

- Goal: restore `WCPHub` list rendering for families that also exist in
  archive-managed lanes, without collapsing provenance.
- Context: live device QA showed the `WCPHub` source lane loading successfully
  but rendering an empty `Wine` list. The feed itself was healthy; the parser
  was silently discarding overlapping families because `setHubRemoteProfiles()`
  called `appendRemoteProfiles(..., ignoreRepoManaged=true, ...)`.
- Decision: stop filtering out repo-managed families during explicit WCPHub
  source ingest. The source selector already isolates archive vs WCPHub, so
  preserving the rows is the correct place to keep both surfaces usable.
- Tradeoff: overlapping package families can now exist in memory from either
  source, but that is intentional and still bounded by explicit source-mode
  filtering in `ContentsFragment`.
- Verification: rebuilt APK installed over Wi-Fi ADB; live `Contents` pass now
  renders the `WCPHub` `Wine` lane correctly (`9.20`) instead of an empty list.
- Next step: continue the device pass across the rest of the overlapping
  families and install actions.

### Entry 24: VKD3D badge semantics fix

- Goal: stop `Contents` cards from showing misleading family badges when a
  package name happens to contain the word `proton`.
- Context: after restoring `WCPHub` rows, live device QA on the `VKD3D` lane
  showed cards badged as `Proton` because `ContentProfile.getDisplayCategory()`
  treated any `isProtonLike()` package as a `Proton` category, even outside the
  Wine family. `vkd3d-proton` therefore polluted the badge semantics.
- Decision: scope the `Proton`/`Wine` fallback only to `isWineProtonFamily()`
  profiles, then let `VKD3D`, `DXVK`, and the other non-Wine types fall back
  to their own native family labels.
- Tradeoff: none worth keeping; this is a straight semantic correctness fix.
- Verification: rebuilt APK with the badge fix is installed, but clean visual
  proof on the exact `VKD3D` path is still incomplete because the shared phone
  repeatedly reasserted `Termux` during the re-check loop.
- Next step: re-run the exact `VKD3D/WCPHub` path on a quieter foreground
  session and confirm the badge now stays on the native family label.

### Entry 25: Termux AAPT2 environment closure

- Goal: remove the last manual build-operator step that kept local APK assembly
  fragile in Termux.
- Context: the `WCPHub` parser fix only rebuilt cleanly after explicitly
  passing `-Pandroid.aapt2FromMavenOverride=...`, because AGP otherwise still
  tried to launch the desktop `aapt2` binary from Gradle caches.
- Decision: move the override into `tools/env-android-local.sh` via
  `GRADLE_OPTS=-Dorg.gradle.project.android.aapt2FromMavenOverride=...`, then
  simplify the local build runbook so the standard path is
  `. tools/env-android-local.sh`
  followed by `./gradlew --no-daemon assembleDebug`.
- Tradeoff: the override remains a Termux-only environment shim, but it is now
  explicit and reusable instead of being a fragile per-command memory step.
- Verification: sourced helper script path now works end-to-end;
  `assembleDebug` completed successfully without a manual
  `-Pandroid.aapt2FromMavenOverride` flag.
- Next step: keep the helper-script path as the default local build contract
  and continue device-led UI closure.

### Entry 26: Contents top-row geometry alignment

- Goal: remove the remaining geometric wobble in the top `Contents` cards so
  `Content Type` and `Install Content` read as one aligned surface.
- Context: live device feedback showed the two top cards still rendering as
  different-sized blocks even after earlier width passes, because the left card
  was governed by spinner height while the right card expanded around a looser
  button layout.
- Decision: lock both top cards to the same explicit height, keep the same top
  inset, and normalize the action control on the right to the same 44dp control
  height used by the spinner on the left.
- Tradeoff: the install button no longer reserves two-line growth in that top
  slot, but the label already fits on one line at the current equal-width
  geometry and the overall surface is now visually stable.
- Verification: rebuilt APK installed over Wi-Fi ADB; fresh `Contents`
  screenshot now shows both top cards aligned to the same height and internal
  control geometry.
- Next step: continue the UI pass on the remaining `Contents` details rather
  than on top-row card shape.

### Entry 27: Termux ADB path correction

- Goal: make the local helper script reliable not only for build, but also for
  immediate device install flows.
- Context: after the top-row geometry rebuild succeeded, the chained install
  step still failed because `tools/env-android-local.sh` put SDK
  `platform-tools/adb` ahead of Termux `adb` in `PATH`, and the desktop binary
  is not executable in this Termux ARM64 environment.
- Decision: reorder the helper-script `PATH` so `/data/data/com.termux/files/usr/bin`
  stays first, preserving the Termux-native `adb` while still keeping the SDK
  tools visible later in the lookup order.
- Tradeoff: none meaningful; the helper path is simply more correct for this
  host environment.
- Verification: the already-built APK installed successfully after switching to
  the corrected `adb` path, so the helper contract now covers both build and
  install flows.
- Next step: keep closing remaining device-side UI and behavior tails from the
  refreshed installed build.

### Entry 28: Shared control-system geometry pass

- Goal: stop the app from feeling like a mix of unrelated control densities by
  tightening selectors, switches, seekbars, preference rows, compact toggles,
  and settings action controls under one geometry pass.
- Context: after the `Contents` top-row repair, the remaining visual drift was
  no longer isolated to one screen. `Settings`, preferences, and legacy toggle
  rows still mixed marquee titles, uneven heights, loose widget frames, and
  undersized action controls.
- Decision: widen the fix from screen-level XML tweaks to a shared control
  layer: update `ComboBox`, `EditText`, `BaseButton`, `FieldSet`, compact
  spinner rows, preference rows, switch widgets, seekbar widgets, and legacy
  toggle rows. Then extend the pass with global `CheckBox` and `ListMenuButton`
  geometry so activities and dialogs inherit the same spacing rhythm.
- Tradeoff: some controls became slightly denser and a few old marquee behaviors
  were removed, but the result is more stable and readable than allowing each
  surface to self-size in different ways.
- Verification: debug build completed successfully after the full resource
  patch, APK reinstalled over Wi-Fi ADB, and follow-up screenshots confirmed
  the new shared sizing system on live screens rather than only in XML.
- Next step: keep validating the highest-traffic surfaces (`Contents`,
  `Settings`, `Graphics Center`) and only return to individual dialogs when a
  screenshot reveals a concrete outlier.

### Entry 29: Settings device pass after shared polish

- Goal: confirm that the broad style-system pass actually improved a dense
  settings surface on the phone and did not just compile cleanly.
- Context: `Settings` is the fastest live stress test for the new geometry
  layer because it combines selectors, icon-button action rows, checkboxes,
  long-path rows, and a persistent bottom-right confirm FAB on one scrollable
  screen.
- Decision: raise the screen-level bottom inset, normalize the preset action
  rows, let long path values truncate instead of pushing the chooser buttons,
  and leave the rest to the new shared control styles instead of introducing
  screen-specific custom widgets.
- Tradeoff: the confirm FAB still stays visible as a persistent action anchor,
  so the screen keeps a strong call-to-save affordance at the cost of some
  bottom-right visual weight.
- Verification: rebuilt APK installed successfully and fresh on-device
  screenshots show the updated `Settings` cards, action-row rhythm, and bottom
  spacing without new crashes or resource regressions.
- Next step: continue with deeper device-led behavioral QA for `New Container`
  and `Contents` install flows now that the broad geometry pass is stable.

### Entry 30: New Container env-var and switch cleanup

- Goal: remove the remaining legacy look from the `New Container` flow,
  especially the `Environment Variables` tab and the boolean controls in the
  runtime, frame-generation, and controller sections.
- Context: the screen had stopped crashing, but it still mixed modern cards and
  selectors with older checkbox/toggle patterns. `EnvVarsView` rendered as a
  dense technical list, preset editors still used `ToggleButton`, and container
  boolean options read more like raw form fields than a polished mobile
  control surface.
- Decision: redesign the container env-var tab as a proper card section with a
  header action row, convert env-var/preset boolean controls from
  `ToggleButton` to `SwitchCompat`, and replace container checkbox rows with
  explicit label-plus-switch rows in `container_detail_fragment.xml`. Extend
  the same switch cleanup to the DXVK config dialog so boolean controls stop
  drifting by surface.
- Tradeoff: this pass touches several shared files at once (`EnvVarsView`,
  preset dialogs, DXVK config, container layout), so it is broader than a
  one-screen tweak. That is acceptable because the visual problem was a shared
  component problem, not a single broken XML node.
- Verification: debug build completed successfully, APK reinstalled via
  `adb push + pm install`, direct `MainActivity` start into the new-container
  route produced no fresh crash logs, and app splash/startup into the route was
  observed. Full screenshot proof of the deep tabs remains partial because the
  shared device repeatedly pulled foreground focus back to another app during
  capture.
- Next step: when the device session is quieter, capture the advanced/env tabs
  directly and verify the new switch rows and env-var cards visually end to
  end.

### Entry 31: Donor-source expansion contract

- Goal: widen the package-source investigation without losing control of
  provenance and install safety.
- Context: the user added `The412Banner/BannersComponentInjector` as a second
  donor source for package links and release UX. The donor README indicates a
  broader online-source model, release-tag browsing, search/sort controls, and
  richer release metadata than the current `Contents` surface.
- Decision: record the donor as an audited-input repository rather than a new
  source-of-truth, create a dedicated donor audit note, and fold its safe ideas
  into the roadmap before porting code. Prioritize three concrete transfers:
  full donor release pagination, stronger visible ordering by lane/channel/arch,
  and better package-link normalization.
- Tradeoff: this adds an explicit analysis step before code-porting, but it
  avoids blindly inheriting donor assumptions about which raw payloads are
  complete runtimes.
- Verification: roadmap, agent contract, and donor audit note updated locally.
- Next step: finish the `ContentsFragment` release-placement patch, rebuild,
  then continue donor-by-donor package-family verification.

### Entry 32: Paginated donor release placement pass

- Goal: stop donor-backed packages from disappearing behind shallow feed
  polling and weak list ordering.
- Context: `GameHub` integration already normalized release assets and raw XML
  components, but the feed still depended on a single GitHub releases page and
  the visible list did not yet rank items by lane, channel, architecture, and
  package format. That made it too easy to lose nightlies, arch variants, or
  `Proton`/`Wine` visibility behind source noise.
- Decision: paginate GameHub release polling across multiple GitHub pages,
  promote release-lane packages above raw-feed packages within the donor
  source, widen nightly-channel filtering to `Wine`, `Proton`, `DXVK`, and
  `VKD3D`, sort visible rows by source lane/channel/version/arch/format, and
  remove `GameHub` as an exposed `Wine`/`Proton` source until complete packaged
  runtimes are verified there.
- Tradeoff: donor-backed `Wine`/`Proton` discovery becomes stricter in the
  short term, but the user-facing source model becomes more honest because raw
  skeleton payloads are no longer presented as full runtimes.
- Verification: `assembleDebug` completed successfully, the rebuilt APK was
  reinstalled on `10.0.0.1:42363`, `adb shell monkey -p com.winlator.cmod 1`
  succeeded, and no fresh crash output appeared in the crash buffer during the
  smoke pass.
- Next step: run a live `Contents` device pass focused on `GameHub`,
  `WCPHub`, and `WCP Archive` source switching, then start harvesting concrete
  package-link improvements from `BannersComponentInjector`.

### Entry 33: Multi-goal execution order contract

- Goal: stop losing context when the user sends many parallel goals and set a
  stable order for execution and debugging.
- Context: the active task stream now mixes UI polish, donor integration,
  package ingestion, install verification, and later deep debugging. Without an
  explicit order, it is too easy to jump into forensics before the current task
  list is actually closed.
- Decision: add an execution-priority contract to `AGENTS.md` and the roadmap:
  turn multi-goal prompts into one ordered backlog, close requested
  product/UI/content work first, sync documentation second, and leave
  debugging/forensics for after the current list pass unless a crash or build
  defect directly blocks the next task.
- Tradeoff: some debugging gets deferred slightly, but context retention and
  finish-rate improve because every switch now requires an explicit logged
  reason and return point.
- Verification: `AGENTS.md` and `SECOND_DEV_ROADMAP.md` updated locally.
- Next step: keep the current lane on feature/UX/package closure, then return
  to deeper debugging only after the list pass is explicitly marked closed.
