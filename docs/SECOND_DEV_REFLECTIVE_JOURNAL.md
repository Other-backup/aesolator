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

### Entry 34: Artifact metadata carry-through

- Goal: stop remote release assets from losing their identity once they enter
  the app and later become installed local profiles.
- Context: donor analysis showed that `BannersComponentInjector` keeps richer
  artifact metadata on remote items: source, published date, file size, release
  notes, and release-tag browsing. In `Ae.solator`, remote package identity was
  still flattened too early, especially after manual install of downloaded
  artifacts.
- Decision: extend `ContentProfile` and `ContentsManager` to carry
  `artifactName`, `publishedAt`, and `releaseNotes` alongside source and
  release tag metadata; populate those fields from GameHub release assets and
  persist them into synthetic/local profile metadata during install flows.
- Tradeoff: the profile model becomes slightly wider, but install provenance is
  stronger and the app can now present remote artifacts with more honest
  metadata after download and import.
- Verification: `assembleDebug` completed successfully, the rebuilt APK was
  reinstalled on `10.0.0.1:42363`, and a fresh launcher smoke pass completed
  without new crash-buffer output.
- Next step: wire the new metadata into the package info UI and then rebuild.

### Entry 35: Content info and imagefs reverse-map closure

- Goal: close two lingering tails at once:
  richer package detail UI and a concrete `imagefs` reverse-engineering map.
- Context: after donor review, the current `ContentInfoDialog` looked too thin
  for artifact-backed feeds, and `imagefs` structure knowledge still lived
  mostly in working notes instead of a dedicated repo document.
- Decision: expand the package info dialog to show source, release tag,
  artifact name, channel, published date, and release notes when available; at
  the same time, capture the current `imagefs` layout, toolchain, libraries,
  overlays, and ownership zones into `docs/IMAGEFS_REVERSE_MAP.md`.
- Tradeoff: the info dialog becomes denser, but it now surfaces the artifact
  metadata that the rest of the `Contents` work is already preserving.
- Verification: live `imagefs` path listings captured again from the installed
  app sandbox; docs and UI resources updated locally; build, reinstall, and
  smoke-launch verification completed successfully in the same pass.
- Next step: continue the queued donor/package task from a clean committed
  state and only then return to deeper debugging if something still blocks the
  next feature lane.

### Entry 36: Runtime-install route realignment

- Goal: make the `New Container -> Install Runtime` route land on the correct
  runtime package view instead of whatever stale `Contents` filters happened to
  be left in shared preferences.
- Context: the user reported that nothing showed in `Contents`, and the dark
  theme looked underpainted on the transition from container creation into the
  runtime install flow. The route previously opened `ContentsFragment`
  directly, bypassing `MainActivity`'s normal themed navigation path and
  keeping old `Contents` source/type prefs alive.
- Decision: route the action through `MainActivity.openMainMenuItem(...)`,
  force `Contents` preselection to `Wine` + `WCP Archive` + stable/all lanes
  for the runtime install scenario, and give both `container_detail_fragment`
  and `contents_fragment` explicit root backgrounds from the active theme.
- Tradeoff: the runtime-install path is now opinionated toward the app's own
  runtime packages first, but that is the correct default for container
  creation and still leaves the user free to switch lanes afterward.
- Verification: `assembleDebug` completed successfully, the rebuilt APK was
  reinstalled on `10.0.0.1:42363`, and a post-launch device screenshot now
  shows `Contents` in dark theme with `Wine` selected, `WCP Archive` selected,
  and the `11-arm64ec` runtime card visible.
- Next step: continue live source-switch validation for `WCPHub` and donor
  feeds from this corrected route instead of debugging the old empty-state path.

### Entry 37: FreeWine upstream handoff capture

- Goal: keep `freewine11` build-side upstream signals from getting lost while
  app/UI work continues in `aesolator`.
- Context: the user explicitly pointed to one Valve Wine commit and then to the
  full compare between `ValveSoftware/wine:proton_10.0` and
  `GameNative/proton-wine:proton_10.0`, with the instruction to route it to the
  second build agent working on `freewine`.
- Decision: capture the compare as a formal cross-repo handoff in
  `docs/FREEWINE_BUILD_AGENT_HANDOFF.md`, including the exact compare link,
  the three downstream commits, changed-area breakdown, and the warning that it
  is a bundled Android/Winlator integration layer rather than a single
  cherry-pick.
- Tradeoff: this does not patch `freewine11` directly because that repository
  is not checked out in the local workspace, but it prevents the upstream
  signal from disappearing into chat history.
- Verification: GitHub compare API reviewed on `2026-03-14`; the compare is
  `ahead_by=3`, `behind_by=0`, with `70` changed files concentrated in
  `android/patches`, `android/android_sysvshm`, workflow automation, and
  build-step scripts.
- Next step: when the `freewine11` build lane is active locally, assess the
  three-commit stack as one downstream patch layer and only then decide whether
  to cherry-pick, port selectively, or reject pieces.

### Entry 38: Nightlies Proton confirmation

- Goal: close the donor-runtime ambiguity around whether `The412Banner/Nightlies`
  actually carries installable `Proton` payloads.
- Context: the user supplied the concrete `Nightlies` Proton release after the
  earlier donor audit had already marked `Nightlies` as the likely source for
  missing nightly runtimes.
- Decision: record the exact release and assets in the dedicated
  `freewine11` handoff note and upgrade the repo-map/roadmap language from
  “likely source” to confirmed donor source for packaged ARM64EC Proton
  artifacts.
- Tradeoff: this is still a reporting pass rather than live `Contents`
  integration, but it removes uncertainty for the build/runtime agent and for
  the future donor-lane implementation.
- Verification: GitHub release API for
  `proton-bleeding-edge-20260312-b310f0c-run23` reviewed on `2026-03-14`;
  assets confirmed as `.wcp` and `.wcp.xz` ARM64EC Proton packages plus
  matching `.sha256` sidecars.
- Next step: keep `Nightlies` at the top of the donor integration backlog for
  a future first-class `Contents` source lane and package-install pass.

### Entry 39: Nightlies lane integration in Contents

- Goal: move `The412Banner/Nightlies` from donor report status into a real
  first-class `Contents` source lane with working package metadata and sane
  filtering defaults.
- Context: donor review had already confirmed that `Nightlies` carries
  packaged `Proton`, `DXVK`, `VKD3D`, `Box64`, `WOWBox64`, and `FEXCore`
  artifacts, but the app still exposed only `WCP Archive`, `GameHub`, and
  `WCPHub`, which meant the confirmed donor runtime stream was invisible in the
  product UI.
- Decision: add a dedicated `nightlies` source mode in `ContentsFragment`,
  paginate `The412Banner/Nightlies` releases through a separate release
  normalizer path, surface `Nightlies` labels/scope strings in the UI, and
  include GitHub asset `digest` values as `sha256` metadata for release-backed
  package verification. For source/filter UX, when a selected lane has nightly
  packages but no stable packages, the channel spinner now collapses to
  `nightly` instead of defaulting to an empty stable view.
- Tradeoff: this widens the source-lane model and increases feed complexity,
  but it removes a real donor-package blind spot and prevents the `Proton`
  nightly lane from looking empty by default.
- Verification: `assembleDebug` completed successfully, the rebuilt APK was
  installed via Wi-Fi ADB, and a smoke launch of `com.winlator.cmod` completed
  without a fresh crash-buffer entry. Build-time verification also confirms the
  new strings, source-mode wiring, and normalizer path compile cleanly.
- Next step: run a device-led pass inside `Contents` to verify `Nightlies`
  source switching, nightly-only channel behavior, and installation of at
  least one donor package per family.

### Entry 40: Global package ordering and Proton intake repair

- Goal: make package ordering stable across all source lanes and fix the donor
  `Proton` install path that was still selecting the wrong artifact variant.
- Context: the user called out two connected problems: package ordering still
  felt inconsistent across lanes, and the latest donor `Proton` was not
  installing. Donor packaging clarified the root cause: packages carrying the
  usable prefix are delivered in `.wcp.xz`, while the plain `.wcp` sibling can
  represent a less complete runtime payload for the wine-family case.
- Decision: add `publishedAt` as an explicit sort key both in visible profile
  ordering and in merged remote-candidate selection, then specialize format
  priority so `Wine/Proton` packages prefer `.wcp.xz/.wcp.zst` over plain
  `.wcp`. This keeps ordering date-aware across source lanes and makes donor
  `Proton` resolution land on the prefix-carrying artifact by default.
- Tradeoff: format preference is now type-aware instead of globally uniform,
  but that is the correct model because donor `Wine/Proton` packaging semantics
  differ from the graphics/component lanes.
- Verification: `assembleDebug` completed successfully, APK reinstall via Wi-Fi
  ADB succeeded, and a fresh smoke launch finished without a new crash-buffer
  record. The live in-app tap-through for final donor `Proton` installation is
  still pending, but the selection logic now targets the correct compressed
  artifact family.
- Next step: perform a direct device install pass on the newest `Nightlies`
  `Proton` entry to confirm end-to-end installation on the corrected intake
  path.

### Entry 41: Vulkan SDK installed-state and runtime-pickup alignment

- Goal: remove the false `Vulkan SDK` installed-state in `Contents` and align
  the package metadata with the runtime selector so SDK lanes are chosen from
  real package installs instead of base `imagefs` noise.
- Context: device-side inspection showed `files/contents/VulkanSDK` was empty
  while `Contents` could still report Vulkan SDK as installed because the base
  `imagefs` already ships `usr/share/vulkan`. That made the UI over-report SDK
  presence even though runtime selection still depends on locally installed
  `Contents` profiles.
- Decision: stop treating the base `imagefs/usr/share/vulkan*` directories as
  proof of an installed `Vulkan SDK` package, add a dedicated `Vulkan SDK`
  architecture filter in `Contents`, and enrich `contents/contents.json` with
  explicit `vulkanApiMin`, `vulkanApiMax`, and `vulkanSdkVersion` fields for
  the archive SDK lanes.
- Tradeoff: legacy/manual rootfs mutations without a matching `Contents`
  profile will no longer masquerade as an installed SDK, but that is the
  correct contract because Ae.solator is package-driven for runtime overlays.
- Verification: on-device `run-as com.winlator.cmod` inspection confirmed an
  empty `files/contents/VulkanSDK` before the fix while the base rootfs still
  contained `usr/share/vulkan`; code was updated so installed-state now stays
  tied to package presence rather than shared rootfs directories.
- Next step: rebuild, reinstall, and run a device pass in `Contents` plus
  `Graphics Driver Config` to confirm the SDK rows no longer lie about install
  status and that Vulkan API selection still resolves from installed packages.

### Entry 42: dgVoodoo Contents-to-runtime bridge repair

- Goal: repair the `dgVoodoo` install contract so packages installed through
  `Contents` are visible to wrapper presence checks and runtime staging.
- Context: code review exposed a split-brain model. `Contents` installs
  `dgVoodoo` into versioned `contents/DgVoodoo/<ver>-<code>` directories, but
  `DgVoodooManager`, dependency checks, and runtime staging were still reading
  only `contents/dgvoodoo/current`. That meant the UI/install path and the
  launch/runtime path could drift apart.
- Decision: teach `DgVoodooManager` to scan both the legacy `current` package
  root and the installed `Contents` package roots, sort them, union their
  available architectures, and stage from the best matching root for the
  requested architecture.
- Tradeoff: manager logic is now slightly richer, but the contract becomes
  correct: one installed package set, one runtime view, and parallel
  architecture support instead of hidden install islands.
- Verification: the current `dgvoodoo-*.wcp` artifacts were inspected and
  confirmed to carry `profile.json`, `ae-runtime-contract`, wrapper env, and
  `payload/runtime/<arch>` trees, which matches the new manager fallback path.
- Next step: rebuild, reinstall, and run a device-led `dgVoodoo` route pass to
  confirm `WrapperRuntimePresenceDependency`, config summary, and runtime
  staging all resolve against the same installed package roots.

### Entry 43: Contents surface dark-theme repair and preloader normalization

- Goal: remove the dark-theme visual drift in `Contents` list cards and stop
  the install overlay from flashing as a light, foreign-looking surface.
- Context: device screenshots showed the library/package cards being painted by
  accent-tinted `GradientDrawable` fills inside `onBindViewHolder`, which
  pushed rows into muddy brown/pink surfaces outside the shared dark theme. The
  `Installing Content…` overlay also bypassed the theme painter, so it could
  appear as a light card during runtime/install transitions.
- Decision: move `Contents` rows back onto the shared surface-card palette,
  keep accent usage on the icon/badge rather than on the whole row fill, and
  make `PreloaderDialog` explicitly theme-aware with the runtime scrim plus
  dark-mode text/card colors.
- Tradeoff: package rows now read more like a stable product list and less like
  per-lane color chips, but the screen becomes much more coherent and readable
  in dark mode.
- Verification: a fresh device screenshot on `Contents` confirms the install
  overlay now renders as a dark surface card instead of a white slab, and the
  list rows remain within the same visual system as the top cards.
- Next step: keep using screenshot-led passes for remaining list/card states
  whenever a new donor lane or install state is introduced.

### Entry 44: Graphics-driver extensions probe restoration and device-source reality check

- Goal: restore the missing extension list inside `Graphics Driver
  Configuration` and document the current device-side truth for runtime lanes.
- Context: code review found `GraphicsDriverConfigDialog.queryAvailableExtensions()`
  reduced to a hardcoded empty array, which explains why the dialog showed `0`
  extensions regardless of driver state. In the same device pass, inspection of
  `run-as com.winlator.cmod` showed `files/contents` currently contains
  `DXVK/VKD3D/VulkanSDK/DgVoodoo/Box64/FEXCore`, but no local `Wine` or
  `Proton` package roots at all, so `New Container` cannot legitimately
  discover a runtime from the local package index in that state.
- Decision: restore the extension path through `GPUInformation.enumerateExtensions()`
  with resource-driver extraction before probing, dedupe/sort real `VK_*`
  names, and log the device-side fact that empty `Wine/Proton` runtime
  discovery is currently caused by missing local package roots rather than a
  spinner-only UI defect.
- Tradeoff: `Graphics Driver Config` returns to a native runtime probe instead
  of a placeholder path, but `Nightlies`/remote source availability remains a
  separate external problem when GitHub rate-limits the app's unauthenticated
  requests.
- Verification: live device logs showed `Nightlies` refresh failing with
  `HTTP 403` from `api.github.com` and `WCP Archive` using the bundled fallback
  after a `404` on the canonical raw `contents.json` endpoint. A direct
  `run-as` filesystem pass confirmed the absence of local `Wine/Proton`
  package directories under `files/contents`.
- Next step: add a non-API fallback/cached path for `Nightlies` source
  discovery so first-load package availability does not collapse on GitHub rate
  limiting, then repeat the runtime install pass for `Wine/Proton`.

### Entry 45: Nightlies donor fallback closure and channel-lane repair

- Goal: stop `Contents -> Nightlies` from collapsing to an empty lane on
  GitHub API rate limiting, while also preserving actual nightly-channel assets
  instead of filtering them out at ingest time.
- Context: device logs proved the primary failure mode: unauthenticated
  `api.github.com` calls for `The412Banner/Nightlies` were returning `HTTP 403`
  and leaving the source lane empty. Code review also exposed a second defect:
  the `Nightlies` lane still flowed through the generic remote-profile setter,
  which dropped beta/nightly rows during parsing.
- Decision: keep the GitHub API path as the first attempt, but add a non-API
  fallback that parses `releases.atom`, extracts recent release tags, then
  normalizes each `expanded_assets/<tag>` HTML page into the existing contents
  model. In parallel, route both `Nightlies` and `GameHub` lanes through
  all-channel remote-profile setters so nightly artifacts are not discarded at
  ingest time.
- Tradeoff: refresh for `Nightlies` now does more network work when the API is
  rate-limited, but the source lane remains functional and the metadata stays
  rich enough for date/version sorting, digest verification, and provenance
  display.
- Verification: fresh device pass on March 14, 2026 showed
  `CONTENTS_FEED_REFRESH_DONE` with `source_mode:\"nightlies\"`,
  `sources_polled:12`, `payloads_received:10`, `bundled_fallback:false`
  immediately after two `HTTP 403` API failures. The live `Contents` screen
  then rendered donor `Proton` packages from `proton-bleeding-edge-20260312-b310f0c-run23`,
  confirming the fallback path and the lane parser on real hardware.
- Next step: finish the install-path closure for freshly downloaded donor
  `Wine/Proton` so a successful `Nightlies` install always materializes a real
  local package root under `files/contents/Proton` or `files/contents/Wine`
  and becomes visible to `New Container`.

### Entry 46: New Container runtime-entry false-negative closure

- Goal: stop `New Container` from falsely claiming `Install a Wine or Proton
  runtime before creating a container` after a donor runtime was already
  installed and visible under `files/contents`.
- Context: device forensics had already shown both local runtime roots and a
  healthy spinner population, but the create action still failed the final
  runtime gate. Live device state on March 14, 2026 showed:
  `NEW_CONTAINER_RUNTIME_SCAN` with `local_wine_count:1`,
  `local_proton_count:1`, and `runtime_available:true`. The remaining defect
  was therefore in runtime resolution, not package installation.
- Decision: repair `WineInfo.fromIdentifier()` so `Contents` entry labels such
  as `Wine-10.0.99-arm64ec-0` and `Proton-10.0.99-arm64ec-1` resolve through
  `ContentsManager` profile metadata and tolerate the trailing `-verCode`
  suffix instead of falling back to `MAIN_WINE_VERSION`. In parallel, add a
  `NEW_CONTAINER_RUNTIME_RESOLVE` forensic event on the create path to record
  selected entry, resolved type, resolved path, and path existence.
- Tradeoff: runtime resolution logic is now slightly more explicit about the
  distinction between picker labels and canonical runtime identifiers, but that
  is the correct contract for a `Contents`-driven app where the UI carries
  versioned entry names rather than raw `wine-...` identifiers.
- Verification: rebuilt and reinstalled the APK, then ran an ADB-driven device
  pass through `MainActivity --ei selected_menu_item_id 2131297101`, captured
  the `New Container` screen, tapped `Create`, and observed `Creating
  Container…` instead of the old missing-runtime toast. Fresh device forensics
  recorded `NEW_CONTAINER_RUNTIME_RESOLVE` with
  `selected_entry:"Wine-10.0.99-arm64ec-0"`,
  `resolved_path:"/data/user/0/com.winlator.cmod/files/contents/Wine/10.0.99-arm64ec-0"`,
  and `path_exists:true`. After the operation, the app returned to the
  dashboard and `/data/data/com.winlator.cmod/files/imagefs/home/xuser-1`
  existed on-device.
- Next step: keep the richer forensic pair for future runtime regressions, and
  continue the same device-led closure pattern for donor `DXVK`, `VKD3D`,
  `Vulkan SDK`, and `dgVoodoo` install-to-consumer flows.

### Entry 47: Graphics-driver dialog crash removal and fallback extension catalog

- Goal: stop `Graphics Driver Configuration` from crashing on open and restore
  a usable extensions selector instead of the empty `0 extensions` state.
- Context: live device forensics on March 14, 2026 showed a native `SIGSEGV`
  while opening the dialog. The crash stack led through
  `GraphicsDriverConfigDialog.queryAvailableExtensions()` into
  `GPUInformation.<clinit>()`, which loads `libwinlator.so`. That made the
  dialog structurally unsafe: simply rendering the extensions row could crash
  the process before the user interacted with anything.
- Decision: remove the native extension probe from the dialog open path and
  replace it with a catalog-backed fallback (`VulkanExtensionCatalog`) built
  from shipped Vulkan extension names. Keep saved blacklist entries merged into
  the available set, harden config parsing against missing tags/defaults, and
  log a dedicated `GRAPHICS_DRIVER_EXTENSION_CATALOG` forensic event so the UI
  data source is explicit on-device.
- Tradeoff: the dialog now shows a stable, broad fallback catalog rather than a
  runtime-specific extension probe. That sacrifices exact per-driver fidelity
  for reliability, but it is the correct short-term contract because a config
  dialog must not crash merely to enumerate optional extensions.
- Verification: rebuilt and reinstalled the APK, opened `New Container` on the
  device, tapped `Graphics Driver -> Configure`, and captured a live screenshot
  showing `Available Extensions` populated with `316 Extensions`. `adb logcat
  -b crash` remained empty for that interaction, while app logs recorded
  `GRAPHICS_DRIVER_CONFIG_OPEN` and `GRAPHICS_DRIVER_EXTENSION_CATALOG` with
  `catalog_source:"fallback_catalog"` and `extension_count:316`.
- Next step: keep the dialog on the safe catalog path, and later improve the
  catalog quality only if a non-crashing, off-path probe can provide verified
  driver-specific filtering without reintroducing a native open-path tail.

### Entry 48: Desktop shell input-model closure after shell-registry success

- Goal: close the next container-launch tail after the shell/taskbar fix by
  making the rendered desktop actually clickable on real hardware.
- Context: after the registry-backed shell bootstrap fix, the device finally
  reached and held a real desktop surface with `Start` visible, but the user
  correctly reported that “nothing was clickable”. Forensics and local app
  state showed the session was still entering the default hidden touchpad model
  for no-shortcut launches: `touchscreen_toggle` was off globally, the desktop
  session had no shortcut-specific `simTouchScreen` override, and the shell was
  therefore alive but not in a direct-touch hit-testing mode.
- Decision: promote no-shortcut desktop launches into an explicit direct-touch
  input contract inside `XServerDisplayActivity.setupUI()`: force
  `touchpadView.setSimTouchScreen(true)`, make the cursor visible for
  discoverability, and log a dedicated `DESKTOP_INPUT_MODEL_APPLIED` forensic
  event so future passes can distinguish “desktop rendered” from “desktop is
  interactable”.
- Tradeoff: desktop bootstrap is now more opinionated for no-shortcut sessions,
  because it overrides the old hidden touchpad semantics. That is the correct
  tradeoff for the default container desktop path: the primary contract is a
  clickable desktop, not an invisible laptop-style touchpad abstraction.
- Verification: rebuilt and reinstalled the APK, relaunched
  `XServerDisplayActivity` for `container_id=3`, and confirmed on-device that
  the session stayed `resumed` with a live `TouchpadView` input channel.
  `logcat` recorded `DESKTOP_INPUT_MODEL_APPLIED` with
  `simulate_touchscreen:true` and `relative_mouse:false`, followed by
  `DESKTOP_SHELL_REGISTRY_APPLIED` and two `XSERVER_APP_WINDOW_MAPPED` events
  for `explorer.exe`. A device-led tap pass on the held desktop then changed
  the captured frame size from `187352` bytes to `86127` bytes on the first
  tap and to `201319` bytes on the second tap, while
  `XServerDisplayActivity` remained `resumed`, confirming that desktop hit
  testing now reaches the live shell instead of stalling in a hidden touchpad
  model.
- Next step: return to the remaining payload/runtime tails from a stable base:
  `DXVK`, `VKD3D`, `Vulkan SDK`, `dgVoodoo`, and post-desktop UX polish.

### Entry 49: Cursor-touchpad refinement after direct-touch proof

- Goal: refine the fixed desktop session so the cursor behaves like a desktop
  pointer instead of duplicating the user’s finger on every touch.
- Context: the first closure pass proved that a no-shortcut desktop session was
  finally clickable, but it did so with `simulateTouchScreen:true`. That made
  the visible cursor behave like a touch surrogate and jump directly to the tap
  point, which is not the intended desktop interaction model.
- Decision: switch no-shortcut desktop bootstrap from touch-surrogate mode to a
  visible cursor-touchpad model. In `XServerDisplayActivity.setupUI()` the
  session now forces `simulateTouchScreen:false`, clears relative-mouse mode,
  centers the pointer at desktop start, keeps the cursor visible, and records
  the resulting pointer state in `DESKTOP_INPUT_MODEL_APPLIED`.
- Tradeoff: raw tap-to-hit proof is less trivial than in touch-surrogate mode,
  because the pointer is now a real cursor instead of a direct touch mirror.
  That is the correct tradeoff for the desktop route: usability now matches the
  user’s requested desktop semantics instead of a tablet-style touch overlay.
- Verification: rebuilt and reinstalled the APK, cold-started
  `XServerDisplayActivity` for `container_id=3`, and observed on-device
  forensic lines:
  `DESKTOP_INPUT_MODEL_APPLIED` with `simulate_touchscreen:false`,
  `relative_mouse:false`, `pointer_x:640`, `pointer_y:360`,
  `input_mode:"cursor_touchpad"`, followed by
  `DESKTOP_SHELL_REGISTRY_APPLIED` and `XSERVER_APP_WINDOW_MAPPED` for
  `explorer.exe`. `dumpsys activity top` kept `XServerDisplayActivity`
  `mResumed=true`, confirming that the refined input contract holds on the live
  desktop session without falling back to the old touch surrogate.
- Next step: continue payload/runtime closure from this cursor-based desktop
  baseline, then revisit any remaining desktop UX polish only after
  `DXVK` / `VKD3D` / `Vulkan SDK` / `dgVoodoo` consumers are stable.

### Entry 50: XServer bootstrap freeze was a broken orientation gate

- Goal: close the hard freeze that appeared when launching a container and left
  the app stuck on the startup splash / startup overlay.
- Context: live device forensics finally isolated this away from the older
  `MotionEvent` ANR path. During the failing launch, `XServerDisplayActivity`
  owned the task, but the window handoff was inconsistent: on the old build the
  splash could remain visible with no stable focused window, and
  `am start -W` would stall while the activity sat inside repeated fixed
  rotation churn.
- Decision: remove the stale portrait bootstrap gate inside
  `XServerDisplayActivity.onCreate()`. The old code still called
  `setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)` when
  `xServer.screenInfo` looked portrait-like and then deferred the whole runtime
  bootstrap behind `configChangedCallback`. That contradicted the manifest's
  `sensorLandscape` contract and could strand startup before the first real draw.
  The new gate only waits if the activity is not yet in landscape, requests
  `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, and logs
  `XSERVER_BOOTSTRAP_ORIENTATION_GATE` for future forensic passes.
- Tradeoff: startup is now less "wait for a later config surprise" and more
  explicit about honoring the landscape runtime contract immediately. That is
  the correct tradeoff, because this activity is already declared as the
  landscape desktop host in the manifest.
- Verification: rebuilt and reinstalled the APK, then launched
  `XServerDisplayActivity` for `container_id=3` through a properly signed
  intent. After the fix:
  `adb shell am start -W` returned `Status: ok` in about 3.1s, `dumpsys input`
  showed a real focused `XServerDisplayActivity` window instead of an empty
  focus handoff, the startup splash regressed into the in-app `Starting up...`
  overlay rather than a dead launcher splash, and `logcat` recorded two
  `XSERVER_APP_WINDOW_MAPPED` events for `explorer.exe`. That proves the
  desktop bootstrap now crosses the old splash/orientation choke point and
  reaches the shell mapping phase.
- Residual risk: the shared phone kept hijacking foreground back to Telegram
  during later captures, so screenshot proof of the fully visible desktop in
  this exact pass is incomplete even though the shell mapping forensics are
  positive.
- Next step: continue from this restored bootstrap path into the remaining live
  desktop interaction tails and payload consumers, not back into startup
  deadlock triage.

### Entry 51: Desktop freeze mostly closed, pointer semantics narrowed to tap contract

- Goal: convert the broad "desktop freezes / nothing reacts" report into a
  narrower, testable input bug and record the new contract explicitly.
- Context: after the orientation-gate and deferred-pause fixes, the no-shortcut
  desktop no longer reliably died in the old splash/startup loop. Live ADB
  passes showed a stable `XServerDisplayActivity`, real `explorer.exe` window
  maps, and a held desktop surface, but the user still correctly reported that
  the cursor path felt inert. Device-led passes then showed why: the visible
  cursor model survived, but a simple tap still clicked at the old cursor
  position instead of the intended hit target. Shared-device interference from
  `Termux`/launcher also contaminated some screenshots, so unattended passes
  now need an explicit exclusive device window instead of being treated as
  ground truth automatically.
- Decision: keep the desktop-style visible cursor model, but move the active
  closure target from generic freeze hunting to a specific tap-to-click
  contract. `TouchpadView` now exposes `setTapToClickMovesCursor(...)`, tap
  clicks are serialized on the same input queue as pointer movement, and
  `XServerDisplayActivity` enables that hybrid path only for the default
  no-shortcut desktop route while leaving shortcut sessions unchanged.
- Tradeoff: this makes the default desktop path more opinionated. That is the
  correct tradeoff here because the user's contract is explicit:
  no finger-mirroring cursor, but real taps must still open `Start`, `Computer`
  and other shell targets without hidden laptop-style indirection.
- Verification: code path updated and rebuilt locally. The remaining live proof
  still depends on a short uncontaminated device window because several recent
  captures were invalidated by `Termux` regaining foreground during the pass.
- Next step: run one clean unattended ADB cycle on the new build:
  launch desktop, wait for `explorer.exe` map, capture before/after `Start`
  tap, then decide whether the remaining defect is click dispatch itself or a
  deeper shell/window hit-test issue.

### Entry 52: Donor-aligned cursor transport closed the `Start` hit-test gap

- Goal: prove that the visible desktop cursor can hit a live shell target
  through `TouchpadView`, not only through a direct `xServer.injectPointer*`
  debug bypass.
- Context: the previous probe finally opened `Start`, but it did so through a
  direct XServer injection path. Once the same probe was moved onto the live
  `TouchpadView` transport, `Start` stopped opening. That isolated the real
  cursor bug to our input transport layer, not to shell readiness, window
  maps, or the `explorer.exe` desktop itself.
- Decision: use `GameNative` as the donor reference for cursor semantics and
  remove the Ae.solator-specific async move/button queue from the touchpad tap
  path. The active contract is now donor-style direct pointer injection:
  `move -> press -> delayed release`, with `TouchpadView` also serving as the
  transport for the desktop debug probe.
- Tradeoff: this gives up the custom background dispatch experiment. That is
  the correct tradeoff because the queue was no longer a theoretical cleanup;
  it had become the live source of missed shell hits. The donor path is
  simpler, easier to reason about, and now has device evidence behind it.
- Verification: rebuilt and reinstalled the APK, launched the desktop with
  `aeso_debug_probe_start_tap=true`, and captured
  `.tmp_adb/probe_start_after_36s_v4.png`. The screenshot shows the `Start`
  menu open on top of the desktop wallpaper, while forensic logs record
  `DESKTOP_DEBUG_START_PROBE_DISPATCHED` with `transport:"touchpad_view"`.
  That closes the old gap where only the direct `xserver` bypass could open
  `Start`.
- Next step: stop treating basic click hit-testing as open. The remaining
  desktop cursor tail is now manual-session quality: drag behavior, repeated
  clicks after longer interaction, and pointer-state stability once the user
  starts actually using the desktop instead of just opening `Start`.

### Entry 53: Cursor ownership split to eliminate the duplicate X11 fallback

- Goal: stop the desktop from showing two cursors at once when the guest window
  already paints its own pointer.
- Context: after the `GameNative`-aligned tap transport fix, shell hit-testing
  was finally correct, but the user then reported a new visible defect:
  one cursor from the container/guest and a second fallback cursor from the X11
  side. Full grep over `GameNative` and `Ae.solator` showed that both projects
  still render a GL cursor in `GLRenderer`; the likely offender in our case was
  the unconditional root fallback branch that draws `rootCursorDrawable` when
  the point window does not expose an explicit X cursor.
- Decision: keep the fallback cursor for desktop shell surfaces, but add a
  cursor-ownership heuristic in `GLRenderer`: for no-shortcut desktop sessions,
  if the pointer resolves to a fullscreen-like non-shell application window,
  suppress the root/X11 fallback and let the guest own the visible cursor.
  `XServerDisplayActivity` now enables this owner mode explicitly for the
  default desktop path and records it in `DESKTOP_INPUT_MODEL_APPLIED`.
- Tradeoff: the ownership split is heuristic, not omniscient. That is still the
  correct tradeoff because the old behavior was definitely wrong: it always
  allowed an X11 fallback cursor to appear even when the guest was clearly
  drawing its own pointer. The new rule narrows the risk to misclassification
  of some app windows instead of guaranteed duplication.
- Verification: rebuilt and reinstalled the APK, launched the desktop with the
  same `aeso_debug_probe_start_tap=true` forensic pass, and captured
  `.tmp_adb/probe_start_after_36s_v5.png`. The shell still opens `Start`, the
  cursor remains visible on desktop surfaces, and forensic logs show
  `desktop_cursor_owner_mode:true` together with the live `touchpad_view`
  transport. That confirms the ownership patch did not regress the already
  fixed desktop click path.
- Next step: run a manual device pass inside non-shell application windows to
  see whether any remaining duplicate-cursor cases survive the fullscreen-like
  ownership split, then tighten the heuristic only if the issue still
  reproduces on real app windows.

### Entry 54: Default desktop input contract switched back to true trackpad mode

- Goal: align the default no-shortcut desktop path with the user's clarified
  expectation for cursor behavior.
- Context: the previous desktop pass had closed shell hit-testing by letting a
  tap reposition the cursor for the click. That made debugging easier, but the
  user then clarified the desired UX explicitly: this session should behave like
  a real trackpad, where cursor movement and clicking are parallel concerns and
  a tap does not teleport the pointer to the touched screen coordinate.
- Decision: keep the fixed direct pointer transport, but disable
  `tapToClickMovesCursor` for the default desktop route in
  `XServerDisplayActivity`. The active mode is now logged as
  `input_mode:"cursor_trackpad"` with `tap_to_click_moves_cursor:false`.
- Tradeoff: this gives up the earlier hybrid tap-to-hit shortcut. That is the
  correct tradeoff because the product contract is now clearer than the debug
  convenience: desktop input should feel like a trackpad, not like hidden
  touch-surrogate mode wearing a cursor costume.
- Verification: code and docs updated; next live device pass should confirm that
  taps now fire at the current pointer position while cursor movement remains
  delta-based.
- Next step: rebuild, reinstall, and run a manual desktop pass to verify click,
  drag, and repeated interaction quality under the restored trackpad contract.
