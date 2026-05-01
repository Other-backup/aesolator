# Second Developer Reflective Journal

## 2026-04-16

### Entry 8: mandatory systemic auto-fix execution contract

- Goal: promote the user's master engineering directive and enforced repair
  policy from chat memory into durable repository rules.
- Context: the operating method is now explicit law for technical work:
  root-cause first, whole source tree / product surface coverage, no
  advisory-only completion for fixable defects, no local symptom patching, and
  full-harvest log handling before build-log repair.
- Decision: add repo-local `docs/MASTER_ENGINEERING_DIRECTIVE.md`, wire it
  into `AGENTS.md`, `README.md`, `docs/CODEX_OPERATING_CONTRACT.md`, the docs
  index, build/device runbooks, and this roadmap/journal path.
- Tradeoff: process docs get stricter and more repetitive, but new sessions no
  longer depend on recovering these rules from chat history.
- Verification: targeted `rg` coverage and syntax compilation of the shared
  `.codex/rules/master_engineering_directive.rules` file.
- Next step: apply this method to the active FreeWine/Ae.solator repair
  frontier after the current full `make -k` harvest reaches a real stop.

### Entry 9: expanded omega authority law

- Goal: fold the expanded master directive into the durable rule surface so
  future sessions inherit not only root-cause closure, but also the explicit
  ban on advice-only, minimal workaround, local symptom masking, and
  close-one-log behavior.
- Context: the method now explicitly includes execution identity, strict
  passive-mode prohibitions, whole-tree propagation surfaces, search/context
  expansion, contract clarity, stable abstractions, systemic consistency, and
  environment-bound authority.
- Decision: extend the canonical `.codex` directive, the machine-readable
  `.rules`, repo-local master directives, AGENTS entrypoints, and the app
  operating contract rather than relying on chat history.
- Tradeoff: the operating docs are more forceful and redundant by design, but
  that prevents method drift after Termux restarts or repo handoff.
- Verification: rerun targeted rule coverage and `.rules` syntax checks after
  the update.
- Next step: continue respecting the active full-harvest rule for `make -k`;
  do not parse/repair partial live tails before the real stop.

## 2026-03-20

### Entry 7: route-aware runtime dialogs and detached-install proof

- Goal: close the newest user-visible runtime drawer tails without reopening
  another sequence of cosmetic-only patches.
- Context: the freshest marked screenshots showed three concrete failures at
  once:
  runtime `Logs` still looked like a striped table instead of a console,
  `Runtime Profiles` still exposed an illogical generic emulator selector with
  both `Box64` and `FEX` lanes visible together,
  and direct `Prefix Pack` GUI installers could open while lane state stayed
  stuck at `queued`, which encouraged retry loops and made the surface feel
  laggy or broken.
- Decision: convert `LogView` from alternating-row paint to a real console
  surface, mark compact runtime dialogs explicitly so `Runtime Profiles`,
  `Screen Effects`, and `Input Controls` stop inheriting the tall
  wide-scrollable dialog geometry, replace the runtime profile dialog with an
  arch-aware `FEX-only` or `Box64-only` lane editor, and promote detached
  primary-payload installs beyond `queued` as soon as the guest launch is
  proven to start.
- Tradeoff: more Java/XML surgery in one batch and a larger final verification
  surface, but the user-facing behavior is finally aligned with the live
  runtime model instead of with stale generic container settings.
- Verification: local `testDebugUnitTest assembleDebug` passed after the batch.
  Live APK reinstall / screenshot proof is currently blocked only by the
  device-side Wi-Fi ADB endpoint refusing connections on the last supplied
  ports.
- Next step: reconnect ADB to the live device, reinstall once, then capture a
  fresh screenshot/forensic bundle for `Debug`, `Runtime Profiles`,
  `Screen Effects`, `Prefix Pack`, and the final `Task Manager` row slack.

## 2026-03-18

### Entry 6: install really means install, not "fetch and hope"

- Goal: close the user's remaining clean-pass complaints without pretending
  that a cached payload equals a completed install.
- Context: the user was still calling out the same root problems:
  `Prefix Pack` felt like a blind cacher instead of a real installer surface,
  `.NET Framework` gaps were still visible in legacy setup failures,
  and the Android overlay could hide the very installer GUI the user needed to
  interact with.
- Decision: add an explicit official `.NET Framework` lane
  (`3.5 SP1`, `4.0 Full`, `4.8`), tighten zero-byte validation across shell /
  Windows / imagefs cache accounting, teach `Install` to auto-run `Prepare`
  for missing payloads, dismiss the Android dialog before launch, and expose
  direct per-lane `State` / `Logs` actions on the main loader surface.
- Tradeoff: the toolkit asset surface widened again and more batch-script exit
  code handling was needed, but the user-facing contract is finally aligned
  with what the install button promises.
- Verification target: one final build/install/device pass should prove the
  new `r7` toolkit, live `.NET` lane visibility, a real installer launch from
  `Prefix Pack`, and updated state/log roots from a fresh runtime session.
- Next step: finish the batched UI cleanup, compile once, then run the live
  device pass with screenshots and a fresh forensic bundle.

### Entry 5: sectioned loader and task-list root cause closure

- Goal: close the user's newest complaints without pretending that missing
  process rows or x86-only Mono wording were "just styling".
- Context: the user kept stressing three concrete failures:
  `Task Manager` visually lost its process list,
  `Prefix Pack` still read like a vague fetcher instead of a real loader,
  and the managed-runtime story still looked falsely limited to `x86`.
- Decision: fix the actual `Task Manager` layout root cause
  (`wrap_content` tab container plus weighted zero-height list region), convert
  `Prefix Pack` into compact sectioned lanes inspired by `Contents`, expose
  direct cache/log entrypoints, and split managed runtimes into
  `Wine Mono` versus a separate dual-arch Mono Project lane.
- Tradeoff: one more asset/UI pass and a bumped toolkit asset version were
  necessary, but the result is much easier to reason about and no longer hides
  real process or installer state behind theme noise.
- Verification: fresh build/install passed, `TASKMGR_REFRESH` still reports
  `windows_visible=11` and `linux_visible=12`, and `PREFIX_PACK_TOOLKIT_READY`
  now exposes `catalog_entry_count=16` after app-entry with toolkit `r6`.
- Next step: run one explicit Mono prepare/install pass so the new managed
  lane leaves device-side state/log markers in addition to the loader proof.

### Entry 4: Prefix-pack closure exposed transport and staging lies

- Goal: finish the user's clean pass by closing the last false-closure points
  instead of only repainting the UI.
- Context: two defects proved that the previous state was not honestly closed:
  `Prefix Pack` installs could crash the app before launch because
  `WinHandler.exec()` still behaved like a tiny fixed-packet bridge, and the
  device staging helper could silently leave zero-byte files while still making
  the cache look populated.
- Decision: packetize long `WinHandler.exec()` payloads, tighten cache
  validation around non-zero files, switch large device staging to the explicit
  `/data/local/tmp -> run-as cp` bridge with byte-count verification, and keep
  the updated contract written into `AGENTS.md`/roadmap instead of chat only.
- Tradeoff: one more closure build/install cycle was needed, but it removed a
  real crash vector and stopped the toolkit from reporting fake-ready payloads.
- Verification: the full local cache now validates cleanly, the device rootfs
  cache is at `14/14` with `DXSDK_Jun10.exe` staged at `599455936` bytes, and
  the live prefix exposes all 14 files under `C:\AePrefixPack\cache`.
- Next step: capture one fresh dialog-driven install with state/log markers on
  the clean post-install process and verify the final live `Prefix Pack`
  contrast from that fresh instance.

### Entry 3: Visible C-cache install contract and donor-diagnostics preservation

- Goal: close the user's remaining `Prefix Pack` complaints at the contract
  level instead of just repainting the UI.
- Context: the user explicitly called out three persistent problems:
  installers were not visibly landing inside the container,
  the diagnostics surface still felt too thin,
  and donor/rootfs diagnostics such as `TestD3D.exe` / `GPUInfo.exe` looked
  lost or mislinked after runtime patching.
- Decision: move the written contract to a visible
  `Z:\opt\ae\prefix-pack\cache -> C:\AePrefixPack\cache -> C:\AePrefixPack\staging`
  flow, widen the audited catalog with `XNA 3.1`, `OpenAL 1.1`, and the
  optional `DirectX SDK June 2010` tooling lane, and treat disappearing donor
  diagnostics as an overlay-preservation bug rather than as an excuse to
  duplicate payloads.
- Tradeoff: more batch-script and UI plumbing, plus a larger catalog surface,
  but far less ambiguity about where installers live and why legacy graphics
  tools are or are not available.
- Verification target: one final grouped build/install/device pass should prove
  the bumped toolkit version, visible C-cache layout, preserved donor
  diagnostics, and updated Android `Prefix Pack` UI from a fresh live bundle.
- Next step: run the single closure build, reinstall, verify the live rootfs
  toolkit version, reproduce the install flow, and capture the freshest
  post-fix crash/forensic bundle.

### Entry 2: Prefix Pack and legacy-DX clean-pass contract hardened

- Goal: convert the user's ten-point runtime clean-pass feedback into
  executable repo rules instead of leaving it as fragile chat context.
- Context: the user called out the same cluster repeatedly:
  freshest-log-first capture, underdesigned runtime-drawer accents, a confusing
  `Prefix Pack` that looked like an opaque fetcher instead of a real installer,
  missing donor diagnostics exposure, unclear `dgVoodoo` routing, and a need
  for a written roadmap that survives the next agent handoff.
- Decision: tighten both workspace and repo `AGENTS.md`, add an explicit active
  roadmap block, and define `Prefix Pack` as an installer/state/diagnostics
  surface with clear cache/install/log semantics rather than a duplicate
  runtime-tool bucket.
- Tradeoff: more doc rigor and more UI/backend plumbing in one pass, but much
  less ambiguity about ownership, diagnostics discoverability, and what counts
  as closure.
- Verification: `AGENTS.md` now carries freshest-crash-first capture, payload
  ownership boundaries, `Prefix Pack` cache/install/state rules, donor
  diagnostic discoverability, and `dgVoodoo` route-visibility requirements.
- Next step: land the corresponding code and asset changes, rebuild, reinstall,
  and verify the revised `Prefix Pack`/legacy-DX path against a fresh device
  crash bundle.

### Entry 1: Ajay audit narrowed into an ownership boundary

- Goal: use Ajay's broad component inventory without turning `Ae.solator`
  `prefix-pack` into a duplicate payload manager.
- Context: Ajay exposes a much wider app-store/start-menu surface, but the user
  explicitly ruled that anything already pulled as a payload must not be moved
  into `prefix-pack`.
- Decision: classify Ajay into three buckets:
  dedicated `Contents/payload` families that stay separate,
  legitimate prefix-local redistributables,
  and hold items that need more source/license/runtime audit.
- Tradeoff: this slows down blind catalog expansion, but it preserves one owner
  per runtime surface and avoids re-importing proprietary Ajay shell logic.
- Verification: public Ajay README/changelog/wiki plus live device traces and
  the live `C:\AJAY_PREFIX_PRO` / `Ajay_prefix\save_data` tree were audited;
  the written result now lives in `docs/AJAY_PREFIX_COMPONENT_AUDIT.md`.
- Next step: keep `DXVK` / `VKD3D` / `DgVoodoo` / `VulkanSDK` in `Contents`,
  and only widen `prefix-pack` with source-backed redistributables like
  `XNA 4.0`, `PhysX`, or `LAVFilters`.

### Entry 0: Prefix-pack toolkit and audited supply-chain lane

- Goal: stop treating extra Windows runtime installs as ad-hoc cache drops and
  turn them into one reproducible prefix-pack workflow.
- Context: the user asked for a clean final pass, a custom prefix pack, and a
  hard choice between opaque mirror pages and transparent upstream sources.
- Decision: stage a rootfs-visible toolkit under `/opt/ae/prefix-pack`, pin
  `VC++ AIO` to `abbodi1406/vcredist v0.103.0`, pin `Wine Mono 11.0.0` and
  `Wine Gecko 2.47.4` to official WineHQ release directories, promote the
  prefix pack into three aligned surfaces (repo helper, rootfs shell loader,
  Windows loader), and bind every entry to both a direct download URL and a
  source page URL.
- Tradeoff: more manifest and docs discipline is required, and toolkit asset
  changes now must bump the rootfs `VERSION`, but the lane becomes inspectable
  and reproducible instead of depending on opaque mirrors or ad-hoc cache
  dumps.
- Verification: source-backed URLs chosen, `DirectX June 2010` official binary
  URL verified from Termux, repo/rootfs loaders updated, and the next step is a
  fresh APK install plus live device staging/forensic proof.
- Next step: rebuild/install the APK, confirm `PREFIX_PACK_TOOLKIT_READY`
  reports the bumped toolkit version, stage the cache into device `imagefs`,
  and verify the same catalog/loader contract is visible from Windows inside
  the container.

### Entry 5: Runtime UX closure requires table-stable process rows and one-shot prefix installs

- Goal: stop treating runtime UI regressions as "just layout noise" when they
  are actually blocking the user from inspecting processes or launching staged
  installers.
- Context: live user feedback showed the Linux `Task Manager` rows clipping or
  visually disappearing during scroll, while `Prefix Pack` kept replaying the
  previous `.NET Framework` auto-install target and could remain stuck at
  `scheduled` without proof of runtime dispatch.
- Decision: tighten the contract so `Task Manager` behaves like a stable
  left-side Linux telemetry table with fixed headers and non-clipping rows, and
  make `Prefix Pack` consume deferred install targets once, dismiss before
  runtime hand-off, and log explicit dispatch or timeout markers instead of
  hiding behind silent retries.
- Tradeoff: a little more UI and forensic plumbing, but a far clearer closure
  signal for the next pass and less ambiguity about whether installers actually
  launched.
- Verification: next pass must include a fresh APK install, live screenshots of
  both `Task Manager` and `Prefix Pack`, plus a clean forensic bundle proving
  row geometry and runtime dispatch behavior.
- Next step: complete the Linux table refit, patch the one-shot install
  consumption path, rebuild once, then verify on device with screenshots and
  forensic logs.

## 2026-03-14

## 2026-03-16

## 2026-03-17

### Entry 0: Migration bootstrap and main-only cleanup

- Goal: make the move to a new device reproducible instead of relying on shell
  memory and ad-hoc handoff notes.
- Context: the user is moving to another device, while `aesolator` still held a
  large dirty batch and `wcp-runtime-lanes` had already become the owner of the
  host `LLVM 22.1.1` CI lane.
- Decision: keep the host-LLVM owner lane removed from `Ae.solator`, add a
  repo-tracked migration bootstrap doc and host setup script, and treat the
  temporary side branch as debt that must be landed back onto `main`.
- Tradeoff: a little more repo documentation and bootstrap plumbing, but far
  less migration guesswork on the next device.
- Verification: bootstrap doc/script staged locally; next step is to land the
  full dirty batch onto `main` and verify clean worktrees afterward.
- Next step: commit the app batch, fast-forward or merge it into `main`, push,
  and record the exact new resume point for the next device.

### Entry 0: Host LLVM ownership moved out of `Ae.solator`

- Goal: stop treating `Ae.solator` as the owner of the `LLVM 22.1.1` CI lane.
- Context: the user explicitly moved shared compiler builds into
  `wcp-runtime-lanes` and requested a strict `main`-first ownership model.
- Decision: remove the owner-side LLVM workflow/build script from `Ae.solator`,
  keep only consumer-side fetch/use docs, and move the real CI/release lane to
  `wcp-runtime-lanes`.
- Tradeoff: local app docs still reference the toolchain, but the actual CI
  build and release ownership no longer live in this repo.
- Verification: `wcp-runtime-lanes/main` now carries
  `ci-host-llvm-toolchain.yml`; `Ae.solator` no longer carries that workflow.
- Next step: keep app-side local fetch pinned to the release asset and do not
  restore a second host-LLVM owner lane here.

### Entry 4: Main-only host-compiler CI rule

- Goal: stop drifting between temporary integration branches and the real owner
  lane for shared compiler work.
- Context: the user explicitly tightened the rule: host `LLVM` build/release
  work belongs in `wcp-runtime-lanes`, not `aesolator`, and ordinary sync work
  should land straight on `main`.
- Decision: strengthen `AGENTS.md` and the roadmap so host-compiler CI is
  treated as `wcp-runtime-lanes/main` work, while `aesolator` stays consumer
  only for that lane.
- Tradeoff: less room for branch-first experimentation in the app repo, but a
  much cleaner owner boundary and less merge debt.
- Verification: rules/docs updated locally.
- Next step: keep the next live fix loop focused on container/X11 startup in
  `aesolator`, and treat compiler CI only as an upstream consumer dependency.

### Entry 3: Restore redirect preload for arm64ec Android-bionic host launch

- Goal: close the fresh `winex11.drv` init regression on container startup.
- Context: the latest forensic bundle showed `winex11.drv` returning
  `PROCESS_ATTACH=0` under direct `android_bionic_wowbox64_guest`, while the
  host X11 lib closure itself was already present.
- Decision: restore `libredirect-bionic.so` into
  `GlibcProgramLauncherComponent.applyAndroidBionicHostEnv()`, alongside
  `libandroid-sysvshm.so` and `libevshim.so`, so the direct arm64ec host lane
  gets the same redirect contract as the dedicated bionic launcher.
- Tradeoff: preload chain is a little heavier again, but the launch contract is
  back in line with the donor/runtime expectation.
- Verification: code patched; next step is rebuild, reinstall, and re-run the
  same forensic container bootstrap on `container_id=1`.
- Next step: verify whether `winex11.drv` now initializes and whether the
  remaining tail moves past `nodrv_CreateWindow`.

### Entry 1: Local host LLVM lane pin

- Goal: pin local host compilation to `LLVM 22.1.1` instead of drifting
  between Termux `21.1.8` and incompatible NDK host binaries.
- Context: APK builds already exposed `llvm-strip` host mismatch tails, and the
  next requested lane includes future local `wine` compilation.
- Decision: add a repo-tracked build script for a source-built
  `LLVM 22.1.1` host toolchain, keep it outside rootfs/APK payloads, and teach
  the local env helper to prefer it automatically when present.
- Tradeoff: first-time setup is heavier, but later local builds get a stable
  compiler baseline.
- Verification: script/env/doc lane staged; actual 22.1.1 build follows next.
- Next step: build the toolchain into
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux` and verify
  `clang` / `llvm-strip` report `22.1.1`.

### Entry 2: Aggressive non-root build profile

- Goal: push the device harder for the `LLVM 22.1.1` source build without
  crossing into unstable OOM/thermal chaos.
- Context: the first `JOBS=2` and `JOBS=4` passes were safe but too slow for a
  full local LLVM lane on this phone.
- Decision: inspect live CPU/memory/swap, enable the strongest available
  non-root power profile through `adb`, disable animation overhead, and move
  the active LLVM build profile to `JOBS=6` with one linker job.
- Tradeoff: higher thermal and battery pressure, but far better compile
  throughput while staying inside non-root controls.
- Verification: device reports `8` CPUs, about `5.8 GiB` RAM, about `4.2 GiB`
  swap; low power mode is off; active LLVM build relaunched under the new
  profile.
- Next step: let the `22.1.1` compile lane run long enough to prove this
  profile survives real LLVM compilation and then verify installed tool
  versions from the local prefix.

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

### Entry 55: Trackpad move queue restored without giving up the cursor contract

- Goal: keep the restored trackpad semantics while removing the `MotionEvent`
  ANR that came back once direct pointer injection was moved onto the UI path.
- Context: after switching back from the hybrid tap-to-hit model, the user
  correctly reported that the desktop could freeze again under heavier cursor
  movement. Fresh forensics showed the process in `D` state and
  `InputDispatcher` waiting on `MotionEvent` delivery for
  `XServerDisplayActivity`, which pointed back to the same choke point:
  high-frequency pointer moves were again being injected synchronously on the
  active touch path.
- Decision: keep the no-shortcut desktop contract as `cursor_trackpad`, but
  reintroduce a coalesced background move queue and a separate button queue in
  `TouchpadView`. The fix preserves trackpad semantics by maintaining a
  logical cursor position, synchronizing button presses to that logical cursor,
  and coalescing move bursts off the UI thread instead of serializing every
  move directly through `MotionEvent`.
- Tradeoff: this is more stateful than the raw donor path, but it isolates the
  UI-thread regression we actually observed. The simpler direct path had already
  proven itself as the ANR trigger on this device, so matching it blindly was
  no longer defensible.
- Verification: the patched build compiles and installs cleanly, and the new
  short stress pass no longer reproduced the old process-wide `D` state in the
  synthetic run. Fresh screenshot proof remains contaminated by the shared
  Termux foreground, so closure currently rests on compile/install plus input
  and dispatcher evidence rather than clean device captures.
- Next step: keep the coalesced queue, then verify real manual drag/repeat-click
  quality the next time the phone can stay on `XServerDisplayActivity` long
  enough for an uncontaminated pass.

### Entry 56: Duplicate desktop cursor fix moved from fallback-only to full compositor ownership

- Goal: stop the desktop from painting a second cursor layer on top of a guest
  window that already owns its own visible pointer.
- Context: the previous ownership split only suppressed `rootCursorDrawable`.
  That was too narrow. A fullscreen-like guest window can still surface an X
  cursor through `pointWindow.attributes.getCursor()`, so the user can still
  see two cursors even after the root fallback is hidden. The report
  "one from the container, one from x11" matched that exact blind spot.
- Decision: move the ownership guard to the top of `GLRenderer.renderCursor()`.
  In desktop owner mode, fullscreen-like non-shell app windows now suppress the
  entire compositor cursor path, including both the ordinary X cursor layer and
  the old root fallback. Desktop shell surfaces (`explorer.exe`, `progman`,
  `shell_traywnd`, `workerw`) still keep the compositor cursor so the shell
  remains navigable.
- Tradeoff: owner classification is still heuristic. That is acceptable because
  the previous behavior was deterministically wrong for guest-owned cursors.
  The risk profile is now "maybe classify a window conservatively" instead of
  "always draw a duplicate cursor in the bad case."
- Verification: build/install passed. Short launch forensics confirm
  `XServerDisplayActivity` still starts cleanly; screenshot-based visual proof
  is currently invalidated by `Termux` retaking foreground on this shared
  device before a clean desktop capture can be taken.
- Next step: validate the new compositor suppression in a stable manual pass,
  then narrow the shell/app heuristic only if a specific window class still
  duplicates or loses its cursor.

### Entry 57: Orphan container recovery closed the `container_id -> null -> finish()` launch tail

- Goal: stop `XServerDisplayActivity` from self-finishing when a real
  `/files/imagefs/home/xuser-*` prefix exists but its `.container` metadata
  file is missing.
- Context: the last launch failure looked like another desktop/input collapse,
  but direct device inspection showed a different root cause. `xuser-4`
  existed, `.wine` existed, and the launch intent carried `container_id=4`,
  yet `ContainerManager` skipped the root entirely because `.container` was
  missing. That left `getContainerById(4)` returning `null`, so
  `XServerDisplayActivity` exited before the desktop bootstrap could even
  start. This was a state-recovery bug, not another cursor or shell bug.
- Decision: teach `ContainerManager.loadContainers()` to treat a prefix root
  with a real `.wine` directory and no `.container` file as an orphaned but
  recoverable container. The manager now scans local `Contents` installs under
  `files/contents/Proton` and `files/contents/Wine`, picks the best candidate
  by local freshness/version, writes a minimal recovered `.container`, and
  records `CONTAINER_CONFIG_RECOVERED` before the launch path continues.
- Tradeoff: the recovered runtime is inferred from the best locally installed
  runtime, not from lost historical metadata. That is still the correct
  tradeoff because the previous behavior was guaranteed failure. Recovery turns
  a dead prefix into a runnable container and keeps the remaining ambiguity
  explicit in forensic logs.
- Verification: rebuilt and reinstalled the APK, confirmed that
  `files/imagefs/home/xuser-4/.container` did not exist before the pass, then
  launched `XServerDisplayActivity` with `container_id=4`. The device created
  a new `.container` with `wineVersion:"Proton-10.0.99-arm64ec-1"`, emitted
  `CONTAINER_CONFIG_RECOVERED`, and `am start -W` returned `Status: ok` rather
  than the old `Failed to retrieve container with ID: 4`.
- Next step: keep desktop debugging focused on real post-bootstrap defects
  again. The recovered-container tail is closed; remaining desktop work is
  pointer/input quality and payload-consumer correctness, not missing metadata
  for existing prefixes.

### Entry 58: Desktop bootstrap termination now waits for the first shell-map race to settle

- Goal: stop `XServerDisplayActivity` from honoring an early guest-launcher
  exit before the first desktop shell window has a chance to map.
- Context: after `Container-4` was removed and the orphan-container tail was
  gone, the next clean device traces showed a narrower risk. The desktop path
  could reach `explorer.exe` mapping, but the code still had a bootstrap race:
  if the guest launcher exited before any tracked application window was
  recorded, `setTerminationCallback()` could go straight to `exit()` even
  though `explorer /desktop=shell` was still in the process of surfacing its
  first real window. That kind of race matches the user's intermittent
  "sometimes black screen, sometimes launcher, sometimes works" reports.
- Decision: add an explicit desktop-shell termination grace window in
  `XServerDisplayActivity`. During no-shortcut shell bootstrap, guest launcher
  termination is now deferred for up to 8 seconds even if tracked-window count
  is still zero. The deferred exit is cancelled immediately once the first
  tracked shell window maps. New forensic markers capture both the defer path
  and the grace-expired fallback.
- Tradeoff: this keeps the activity alive a little longer in the genuinely bad
  case where no shell window ever appears. That is the correct tradeoff
  because the previous behavior could kill a valid desktop bootstrap on a race,
  which is worse than waiting a few seconds before conceding failure.
- Verification: rebuilt and reinstalled the APK, ran a clean device launch for
  `container_id=3`, and confirmed `PRELOADER_MAP_FALLBACK`,
  `XSERVER_APP_WINDOW_MAPPED`, and `XSERVER_WINDOW_FOCUS_CHANGED` without any
  immediate `XSERVER_EXIT_REQUESTED` or
  `GUEST_PROGRAM_TERMINATION_GRACE_EXPIRED` in the short bootstrap pass.
- Next step: keep hunting the later desktop tail with this race removed from
  the picture. The remaining suspects are true post-bootstrap interaction
  defects or native/process death after the shell is already live, not the old
  first-window termination race.

### Entry 59: `Computer` was a desktop-icon double-click tail, not a broken file-manager package

- Goal: explain why the desktop reacted to `Start` and other shell actions but
  still appeared not to open the file manager when the user clicked
  `Computer`.
- Context: device reports narrowed the issue to one target only: shell clicks
  generally worked, but `Computer` did not appear to open. Fresh shell
  inspection already showed `explorer.exe` and `winefile.exe` present in both
  the runtime payload and the live prefix, so the remaining suspect was the
  desktop icon interaction contract rather than missing binaries. To remove
  guesswork, `XServerDisplayActivity` gained a debug probe that can send
  anchored single- or multi-tap sequences to arbitrary logical desktop
  coordinates after shell bootstrap.
- Decision: use that probe to sweep the upper-left desktop icon lane. The
  targeted two-tap probe at `x=60, y=72/96` changed only the upper desktop
  region relative to a clean baseline, which matches a `Computer`/explorer
  window opening path rather than a global shell failure. With the shell path
  proven alive, the manual-input defect reduced to tap stability: our normal
  tap path clicked at the current logical cursor, which could drift a few
  pixels during micro-jitter between finger-down and finger-up. `TouchpadView`
  now anchors tap clicks to the cursor position captured at finger-down, so a
  two-tap desktop icon gesture stays on one target even if the finger jitters.
- Tradeoff: tap clicks now favor target stability over preserving tiny cursor
  drift during a tap-sized gesture. That is the correct tradeoff for desktop
  semantics: a real move still works once the gesture exceeds tap slop, while
  icon activation becomes far less fragile.
- Verification: the debug probe build compiled and installed successfully, the
  coordinate sweep isolated a stable `Computer` icon region, and the follow-up
  anchor-click patch rebuilt and installed cleanly. The shared-device capture
  environment still contaminates some post-probe screenshots, so final closure
  needs a short live user pass on the new build rather than more ADB-only
  screenshots.
- Next step: validate the manual two-tap `Computer` gesture on device, then
  keep the same anchored-tap contract for any remaining desktop icon or
  explorer-shell tails before revisiting broader runtime payload work.

### Entry 60: Anchored tap targeting was rolled back after it degraded normal desktop clicks

- Goal: recover the previously stable desktop click path after the first
  `Computer`-focused fix attempt made ordinary interaction feel dead on device.
- Context: the `Computer` probe sweep was useful, but the first follow-up fix
  went too far. Anchoring taps to the cursor position captured at finger-down
  improved desktop-icon target stability in theory, yet the live device report
  immediately regressed to "stopped reacting". That was enough evidence to
  treat the change as a bad experiment rather than keep rationalizing it.
- Decision: revert only the `TouchpadView` anchor-tap portion and keep the
  new debug probe infrastructure in `XServerDisplayActivity`. That restores the
  previously working click contract while preserving the forensic tooling that
  proved `Computer` itself is not a missing-binary problem.
- Tradeoff: the narrow `Computer` convenience tail stays open a little longer,
  but ordinary desktop interaction returns to the known-good state. That is the
  right priority order; a partially helpful icon tweak is not worth breaking
  the base desktop.
- Verification: rebuilt and reinstalled the rollback build, then reran a clean
  cold start. `am start -W` returned `Status: ok`, followed by
  `PRELOADER_MAP_FALLBACK`, two `XSERVER_APP_WINDOW_MAPPED` events, and
  `XSERVER_WINDOW_FOCUS_CHANGED has_focus:true`, which put the shell back into
  the stable state that existed before the anchored-tap experiment.
- Next step: pursue `Computer` convenience via a safer shell/UI route instead
  of modifying the base tap contract again without stronger device evidence.

### Entry 61: `GameNative` donor audit was narrowed to foundation imports instead of a blind merge

- Goal: turn the user's broad `GameNative` request into a usable import lane
  for `x11`, renderer, and driver work without collapsing `aesolator` into a
  donor code dump.
- Context: the donor repository mixes several layers together:
  shared Winlator/X11 code, richer Vulkan probe helpers, alternate renderer
  plumbing, launch-time graphics policy, gesture UX, and app-specific UI. A
  naive copy would blur ownership and destabilize the most fragile paths.
- Decision: classify the donor into `import now`, `adapt later`, and
  `do not blind-copy`, then land only the foundation pieces that materially
  strengthen our stack:
  `GPUHelper`, native Vulkan extension/API probe, GPU classification helpers,
  `GPUImage` hardware-buffer exposure, and `XConnectorEpoll` fd/rlimit
  hygiene. The written system map now lives in
  `docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md`.
- Tradeoff: this is slower than cargo-culting whole donor subsystems such as
  `TouchGestureConfig` or Vortek launch policy. That tradeoff is correct
  because `aesolator` already exceeds the donor in forensics, contents
  contracts, and driver-package metadata, so bulk merging would add churn
  faster than value.
- Verification: the donor inventory was completed across
  `xserver/*`, `renderer/*`, `TouchpadView`, `GPUInformation`,
  `GPUHelper`, `GeneralComponents`, `XConnectorEpoll`, native `cpp/winlator/*`,
  and `app/gamenative/ui/screen/xserver/XServerScreen.kt`. The resulting
  import queue was then written down and the first donor-derived code blocks
  were added to the local tree without compiling yet, per the user's request.
- Next step: wire the new foundation helpers into graphics/runtime policy,
  then resume the live desktop/input lane on top of a cleaner X11/renderer
  base instead of ad-hoc patches.

### Entry 62: the `GameNative` transfer scope was widened from X11-only to the full runtime stack

- Goal: respond to the user's instruction to prepare a full donor transfer
  covering runtime, payloads, placement, containerization, launch flow, and
  Wine/Proton handling, not just X11/renderer.
- Context: the first donor audit closed only the `x11` / renderer / driver
  foundation. A broader grep across `app/gamenative/*` and `com/winlator/*`
  then showed the real heavyweight donor layers:
  `XServerScreen.kt`, `ContainerUtils.kt`, `IntentLaunchManager.kt`,
  `ManifestInstaller.kt`, `ManifestComponentHelper.kt`, `PreInstallSteps.kt`,
  launcher components, and Wine/Proton manager surfaces.
- Decision: add a second donor document,
  `docs/GAMENATIVE_FULL_TRANSFER_MATRIX.md`, that treats `GameNative` as a
  multi-lane runtime transfer instead of a single X11 donor. The matrix now
  tracks already imported foundation pieces and the next three major import
  queues:
  launcher/runtime execution, payload/manifest install, and container routing.
- Tradeoff: the transfer is now explicitly staged instead of pretending we can
  safely absorb every donor subsystem in one opaque patch. That is the only
  defensible way to preserve `aesolator` provenance and forensic contracts
  while still honoring the user's "full transfer" direction.
- Verification: completed broad grep passes over both `app/gamenative/*` and
  `com/winlator/*`, ranked the highest-signal runtime files, wrote the global
  transfer matrix, and synchronized `AGENTS.md` plus the roadmap to treat the
  full transfer as an active top-level lane.
- Next step: start the next code lane with donor launcher/runtime components
  and the payload/manifest install stack, because those are the biggest
  remaining gaps between package intake and actual execution.

### Entry 63: helper-level donor transfers were expanded beyond Vulkan probe only

- Goal: keep the donor transfer moving in code, not just in planning docs, by
  importing low-risk runtime helpers that will support the next large launcher
  and payload passes.
- Context: after the broader grep, three small but useful donor helpers stood
  out as portable enough to land immediately:
  `DXVKHelper`, `GeneralComponents`, and `PatchElf`. None of them solve the
  big runtime lane alone, but all three reduce future pressure on
  `XServerDisplayActivity` and component-placement logic.
- Decision: add adapted `cmod` versions of
  `core/DXVKHelper.java`, `core/GeneralComponents.java`, and
  `core/PatchElf.java` to the local tree, and mark them as already imported in
  the full transfer matrix. They are not yet wired into the active launch path;
  this pass was about staging reusable helpers first.
- Tradeoff: these imports add more dormant code before the next big runtime
  execution transfer lands. That is acceptable because they are small,
  self-contained, and directly support upcoming work on payload placement and
  environment shaping.
- Verification: static integration only. No build or device pass was run in
  this sub-pass.
- Next step: move from helper imports to the actual launcher/runtime layer:
  donor `GuestProgramLauncherComponent`, `BionicProgramLauncherComponent`,
  `GlibcProgramLauncherComponent`, and the payload/manifest install lane.

### Entry 64: donor manifest and install foundation was adapted onto the local contents contract

- Goal: turn the `GameNative` payload/manifest donor lane into real local code
  instead of leaving it as a grep-only plan.
- Context: `GameNative` already had a stronger donor layer for package
  availability, manifest caching, version-option building, and post-download
  install routing. `aesolator` had the low-level install primitives already,
  but no dedicated donor-style manifest abstraction above them.
- Decision: add local Java adaptations of donor
  `ManifestModels`, `ManifestRepository`, `ManifestInstaller`, and
  `ManifestComponentHelper` under `app/src/main/java/com/winlator/cmod/contents`.
  The crucial constraint was to keep `Ae.solator` on a single trusted install
  path:
  content payloads still go through
  `ContentsManager.extraContentFile -> finishInstallContent`,
  and graphics-driver payloads still go through
  `AdrenotoolsManager.installDriver()`.
- Tradeoff: this is foundation only. The new manifest layer is not wired into a
  visible dialog or screen yet, and it was intentionally ported in Java rather
  than by turning on Kotlin for the whole module.
- Verification: static integration only. No build or device pass was run in
  this donor-transfer sub-pass.
- Next step: continue with launcher/runtime execution import
  (`GuestProgramLauncherComponent` diff against donor), then wire this manifest
  layer into a visible runtime/package-management surface without breaking
  provenance labels or existing `Contents` rules.

### Entry 65: donor `ubuntufs` path was reframed into a hybrid `imagefs` plan

- Goal: turn the user's rootfs request into a real engineering lane instead of
  vaguely "using the donor UbuntuFS".
- Context: a direct donor grep showed that `GameNative` `:ubuntufs` is only an
  on-demand dynamic-feature shell. The real rootfs ownership still lives in
  `ImageFsInstaller`, which selects variant-specific archives
  (`imagefs_gamenative.txz`, `imagefs_bionic.txz`), tracks a newer rootfs
  version (`26`), deploys `redirect.tzst` / `extras.tzst`, and preserves
  imported `Wine` / `Proton` payloads under `opt/`.
- Decision: add a dedicated hybrid-rootfs document,
  `docs/IMAGEFS_HYBRID_PLAN.md`, and elevate `imagefs` refresh to its own
  roadmap lane. The chosen direction is not donor replacement; it is a hybrid
  `Ae.solator` rootfs that keeps our `Contents` / runtime / forensic contracts
  while harvesting fresher donor userland pieces.
- Tradeoff: this adds another explicit lane before any archive rebuild can
  happen. That extra discipline is necessary because rootfs mistakes are more
  expensive than ordinary UI or package-feed regressions.
- Verification: source-path audit only. No archive extraction or build was run
  in this sub-pass.
- Next step: unpack donor `imagefs_gamenative.txz` and `imagefs_bionic.txz`,
  diff them against our `imagefs.txz`, and build a per-library adoption table.

### Entry 66: donor launcher/runtime execution split was transferred without reopening builds

- Goal: move the `GameNative` transfer from helper-only groundwork into the
  real runtime execution path while honoring the user's new rule not to
  compile until the broader transfer lane is materially in place.
- Context: the previous pass had already imported manifest/install helpers and
  runtime infrastructure, but `Ae.solator` still hardcoded a single launcher
  model and did not expose donor-style runtime libc markers inside `ImageFs`.
  That made it impossible to carry donor `bionic` / `glibc` execution policy
  into the live launch stack cleanly.
- Decision: extend local `ImageFs` with donor-style variant/runtime helpers,
  open `GuestProgramLauncherComponent` for subclass-based launcher models, add
  local `BionicProgramLauncherComponent`, `GlibcProgramLauncherComponent`, and
  `GuestProgramLauncherFactory`, then switch `XServerDisplayActivity` to pick
  the launcher from the active runtime libc model instead of hardcoding one
  path.
- Tradeoff: this is still a foundation transfer, not full closure. It adds new
  launcher/runtime structure before `ImageFsInstaller`, request-path parity,
  and payload-placement parity are imported. That is acceptable because the
  user's rule currently prefers a complete staged transfer over premature build
  verification.
- Verification: static code transfer only. No compile, APK install, or device
  pass was run in this sub-pass by design.
- Next step: continue directly into donor `ImageFsInstaller` / runtime
  placement and launcher-request plumbing, then sync the transfer matrix again
  before reopening any build pass.

### Entry 67: donor `ImageFsInstaller` and rootfs placement foundation were transferred without reopening builds

- Goal: move the `GameNative` rootfs lane from planning docs into actual local
  code so the launcher/runtime split now lands on a donor-style installer and
  payload-preservation contract instead of the old monolithic `imagefs.txz`
  assumption.
- Context: after Entry 66, the app already knew about launcher libc models, but
  the rootfs install path still lived on the old local contract:
  one `imagefs.txz`, no donor overlay deployment, no `containerVariant`, and no
  donor-style preservation of imported `Wine` / `Proton` payloads in `opt/`.
- Decision: adapt the donor `ImageFsInstaller` foundation into the local tree:
  raise `LATEST_VERSION` to `26`, add variant-aware archive selection with
  legacy fallback, preserve imported runtimes in `opt/`, deploy donor
  `redirect.tzst` / `extras.tzst` overlays when present, add
  `Container.containerVariant`, and write `.variant` / `.arch` markers into
  `imagefs` at launch time.
- Tradeoff: this is still not a rebuilt hybrid rootfs. The donor base archives
  are not yet staged locally, so the install path can only use them when they
  are later supplied via assets/downloads. That is acceptable because the user
  explicitly asked for the full transfer lane to stay build-free until more of
  the donor stack is in place.
- Verification: static code and asset transfer only. No compile, APK install,
  or device pass was run in this sub-pass by design.
- Next step: inspect and diff actual donor base archives
  `imagefs_gamenative.txz` / `imagefs_bionic.txz`, then continue into donor
  request/runtime routing (`WineRequestComponent` / related launcher helpers)
  so rootfs placement, runtime launch, and payload intake converge on one
  execution model.

### Entry 68: donor network/runtime environment helpers were staged into the local execution stack

- Goal: keep the launcher/runtime transfer moving past rootfs and into the
  surrounding environment components that real Wine/Proton sessions depend on
  at boot.
- Context: `GameNative` does not just launch Wine; it also refreshes
  network-facing runtime files under `imagefs/usr/tmp` and `etc/hosts` through
  donor `NetworkHelper` and `NetworkInfoUpdateComponent`. Local `Ae.solator`
  still had a more limited Wi-Fi-only helper and no environment component for
  `ifaddrs` / hosts refresh.
- Decision: adapt donor-style `IFAddress`, active-network probing, and IPv4
  discovery into local `NetworkHelper`, add a local
  `NetworkInfoUpdateComponent`, and stage that component into
  `XServerDisplayActivity`'s environment stack next to the existing X11/audio
  components.
- Tradeoff: `WineRequestComponent` is still open, so request-path parity is not
  closed yet. This pass intentionally targeted the simpler network/runtime
  foundation first instead of mixing URL/clipboard request routing into the
  same patch.
- Verification: static code transfer only. No compile, APK install, or device
  pass was run in this sub-pass by design.
- Next step: continue with donor `WineRequestComponent` versus local
  `WineRequestHandler`, then decide whether to fold request handling fully into
  `XEnvironment` or keep a hybrid bridge with donor socket behavior.

### Entry 69: donor request routing was folded into `XEnvironment`

- Goal: remove another non-donor execution tail by replacing the old standalone
  `WineRequestHandler` with an environment-managed request component.
- Context: local `Ae.solator` still handled browser/clipboard requests through a
  separate `WineRequestHandler` field in `XServerDisplayActivity`, while
  `GameNative` keeps URL request routing inside `XEnvironment` as
  `WineRequestComponent`. That split left lifecycle and socket ownership
  outside the same environment model as X11/audio/network components.
- Decision: add a local `WineRequestComponent` under
  `xenvironment/components`, move request-server lifecycle into
  `XEnvironment`, delete the old `core/WineRequestHandler.java`, and keep a
  hybrid request surface: donor-style socket/lifecycle structure plus the local
  Android clipboard import/export bridge.
- Tradeoff: the donor's Epic/auth-specific branch was not copied blindly,
  because there is no equivalent local auth activity in `Ae.solator`. The
  request lane is therefore materially transferred, but donor-specific
  storefront/auth hooks remain consciously open rather than fake-imported.
- Verification: static code transfer only. No compile, APK install, or device
  pass was run in this sub-pass by design.
- Next step: continue into the next launcher-lane holdouts,
  `SteamClientComponent` and any remaining auth/request branches that are worth
  adapting, then resume donor archive diffing for the hybrid rootfs lane.

### Entry 70: commit discipline was re-aligned to a single donor-transfer batch

- Goal: align the work process with the user's new rule that the full donor
  transfer should land as one batch commit instead of a series of subsystem
  commits.
- Context: earlier in the transfer lane, launcher/runtime, imagefs/rootfs, and
  network/runtime environment passes had been committed separately as the work
  advanced. The user then clarified that the intended discipline is one
  cumulative transfer batch, not fragmented commit history during the lane.
- Decision: from this point forward, keep the remaining donor-transfer work as
  one cumulative staged batch until the lane reaches a real closure point.
  Continue syncing docs and staging transfer files, but do not cut another
  transfer commit mid-lane unless the user changes the rule again.
- Tradeoff: this keeps the batch semantically cleaner for the user, but it also
  means intermediate transfer steps remain less independently checkpointed in
  git while the lane is open.
- Verification: process/documentation update only. No compile, APK install, or
  device pass was run in this sub-pass.
- Next step: continue the remaining donor execution stack under the new batch
  rule, starting with `WineRequestComponent` closure and then the next
  launcher/runtime holdouts.

### Entry 71: donor Steam pipe foundation was staged without pretending storefront wiring exists

- Goal: keep the launcher/runtime donor lane moving by importing the small,
  self-contained Steam-side runtime foundation instead of leaving it as a
  permanent TODO.
- Context: donor `SteamClientComponent` turned out to be thin wrapper logic over
  `SteamPipeServer`, not a giant storefront subsystem. Local `Ae.solator` did
  not have any equivalent classes in-tree, but it also does not yet have a
  donor-style Steam source lane that would exercise the component.
- Decision: import local adaptations of donor `RequestCodes`,
  `SteamPipeServer`, and `SteamClientComponent`, but keep them unwired for now.
  This treats them as staged runtime infrastructure rather than falsely calling
  Steam integration complete.
- Tradeoff: this adds dormant donor foundation code before the local product has
  a consumer for it. That is acceptable because the component is small and
  isolated, and the alternative was to keep lying to the transfer matrix about
  the remaining scope.
- Verification: static code transfer only. No compile, APK install, or device
  pass was run in this sub-pass by design.
- Next step: continue the launcher/runtime lane with the next real consumers:
  launch-request/container-routing helpers and donor rootfs archive diffing.

### Entry 72: donor rootfs delivery sources were pinned down beyond archive names

- Goal: close another rootfs-tail in the docs by recording where donor imagefs
  artifacts actually come from, not just what they are called.
- Context: earlier rootfs notes already identified the donor archive names and
  overlay files, but the delivery side was still fuzzy. A targeted grep through
  donor `SteamService` showed the real download contract:
  `fetchFileWithFallback()` serves rootfs artifacts from a GameNative primary
  host plus an R2 fallback bucket, and also exposes
  `imagefs_patches_gamenative.tzst` beyond the base archives.
- Decision: update the hybrid-rootfs docs and transfer matrix to record the
  primary/fallback URLs and the existence of the extra patches archive, so the
  next archive-diff pass has an explicit source map.
- Tradeoff: this is still documentation/forensics, not the archive diff itself.
  That tradeoff is correct because knowing the real delivery contract is a
  prerequisite for a defensible hybrid-rootfs lane.
- Verification: source grep only. No compile, APK install, or device pass was
  run in this sub-pass.
- Next step: either stage the donor rootfs archives locally for actual diffing,
  or continue container-routing/request-transfer work in parallel until the
  archive inputs are available.

### Entry 73: donor rootfs delivery was confirmed live, not just inferred from code

- Goal: upgrade the rootfs lane from code-only delivery assumptions to verified
  remote artifact facts.
- Context: after Entry 72, the docs already knew the primary/fallback URLs, but
  they still lacked proof that the primary host was serving the expected rootfs
  artifacts right now.
- Decision: perform live `HEAD` checks against the primary donor host for
  `imagefs_gamenative.txz`, `imagefs_bionic.txz`,
  `imagefs_patches_gamenative.tzst`, and `extras.tzst`, then write the
  observed sizes and modification dates into the rootfs documents.
- Tradeoff: this still does not unpack or diff the archives. It simply removes
  another layer of uncertainty before the expensive archive-analysis pass.
- Verification: live remote `HEAD` checks completed on `2026-03-15`; no build,
  APK install, or device pass was run.
- Next step: either bring the donor rootfs archives into local analysis for an
  actual file-level diff, or continue closing launch-routing/storefront tails
  while the archive analysis lane is prepared.

### Entry 74: subclass-aware environment lookup fixed a transfer-induced runtime tail

- Goal: close a subtle logic regression introduced by the launcher split before
  it became another hidden runtime failure later.
- Context: after adapting local `BionicProgramLauncherComponent` and
  `GlibcProgramLauncherComponent`, `XEnvironment.onPause()` / `onResume()`
  still looked up `GuestProgramLauncherComponent` by exact class equality.
  That meant the new launcher subclasses would not be found for
  suspend/resume, even though the donor transfer itself had succeeded.
- Decision: change `XEnvironment.getComponent()` to use
  `componentClass.isInstance(component)` and a typed cast instead of exact class
  comparison.
- Tradeoff: none worth keeping. The old exact-class lookup was now simply wrong
  for the transferred launcher model.
- Verification: static code correction only. No compile, APK install, or device
  pass was run in this sub-pass.
- Next step: keep scanning the transferred execution stack for similar
  donor-induced integration tails while container-routing and request wiring
  continue.

### Entry 75: donor container-routing lane was explicitly put on hold instead of fake-imported

- Goal: close a planning tail in the transfer matrix by stating clearly what is
  actually blocked in donor container-routing, instead of leaving Lane 4 as a
  vague "not yet transferred".
- Context: a fresh pass over donor `IntentLaunchManager`, `ContainerMigrator`,
  and `ContainerUtils` showed that the lane is tightly coupled to storefront
  `appId` strings, donor `ContainerData`, and temporary config overrides. Local
  `Ae.solator` currently has only direct `selected_menu_item_id` routing and no
  equivalent DTO/override model.
- Decision: mark Lane 4 as an explicit `hold` in the matrix. That preserves
  honesty in the full-transfer plan and avoids cargo-culting donor routing code
  into a data model that does not exist locally yet.
- Tradeoff: this leaves a visible open lane in the transfer matrix, but it is a
  truthful open lane rather than a fake closed one.
- Verification: donor-code audit only. No compile, APK install, or device pass
  was run in this sub-pass.
- Next step: keep pushing the lanes that do have a compatible local substrate
  now: launcher/runtime, request routing, and rootfs analysis.

### Entry 76: rootfs transfer stopped being theoretical and gained a real donor install chain

- Goal: close the biggest remaining honesty gap in the rootfs lane before any
  future compile: the code already knew donor archive names, but it still did
  not behave like donor delivery/runtime patching actually existed.
- Context: local `ImageFsInstaller` had variant-aware names and overlay hooks,
  but still assumed the archive was somehow already present in `filesDir` or
  assets. On top of that, glibc support still lacked the donor patch layer,
  donor main-runtime container pattern, donor pulseaudio overlay, and
  `prefixPack.tzst` fallback parity.
- Decision: extend `ImageFsInstaller` with donor-style primary/fallback remote
  delivery for `imagefs_gamenative.txz`, `imagefs_bionic.txz`, and
  `imagefs_patches_gamenative.tzst`; stage donor
  `container_pattern_gamenative.tzst` and `pulseaudio-gamenative.tzst`
  locally; route glibc `applyGeneralPatches()` through the donor patch/audio
  path; and let `ContainerManager` accept both `prefixPack.tzst` and
  `prefixPack.txz`.
- Tradeoff: this is still not the full archive-diff/rebuild stage. It closes
  the delivery/runtime-contract tail first, while the heavier library-level
  rootfs analysis remains open.
- Verification: code-and-doc transfer only. No compile, APK install, or device
  pass was run in this sub-pass by rule.
- Next step: continue Lane 8 by unpacking and classifying donor rootfs
  archives/payloads, then keep folding the remaining runtime/container tails
  into the same batch until it reaches an honest compile point.

### Entry 77: X11/input payload selection stopped treating every runtime like one generic archive

- Goal: close a small but real donor tail in the X11/input lane before compile:
  input DLL bootstrap payload should follow runtime arch instead of always
  unpacking the same legacy archive.
- Context: donor `GameNative` `XServerScreen.kt` already split this payload by
  runtime arch (`arm64ec_input_dlls.tzst`, `x86_64_input_dlls.tzst`), while
  local `XServerDisplayActivity` still always extracted one generic
  `input_dlls.tzst`.
- Decision: stage both donor input DLL archives locally and adapt
  `extractInputDLLs()` to prefer the arch-specific asset when the active
  runtime is `arm64ec` or `x86_64`, falling back to the legacy archive only
  when no matching donor asset is bundled.
- Tradeoff: this is a narrow import, not a full donor gesture-stack merge.
  That is intentional; payload selection is stable low-risk infrastructure,
  while broader touch/gesture logic is still a separate closure problem.
- Verification: code-and-doc transfer only. No compile, APK install, or device
  pass was run in this sub-pass by rule.
- Next step: keep scanning the donor execution stack for other small but
  runtime-critical payload mismatches that can be closed before the honest
  compile point.

### Entry 78: main runtime pathing and archive inventory were brought closer to donor reality

- Goal: close the next class of hidden rootfs/runtime mismatches before compile:
  not just named donor archives, but the actual runtime layout assumptions they
  carry.
- Context: donor `ImageFs` and several donor helpers assume `/opt/wine` for the
  main runtime, while local code still had scattered fallback checks for only
  `/opt/<main-runtime-id>`. At the same time, streamed donor archive inventory
  showed a real split in archive character: `glibc` carries a heavy `opt/`
  tooling/runtime layer plus host-metadata noise, while `bionic` looks much
  closer to a clean userland/Vulkan/OpenAL surface.
- Decision: add a local `getMainWineDir()` bridge so core runtime/profile/UI
  paths accept both `/opt/wine` and `/opt/<main-runtime-id>`; update main
  runtime fallback checks in `WineInfo`, `WineRuntimePresenceDependency`, and
  `ContainerDetailFragment`; add donor-derived
  `ImageFsInstaller.generateCompactContainerPattern()` adapted to the bridged
  layout; and write the archive findings into the rootfs docs/transfer matrix.
- Tradeoff: this still does not resolve the full archive-adoption table. It
  does remove another class of silent false negatives before the first honest
  compile.
- Verification: code-and-doc transfer only, plus streamed archive inventory
  from donor hosts. No compile, APK install, or device pass was run in this
  sub-pass by rule.
- Next step: continue closing remaining donor/runtime tails until the batch is
  ready for the first honest compile, then only move into build verification.

### Entry 79: installed runtime packages stopped pretending install dir is always the runtime root

- Goal: close another silent runtime-placement mismatch before the first honest
  compile: imported `Wine` / `Proton` packages can carry a wrapper directory,
  while launcher/container code historically treated the install dir itself as
  the runtime root.
- Context: local `ContentsManager` already carried `wineBinPath`,
  `wineLibPath`, and `winePrefixPack`, but `WineInfo.fromIdentifier()` still
  handed `ContainerManager` and launcher code only the plain install path.
  That left nested runtime layouts vulnerable to false missing-file behavior
  even when the package metadata itself was correct.
- Decision: make `ContentsManager` derive an effective runtime root from the
  shared parent of `wineBinPath` / `wineLibPath` / `winePrefixPack`; make
  `WineInfo` consume that resolved root; and add donor-style post-install
  runtime cleanup so imported packages also normalize `lib/wine`, guarantee a
  runtime-root `prefixPack.*` path for legacy container consumers, and restore
  executable bits on installed binaries.
- Tradeoff: this still assumes donor-style runtime packages keep
  `bin` / `lib` / `prefixPack` under one shared payload root. That is the right
  pre-compile assumption because the donor and local runtime consumers already
  depend on that contract.
- Verification: static code and documentation pass only. No compile, APK
  install, or device run was performed in this sub-pass by rule.
- Next step: keep closing the remaining donor/runtime tails until the batch
  reaches the first honest compile point, then move into real build
  verification.

### Entry 80: donor Steam-side runtime plumbing stopped being a dead transfer artifact

- Goal: close another small but real donor tail before compile: `SteamPipeServer`
  and `SteamClientComponent` were already imported, but the local environment
  stack still never started them.
- Context: donor `GameNative` wires `SteamClientComponent()` into the live
  X-server environment next to `NetworkInfoUpdateComponent`, while local code
  still left the component staged but disconnected.
- Decision: attach `SteamClientComponent` to local `XServerDisplayActivity`
  during `XEnvironment` construction so the Steam-side donor plumbing is part
  of the runtime stack, not just code parked in-tree.
- Tradeoff: this is still only the infrastructure wiring step, not a verified
  Steam runtime flow. That is acceptable before compile because the open tail
  has narrowed from “not wired at all” to “needs real runtime verification”.
- Verification: static code and documentation pass only. No compile, APK
  install, or device run was performed in this sub-pass by rule.
- Next step: keep closing the remaining donor/runtime/rootfs tails until the
  batch reaches the first honest compile point.

### Entry 81: rootfs archive diffing stopped treating donor bionic and donor glibc as one blur

- Goal: move the rootfs lane one step closer to an honest compile by replacing
  vague “donor rootfs is newer” language with subsystem-level archive facts.
- Context: code and docs already knew the donor archive names and delivery
  chain, but the adoption strategy was still too hand-wavy about which archive
  actually carries userland libraries and which one carries utility overlays.
- Decision: stream-inspect local `imagefs.txz`, donor `imagefs_bionic.txz`,
  donor `imagefs_gamenative.txz`, and donor
  `imagefs_patches_gamenative.tzst`; record that local/base and donor `bionic`
  are close on userland surfaces (GStreamer, Pulse, DBus, fonts, Vulkan,
  OpenAL, winetricks, CLI tooling), while donor patch/glibc layers own the
  extra `opt/system32` / `opt/apps` / `Steamless` / `7-Zip` utility surface
  and also carry macOS archive noise that must not leak into a rebuilt rootfs.
- Tradeoff: this is still streamed manifest evidence, not a fully unpacked
  library-by-library adoption table. That is acceptable at this stage because
  it closes the false equivalence between donor `bionic` and donor `glibc`
  before compile.
- Verification: source/archive inspection only. No compile, APK install, or
  device run was performed in this sub-pass by rule.
- Next step: keep folding the remaining rootfs/runtime tails into the same
  donor-transfer batch until the tree reaches the first honest compile point.

### Entry 82: staged donor overlays stopped being anonymous blobs

- Goal: close another rootfs tail before compile by classifying the already
  imported donor overlays instead of leaving `extras.tzst` and `redirect.tzst`
  as opaque assets.
- Context: the donor overlay archives were already present locally, but docs
  still treated them mostly as names rather than owned subsystem layers.
- Decision: inspect both staged archives directly and record that
  `extras.tzst` is the utility overlay (`Steamless`, `7-Zip`, `GPUInfo.exe`,
  `TestD3D.exe`, `generate_interfaces_file.exe`, `wine-mono`) while
  `redirect.tzst` is the redirect-hook layer
  (`usr/lib/libredirect.so`, `usr/lib/libredirect-bionic.so`); also record that
  both overlays still carry macOS packaging noise and therefore need cleanup
  before any rebuilt rootfs lane.
- Tradeoff: this still does not answer whether each utility should live in base
  rootfs, overlay, or package/runtime space long-term. It does remove the
  ambiguity about what the imported archives actually contain.
- Verification: archive inspection only. No compile, APK install, or device run
  was performed in this sub-pass by rule.
- Next step: keep closing the remaining donor/runtime/rootfs tails until the
  batch reaches the first honest compile point.

### Entry 83: donor runtime payload parity stopped having obvious holes

- Goal: close the remaining easy donor-runtime gap before the first compile by
  staging the missing payload assets rather than leaving half the donor lanes
  present only on paper.
- Context: a focused asset inventory showed local runtime payload coverage was
  still visibly behind donor `GameNative` in `graphics_driver`, `dxwrapper`,
  `fexcore`, `wowbox64`, `steampipe`, `wincomponents`, `box86_64`, and
  `steaminput`, plus root-level `steam_regions.json` and
  `box86_env_vars.json`.
- Decision: copy the missing donor runtime assets into the local tree without
  overwriting already existing local payloads, then rerun the focused asset
  inventory to confirm that the selected donor runtime lanes no longer had
  missing files on the local side.
- Tradeoff: this closes payload parity faster than code parity. That is the
  right order here because payload absence would invalidate later runtime
  verification even if the surrounding Java glue looked complete.
- Verification: static asset transfer plus focused donor/local inventory diff.
  No compile, APK install, or device run was performed in this sub-pass by
  rule.
- Next step: continue with the remaining code-side donor gaps, not payload
  staging.

### Entry 84: donor bionic helper libraries stopped being implicit runtime assumptions

- Goal: close another pre-compile donor/runtime tail by making the donor helper
  libs explicit instead of assuming they magically appear in the guest rootfs.
- Context: donor `GameNative` expects `libevshim.so` and
  `libredirect-bionic.so` in guest `usr/lib`, while renderer-side donor lanes
  expect APK-native `libvirglrenderer.so` / `libvortekrenderer.so`. Local tree
  had part of that story in overlays, but not the full ownership rule.
- Decision: stage donor `libdummyvk.so`, `libevshim.so`,
  `libvirglrenderer.so`, and `libvortekrenderer.so` into local `jniLibs`,
  mirror `libevshim.so` and `libdummyvk.so` into guest `usr/lib` during
  `ImageFsInstaller.installGuestLibs()`, and teach the bionic launcher to
  append `libevshim.so` plus `libredirect-bionic.so` into `LD_PRELOAD` when
  present.
- Tradeoff: this still stops short of a full source-backed `evshim` build
  because donor repo exposes `evshim.c` without the SDL2 header/toolchain
  needed for an immediate honest local compile. Pre-compile parity therefore
  uses staged donor binaries with explicit ownership docs.
- Verification: code-and-doc transfer only. No compile, APK install, or device
  pass was run in this sub-pass by rule.
- Next step: keep closing the remaining donor utility/input gaps before the
  first honest compile.

### Entry 85: donor Vortek stopped being just an audit note

- Goal: close the last obvious renderer-subsystem gap before compile by moving
  donor `Vortek` from “documented missing lane” to staged local foundation.
- Context: earlier audit already showed `Vortek` was a real donor subsystem:
  socket transport, HardwareBuffer-backed window content, native
  `libvortekrenderer.so`, and graphics-driver coupling. Leaving it out would
  keep the transfer batch in a half-documented state.
- Decision: stage local `VortekRendererComponent`, local `VortekConfigDialog`,
  add donor parity socket constants for `VORTEK_SERVER_PATH`, and stage the
  donor APK native library plus graphics-driver assets.
- Tradeoff: the lane is now present in-tree, but not yet wired into the live
  runtime path or build-verified. That is acceptable in this no-compile phase:
  the missing-subsystem tail is closed, while runtime wiring remains an
  explicitly documented later step.
- Verification: code-and-doc transfer only. No compile, APK install, or device
  pass was run in this sub-pass by rule.
- Next step: run one more donor inventory pass and close whatever remains
  before the first honest compile.

### Entry 86: donor libwinlator_11 stopped looking like a mysterious upgrade button

- Goal: answer the donor `libwinlator_11.so` question before compile so the
  batch does not accidentally pivot onto a binary-only native lane.
- Context: the donor repo ships `libwinlator_11.so` only as a prebuilt APK
  native library, while the visible donor source tree still builds `winlator`
  from source. That smelled like a trap unless the exported JNI surface was
  inventoried explicitly.
- Decision: inspect donor `libwinlator_11.so` symbol exports and compare them
  against donor `libwinlator.so` and local source-backed native code. The
  result: `libwinlator_11.so` maps to the donor `XInputStream` /
  `XOutputStream` JNI path, while local `cmod` already uses Java-side stream
  classes plus richer source-backed native `GPUInformation`.
- Tradeoff: this means the donor binary is useful as a reference artifact, not
  as a drop-in source-of-truth. That is the correct conservative conclusion
  before the first honest compile.
- Verification: binary symbol inventory only. No compile, APK install, or
  device pass was run in this sub-pass by rule.
- Next step: keep transfer focus on source-backed donor improvements and leave
  binary-only donor lanes explicitly classified instead of half-imported.

### Entry 87: donor compatibility perimeter shrank to the genuinely nontrivial tail

- Goal: reduce the remaining donor gap list again before the first honest
  compile without dragging in app-specific Compose/DataStore debt.
- Context: after the second inventory pass, the remaining donor code files were
  already mostly down to `box86_64` management, donor input-model classes, and
  a few utility wrappers. Some of those were pure compatibility shells rather
  than deep subsystem work.
- Decision: stage `core.envvars.EnvVars` compatibility wrapper, donor
  `Box86_64Preset`, and a lightweight local `Win32AppWorkarounds` shell so the
  remaining donor list reflects only genuinely nontrivial lanes.
- Tradeoff: this still leaves donor `Box86_64PresetManager` / rc parser, donor
  input-model classes, and donor app-wrapper classes open. That is acceptable:
  those are now the honest edge of the transfer batch, not forgotten easy
  wins.
- Verification: inventory diff only. The remaining donor code-file gap count
  dropped again, now to the compatibility/input perimeter. No compile, APK
  install, or device pass was run in this sub-pass by rule.
- Next step: decide whether the pre-compile boundary is strong enough for the
  first honest compile, or whether the remaining 12 donor files justify one
  more compatibility/import pass first.

### Entry 88: donor gap list collapsed to six non-foundational files

- Goal: close one more chunk of donor compatibility debt before the first
  honest compile so the remaining delta is clearly outside the core runtime
  stack.
- Context: after the previous pass, the donor gap list still contained the
  whole `box86_64` manager/rc package even though the payload assets were
  already local and the remaining work was mostly compatibility scaffolding.
- Decision: stage donor `Box86_64PresetManager` plus the `box86_64.rc` package
  locally, adapting storage to local `SharedPreferences` and local env-var
  wrappers instead of donor `PrefManager`.
- Tradeoff: this still leaves donor `PrefManager`, `ContainerData`,
  `NavigationDialog`, and the donor input-model classes out of tree. That is
  now acceptable: those six files form a bounded donor-app/input perimeter and
  no longer block the runtime-transfer claim.
- Verification: donor/local inventory diff only. The remaining donor code-file
  delta is now six files. No compile, APK install, or device pass was run in
  this sub-pass by rule.
- Next step: the tree is now at a defensible pre-compile boundary; the next
  rational move is the first honest compile rather than another bulk donor
  import.

### Entry 89: donor wrapper lane staged to zero file-gap

- Goal: close the remaining donor Java/Kotlin filename delta before the first
  honest compile.
- Context: the transfer batch had already absorbed most runtime foundation, but
  a small wrapper/app-model perimeter still remained visible in the inventory.
- Decision: stage local-compatible versions of `PrefManager.kt`,
  `container/ContainerData.kt`, `contentdialog/NavigationDialog.java`, and
  replace `xserver/XKeycode.java` with donor-shaped `xserver/XKeycode.kt`
  while preserving Java field access semantics.
- Tradeoff: `ContainerData.kt` is intentionally adapted without Compose
  saveable dependencies for now, because the local tree is not using the donor
  Compose editor surface before the first honest compile.
- Verification: file-level donor inventory now reports `MISSING_COUNT 0` under
  `GameNative/app/src/main/java/com/winlator` versus local
  `app/src/main/java/com/winlator/cmod`.
- Next step: record the zero-gap state in the transfer matrix and keep the
  remaining open tail focused on compile/runtime proof, not file presence.

### Entry 90: Kotlin build perimeter closure

- Goal: avoid a fake transfer closure where staged donor Kotlin files exist in
  tree but the app module is not even configured to compile them.
- Context: the donor-transfer lane now includes multiple Kotlin files, and the
  pre-compile state would stay dishonest if the module still exposed only a
  Java/Android plugin surface.
- Decision: enable `org.jetbrains.kotlin.android` in the app module and pin
  `kotlinOptions.jvmTarget = 17` to the existing Java level.
- Tradeoff: this widens the future build surface, but it is a necessary
  pre-compile closure step once donor Kotlin is staged locally.
- Verification: static diff pass clean; no build run yet by current rule.
- Next step: keep all remaining tails code/docs only until the user reopens the
  first honest compile.

### Entry 91: libwinlator_11 source-backed boundary written down

- Goal: make donor `libwinlator_11.so` handling explicit so the batch does not
  quietly slide into opaque binary adoption.
- Context: the donor binary clearly looks broader, but the reconstructible part
  of that breadth is already increasingly represented in local source and
  staged native helpers.
- Decision: add a dedicated audit note
  `docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md` and classify the donor binary
  as an audit/reference surface only. Source-backed imports stay preferred:
  `GPUHelper`, `xconnector_epoll`, `GPUImage`, helper libs, Vortek foundation,
  and stream-path parity.
- Tradeoff: slower than dropping in the donor library, but keeps `Ae.solator`
  maintainable and reviewable.
- Verification: docs updated and linked from the main transfer matrix and gap
  inventory; no compile run yet.
- Next step: finish the remaining documentation sync and then reopen the donor
  inventory one more time before the first honest compile.

### Entry 92: second broad GameNative sweep confirmed the remaining edge

- Goal: verify that the transfer batch had not quietly missed a useful donor
  runtime lane outside the classic `com/winlator/*` tree.
- Context: after the file-level parity pass reached `MISSING_COUNT 0`, the next
  realistic risk was not a missing `com/winlator` file but an overlooked
  `app/gamenative/*` subsystem tied to runtime/container delivery.
- Decision: run a second sweep across `app/gamenative/*`, `ubuntufs`, and donor
  build files; record the result in
  `docs/GAMENATIVE_SECOND_SWEEP_INVENTORY.md`.
- Tradeoff: this is inventory/documentation closure, not another blind import
  burst. That is intentional, because the remaining donor surfaces are now
  architectural lanes:
  storefront-aware routing, external display, Compose gesture UX, and
  dynamic-feature rootfs delivery.
- Verification: second sweep completed and written down; no compile run yet.
- Next step: carry the batch to the first honest compile boundary with these
  remaining lanes explicitly classified instead of hidden.

### Entry 93: donor Container contract was widened before the first honest compile

- Goal: remove the quiet mismatch where staged donor wrappers/data models
  expected a richer container contract than the live local `Container`
  actually provided.
- Context: `ContainerData.kt` and the second donor sweep both showed the same
  problem: local `Container.java` still lacked many donor runtime fields and
  JSON keys, so the batch looked broader on paper than in live state.
- Decision: rewrite local `Container.java` into a donor-compatible hybrid while
  preserving local `int id`, local manager wiring, local fullscreen flag, and
  local default values where they still matter.
- Tradeoff: this is a large source patch before compile, but it is still safer
  than compiling against a half-width container contract and discovering the
  break only after the batch is declared "done".
- Verification: static contract sweep only. The widened class now exposes the
  previously missing donor state and `git diff --check` stayed clean. No build
  run yet by rule.
- Next step: stage the low-dependency second-sweep donor classes that depend on
  this wider contract, then rerun inventory.

### Entry 94: low-dependency second-sweep donor foundation moved from paper to code

- Goal: reduce the second-sweep donor tail again before the first honest
  compile by staging the donor pieces that are runtime-relevant but not tightly
  coupled to donor storefront routing.
- Context: after the `Container` closure, the cleanest remaining donor pieces
  were gesture data, physical-controller handling, and the external-display
  input/swap/IME surface.
- Decision: stage local-compatible versions of `TouchGestureConfig`,
  `PhysicalControllerHandler`, `ExternalDisplayInputController`,
  `ExternalDisplaySwapController`, `ExternalOnScreenKeyboardView`,
  `IMEInputReceiver`, and `SwapInputOverlayView`, plus the supporting
  colors/strings.
- Tradeoff: these classes are staged as foundation only. They are not yet
  wired into the live app flow, because the current rule is still
  transfer-first, compile later.
- Verification: repeated donor parity under `com/winlator/*` remains
  `MISSING_COUNT 0`; second-sweep status now shows those low-dependency donor
  files as `STAGED`, with the remaining open tail reduced to
  `ContainerUtils`, `IntentLaunchManager`, `ContainerMigrator`, and
  `ContainerConfigTransfer`. `git diff --check` stayed clean.
- Next step: keep moving only on the genuinely high-coupled donor app-routing
  layer before the first honest compile.

### Entry 95: donor app-routing layer was staged through a local appId bridge

- Goal: close the last obvious second-sweep donor filename gap before the first
  honest compile.
- Context: donor `ContainerUtils`, `IntentLaunchManager`,
  `ContainerMigrator`, and `ContainerConfigTransfer` were the only explicit
  second-sweep files still marked open, but the real blocker was structural:
  donor code assumes storefront-style string `appId` identities while
  `Ae.solator` still uses numeric `ContainerManager` IDs.
- Decision: widen local `PrefManager` into a donor-compatible property
  surface, then stage local-adapted versions of the donor routing/config files
  on top of a `Container.sessionMetadata` bridge instead of mutating the local
  container identity model blindly.
- Tradeoff: this is a compatibility adaptation, not a verbatim donor copy.
  That is correct here because fake string-ID replacement would create more
  fragility than parity.
- Verification: new staged files are in tree, the second-sweep routing layer is
  no longer missing by filename, and `git diff --check` stayed clean. Build
  verification is still intentionally deferred by rule.
- Next step: rerun the written donor inventories and convert the remaining tail
  from "missing source files" into "compile/runtime proof plus rootfs archive
  ownership mapping".

### Entry 96: donor inventory moved from missing files to proof burden

- Goal: rewrite the donor status docs so they describe the actual remaining
  risk rather than stale pre-import gaps.
- Context: after staging the app-routing layer, the old docs still described
  `ContainerUtils`, `IntentLaunchManager`, `ContainerMigrator`, and
  `ContainerConfigTransfer` as open, which was no longer true.
- Decision: update the runtime gap inventory, second-sweep inventory, transfer
  matrix, roadmap, and `AGENTS.md` to mark the routing layer as staged, record
  the `sessionMetadata` bridge explicitly, and move the remaining tail to
  honest compile/runtime proof plus donor rootfs ownership work.
- Tradeoff: the written state is now stricter: it no longer flatters progress
  with fake closure, but it also stops pretending there are still missing donor
  files where there are none.
- Verification: documentation updated locally after a fresh staged-file sweep
  and clean `git diff --check`.
- Next step: if the user keeps the build lock in place, continue with donor
  rootfs archive ownership mapping; if the user reopens compilation, move to
  the first honest compile and runtime verification pass.

### Entry 97: rootfs ownership moved from archive folklore to an explicit table

- Goal: close the last non-code tail before the first honest compile by making
  the hybrid `imagefs` lane explicit about what each archive actually is.
- Context: the donor transfer batch had already staged installer, overlay,
  prefix, audio, and runtime-placement logic, but the written state still let
  `imagefs_*`, `extras.tzst`, and `redirect.tzst` blur together as if they were
  interchangeable rootfs pieces.
- Decision: add `docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md`, link it from the
  reverse map and hybrid plan, and classify `imagefs.txz`, donor `bionic`,
  donor `glibc`, patch/utility overlays, prefix/audio overlays, and the local
  `imagefs.txz.02` fragment.
- Tradeoff: this is documentation/inventory closure, not another code import.
  That is correct here because the remaining honest burden is compile/runtime
  proof and per-library adoption decisions, not more speculative transfer.
- Verification: ownership table written, roadmap/hybrid docs synced, and the
  orphan state of `imagefs.txz.02` is now explicit instead of implicit.
- Next step: rerun the donor/rootfs inventory one more time, then hold the lane
  at compile-boundary until the user reopens the first honest build.

### Entry 98: control inventory confirmed the rootfs lane is now compile-boundary honest

- Goal: verify that the new ownership write-up matched the live tree instead of
  becoming another optimistic doc layer.
- Context: after the ownership table landed, the remaining risk was simple but
  ugly: a stale asset list or a fake statement about `imagefs.txz.02`.
- Decision: rerun control inventory against local assets, grep the tree for
  `imagefs.txz.02`, and validate both archives with `xz -t` before declaring
  the documentation pass closed.
- Tradeoff: this is still not a compile. It is intentionally the last factual
  shell-level closure step before the first honest build is reopened.
- Verification: local asset inventory confirms
  `imagefs.txz`, `imagefs.txz.02`, `extras.tzst`, `redirect.tzst`,
  `container_pattern_common.tzst`, `container_pattern_gamenative.tzst`,
  `pulseaudio.tzst`, and `pulseaudio-gamenative.tzst`; `imagefs.txz` passes
  `xz -t`, `imagefs.txz.02` fails, and no live source references
  `imagefs.txz.02`.
- Next step: hold at compile-boundary and reopen only the first honest compile
  or the per-library hybrid rootfs adoption table, depending on user priority.

### Entry 99: donor rootfs became canonical and the hybrid split moved upward

- Goal: stop treating donor rootfs as a documentation idea and make the app
  itself speak the new contract before the first honest compile.
- Context: after the ownership and control-inventory passes, the biggest
  remaining lie would have been leaving `Ae.solator` on legacy `imagefs`
  assumptions while claiming that `GameNative` rootfs was the canonical base.
- Decision: stage donor-rootfs-first code paths:
  canonical donor archive downloads, `:ubuntufs` dynamic-feature scaffold,
  rootfs provider/layout markers, canonical `/tmp`, shared `imagefs/opt` for
  `Wine` / `Proton`, canonical `usr/local/bin/box64`, and compatibility
  bridges for `/usr/tmp` and `usr/bin/box64`. At the same time, add
  `docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md` so the base/overlay/helper/hold
  split is explicit at library level.
- Tradeoff: this is a broad structural patch before compile. That is
  intentional, because the user asked for full donor-rootfs adoption before the
  first honest build, not another half-step with legacy paths still baked in.
- Verification: static/documentation closure only; no build run yet by active
  rule.
- Next step: run the first honest compile/runtime proof for the donor-rootfs-
  first lane once the user reopens build verification.

### Entry 100: static audit cut through the donor-rootfs-first optimism

- Goal: answer the hard question before compile: did the donor-rootfs-first
  pass actually close the runtime split, or only move the code closer.
- Context: after the rootfs-first transfer, the repo could easily drift into a
  dangerous half-state where docs say `glibc` and `bionic` are cleanly split,
  while launch/runtime resolution still quietly operate on one mutable rootfs.
- Decision: run a static audit across assets, installer, container launch,
  launcher factory, `Contents` runtime resolution, and local feed metadata; add
  `docs/ROOTFS_RUNTIME_STATIC_AUDIT.md`; and record the honest blockers instead
  of pretending compile is already justified.
- Tradeoff: this slowed the push toward the first build, but it prevented a
  worse mistake: compiling a repo that still packages old `imagefs` baggage and
  still chooses launcher model from rootfs state rather than selected runtime
  contract.
- Verification: asset inventory confirms `imagefs.txz` and invalid
  `imagefs.txz.02` are still physically present; installer still hides legacy
  behind a marker rather than removing it; launch still does not call the
  variant-aware installer path with container context; `Contents` runtime
  resolution still scores by family/arch only.
- Next step: close those blockers in code, then rerun the inventory before the
  first honest compile.

### Entry 101: runtime model became a real contract instead of a naming habit

- Goal: close the static blockers without jumping prematurely into compile by
  making `runtimeModel` authoritative from feed normalization all the way to
  launch.
- Context: before this pass, donor-rootfs-first code still had one dangerous
  split-brain pattern: installer logic knew about `glibc` / `bionic`, but
  `Contents`, `WineInfo`, launcher selection, and container launch still had
  enough old heuristics to drift apart.
- Decision: add explicit `runtimeModel` handling to `ContentProfile`,
  `ContentsManager`, `GamehubFeedNormalizer`, and local `contents.json`; make
  `Wine` / `Proton` install roots canonical under
  `runtime-<model>-<family>-<version>-<verCode>`; enforce variant-aware rootfs
  preparation before `XServerDisplayActivity` continues; and make launcher
  creation depend on requested runtime model instead of ambient `imagefs`
  state.
- Tradeoff: this deliberately tightened behavior. Silent cross-family fallback
  between `Wine` and `Proton` was removed, which is harsher than the old
  resolver, but it is the correct tradeoff because the old behavior caused
  ambiguity and package duplication.
- Verification: `git diff --check` stayed clean; direct single-arg resolver
  tails were removed from the critical launch/dependency path; and the static
  audit findings for launch enforcement, runtime-model metadata, launcher
  routing, and `/opt` collision risk are now closed in code.
- Next step: rerun the inventory one more time, document the now-narrower
  residuals, and hold the batch at the first honest compile boundary.

### Entry 102: payload lanes and legacy rootfs baggage were cut to the real compile boundary

- Goal: close the remaining static tails before the first honest compile by
  fixing payload families outside `Wine/Proton` and physically removing legacy
  rootfs baggage from the live asset tree.
- Context: after Entry 101, the runtime-model lane was coherent, but two soft
  spots remained: `Vulkan SDK` could still be selected as unrelated per-arch
  crumbs, and `dgVoodoo` still depended on a weaker “some package exists”
  contract. At the same time, the old `imagefs` blobs were still physically
  sitting in `app/src/main/assets`, which is the kind of ugly residue that
  quietly crawls back into a build.
- Decision: move legacy `imagefs.txz` and `imagefs.txz.02` out of the asset
  tree into `.legacy_rootfs/`, remove live installer fallback to legacy
  rootfs, teach `ContentProfile` / `ContentsFragment` explicit architecture and
  bundle semantics, make `Vulkan SDK` selection group-aware in
  `XServerDisplayActivity`, synthesize `Vulkan SDK` / `dgVoodoo` profiles from
  extracted package structure, and tighten `dgVoodoo` dependency checks to the
  stage arch that will actually be used.
- Tradeoff: this is stricter than the old permissive path. That is the point.
  The previous behavior was tolerant in exactly the wrong places: mixed SDK
  groups, vague wrapper presence checks, and stale rootfs baggage parked inside
  live assets.
- Verification: static-only closure; legacy rootfs files are no longer under
  `app/src/main/assets`, runtime code no longer references legacy base
  archives, `git diff --check` remains clean, and payload lanes now have
  explicit static contracts instead of “works if profile.json happens to be
  there”.
- Next step: rerun the inventory one more time and then stop at the first
  honest compile/runtime proof boundary.

### Entry 103: the last live legacy rootfs marker path was removed

- Goal: eliminate the last code-level path where old rootfs metadata could
  still write or re-activate `legacy` provider/layout markers at launch time.
- Context: after Entry 102, base archives and installer logic were already
  donor-only, but `XServerDisplayActivity` still wrote provider/layout through
  a stale `isGameNativeRootfs()` branch, and `ImageFs` still had a read-time
  fallback that could surface `legacy` markers from old container metadata.
- Decision: make `XServerDisplayActivity` always write
  `gamenative` / `ubuntufs`, remove public legacy marker constants from
  `ImageFs`, and normalize any old `.provider` / `.layout` file values back to
  donor markers on read/write.
- Tradeoff: this is intentionally uncompromising. Old container metadata no
  longer preserves an observable “legacy mode” distinction. That is the right
  trade because the old distinction no longer matches the new rootfs contract.
- Verification: grep over app code no longer finds
  `ROOTFS_PROVIDER_LEGACY` / `ROOTFS_LAYOUT_LEGACY`, `git diff --check`
  remains clean, and the static inventory now records rootfs marker
  normalization as closed.
- Next step: stop the static lane here and treat the remaining work as
  compile/runtime proof rather than another paper cleanup pass.

### Entry 104: passive device forensics exposed a dead shell path and a sticky multitouch kill-state

- Goal: continue desktop debugging on the shared phone without foreground
  launching `Ae.solator` from the same Termux session, which contaminates the
  signal by stealing focus from Codex.
- Context: passive ADB forensics showed two concrete defects. First, the live
  rootfs contains `explorer.exe` but no `wfm.exe`, while local shell bootstrap
  and the `Computer` start-menu entry still pointed at `wfm.exe`. Second, the
  trackpad path in `TouchpadView` left `scrolling`/gesture state sticky after
  two-finger interaction and also kept an aggressive
  `suppressTrackpadUntilAllFingersUp` path that could freeze desktop input
  until every finger was lifted.
- Decision: remove the kill-state from `TouchpadView`, explicitly clear
  `scrolling` and `scrollAccumY` whenever multitouch collapses back below two
  fingers, keep compositor/X11 cursor ownership for the desktop shell, and
  replace hard `wfm.exe` assumptions with a runtime fallback that prefers
  `wfm.exe` only if it actually exists in the container and otherwise uses
  `explorer.exe`. The start-menu `Computer` entry was also switched to
  `C:/windows/explorer.exe`.
- Tradeoff: this favors a stable desktop-shell contract over blind donor
  fidelity. `GameNative` still references `wfm.exe`, but the live device rootfs
  does not ship it, so preserving that donor string would only preserve a dead
  path.
- Verification: passive forensic bundle and `run-as` inspection confirmed
  `explorer.exe` exists under the live prefix while `wfm.exe` does not; local
  app code no longer routes default shell bootstrap or the `Computer` menu
  entry through a guaranteed-missing binary; and the touchpad state machine no
  longer keeps desktop input muted after a two-finger gesture collapses.
- Next step: rebuild without foreground launch, install safely, and verify the
  shell/file-manager path plus cursor clicks on device with passive forensics.

### Entry 105: fresh post-install forensic caught a theme-registry crash before shell bootstrap

- Goal: validate container open on the rebuilt donor-rootfs batch with passive
  ADB forensics instead of foreground-launching the app from the shared Termux
  session.
- Context: the first fresh device pass after install still failed before the
  desktop shell came up. Crash buffer and the full forensic bundle
  `20260315_222602_container-open-failure-theme-npe` showed a
  `NullPointerException` in `WineThemeManager.apply()` while writing
  `Control Panel\\Desktop -> Wallpaper` through `WineRegistryEditor`.
- Decision: harden `WineRegistryEditor` so a missing/failed key creation no
  longer dereferences a null `Location`, and make `WineThemeManager` treat
  registry-write failures as non-fatal. Theme application should never be a
  crash gate for container launch.
- Tradeoff: if registry creation still fails in an odd container state, theme
  writes may be skipped for that pass. That is a better outcome than killing
  `XServerDisplayActivity` before the container even opens.
- Verification: full forensic capture completed, the exact Java stack was
  pinned to `WineThemeManager.java:56`, a new APK with the guardrails was built
  successfully, and that APK was reinstalled on the device without launching
  the app from this Termux session.
- Next step: clear logs, reproduce container open once more on the new build,
  and verify that the previous `WineThemeManager` crash is gone before moving
  back to cursor/file-manager tails.

### Entry 106: local no-ADB forensic moved from shell logcat to app-owned external traces

- Goal: keep autonomous device debugging viable after Wi-Fi `adb` disappeared
  and local Termux `logcat` stopped showing meaningful `Ae.solator` app-UID
  lines.
- Context: the first local shell bundle after a manual repro mostly captured
  `TermuxActivity` churn, not `com.winlator.cmod`. The real signal was still
  being written by the app into `/storage/emulated/0/Ae.solator/logs/`.
- Decision: treat external app-owned logs as primary truth on this ROM, add a
  process-wide uncaught-exception handler that writes `fatal_crash_*.txt`
  plus a synchronous `APP_FATAL_CRASH` forensic event, and bracket the narrow
  bootstrap gap after `FORENSIC_STREAM_HOOKS_READY` with new explicit markers.
- Tradeoff: this adds a little more disk I/O during fatal failures, but it is
  the only honest way to keep debugging without pretending shell `logcat`
  still sees the whole app.
- Verification: external forensic files were found under
  `/storage/emulated/0/Ae.solator/logs/forensics/`, and the latest repro was
  proven to stop right after `FORENSIC_STREAM_HOOKS_READY`, before the usual
  `orientation_gate` / runtime-prepared markers.
- Next step: rebuild, install the fresh APK, reproduce once more, then inspect
  `fatal_crash_*.txt` and the new post-hooks bootstrap markers before touching
  runtime routing again.

### Entry 107: the active native blocker is now narrowed to `libwinlator` preload or early `XServer` construction

- Goal: stop guessing about "container does not open" and pin the current
  failure to the smallest possible native/bootstrap surface before the next
  rebuild.
- Context: the latest external trace advanced beyond the earlier post-hooks
  gap. The app now reaches `XSERVER_BOOTSTRAP_PRELOADER_SHOW`,
  `XSERVER_BOOTSTRAP_INPUT_MANAGER_READY`, and
  `XSERVER_BOOTSTRAP_XSERVER_BEGIN`, then dies before
  `XSERVER_BOOTSTRAP_XSERVER_READY`. No guest runtime stream grows, and the
  process is gone afterward.
- Decision: centralize `libwinlator` loading through a new `WinlatorNative`
  loader with synchronous forensic checkpoints, preload it deliberately before
  `new XServer(...)`, add constructor-level checkpoints inside `XServer`
  itself, and strip host `RUNPATH` from native targets in CMake so the Android
  library perimeter stops carrying host linker baggage.
- Tradeoff: this adds more low-level instrumentation and slightly more disk I/O
  on the hottest bootstrap path. That is justified here because the current
  blocker sits before guest runtime startup, where ordinary runtime logs are
  still empty and asynchronous breadcrumbs can be lost.
- Verification: static pass is clean, legacy scattered `System.loadLibrary`
  calls have been replaced with the central loader, and the next rebuild will
  be able to distinguish `libwinlator` preload failure from later `XServer`
  constructor failure.
- Next step: rebuild, install, reproduce once, and inspect the new synchronous
  checkpoints to see whether the process dies inside `WINLATOR_NATIVE_LOAD_*`
  or after a specific `XSERVER_CONSTRUCTOR_*` milestone.

## 2026-03-18

### Entry 108: new-device bootstrap exposed host-LLVM drift and missing Wi-Fi ADB ergonomics

- Goal: make the freshly migrated Termux device use the pinned host compiler
  lane immediately and stop relying on remembered ADB-over-Wi-Fi command
  fragments.
- Context: the new device bootstrap succeeded only after manual intervention:
  the published host LLVM release tag had moved to
  `host-llvm-22.1.1-latest`, the archive unpacked into
  `llvm-22.1.1-termux-android-aarch64` instead of the local contract path, the
  bootstrap script skipped host LLVM fetch because it tested the fetch script
  with `-x` even though it was invoked through `sh`, and there was no local
  repo helper for `adb pair` / `adb connect` / debug APK install.
- Decision: harden the consumer fetch script around the latest release tag and
  archive-root normalization, teach the Termux bootstrap path to hydrate SDK
  command-line tools automatically when `sdkmanager` is absent, relax the host
  LLVM fetch check from executable-only to file presence, and add a dedicated
  `tools/adb-wifi-debug.sh` plus `docs/ADB_WIFI_DEBUG.md` for pairing,
  reconnect, install, and launch.
- Tradeoff: bootstrap now downloads a larger public Android SDK bundle on a
  truly fresh device, but the migration path is more honest and no longer
  depends on shell archaeology.
- Verification: the new device now has live `platform-tools`,
  `platforms;android-34`, `build-tools;35.0.0`, `ndk;29.0.14206865`, and a
  working host LLVM `22.1.1` lane under
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux`; local
  `adb start-server` / `adb devices -l` succeeds; and the new helper/docs are
  staged in repo.
- Next step: build a fresh debug APK on this migrated device, pair or reconnect
  the phone over Wi-Fi ADB through the helper script, then resume the live
  XServer bootstrap blocker with fresh on-device forensics.

### Entry 109: Codex operating rules and repo runbooks were realigned to the live build and forensic contract

- Goal: stop repo/process drift by encoding the user's approval-gated
  staff-review workflow in tracked docs while also updating the build and
  forensic runbooks to the current `com.winlator.cmod` / Termux / device state.
- Context: the repo had already grown strong local rules in `AGENTS.md`, but
  the user supplied a stricter review-mode prompt with explicit
  `APPROVED: IMPLEMENT` and `APPROVED: EXECUTE PLAN` gates. At the same time,
  several docs still described the old package id
  `by.aero.so.benchmark`, an outdated early-`XServer` failure point, and older
  local-build assumptions.
- Decision: add a canonical `docs/CODEX_OPERATING_CONTRACT.md`, mirror the
  approval-gated mode into workspace/repo `AGENTS.md`, and update the repo
  entry-point docs (`README`, docs index, Termux local build, migration
  bootstrap, Harvard ADB forensics, roadmap, SVG identity) so future Codex
  passes read one coherent contract instead of stale fragments.
- Tradeoff: the repo now carries a stricter documentation-sync burden, and
  future consultative/review requests will intentionally slow execution until
  the user opens the gates. That is acceptable because the alternative was
  process ambiguity and repeated repo drift.
- Verification: the canonical process doc is now tracked in-repo, the old
  package id was removed from the main entry points, the migration resume point
  now reflects the live Vulkan/wrapper failure chain, and the local build /
  forensic runbooks now point at the current Termux + Wi-Fi ADB workflow.
- Next step: finish the live runtime lane by proving that the freshly staged
  `libandroid-sysvshm.so` resolves the wrapper-side `libandroid_shm*` ABI
  expectation during a clean-session launch of `Container-1`.

### Entry 110: the wrapper ABI blocker was real, but the next tail was a donor absolute RUNPATH contract inside the installed runtime

- Goal: close the surviving container self-exit after the `libandroid_shm*`
  compatibility bridge landed and stop treating the old wrapper failure as the
  current blocker.
- Context: the rebuilt APK and fresh issue bundle proved that the old Vulkan
  wrapper chain was gone: the staged `libandroid-sysvshm.so` now exports both
  `shm*` and `libandroid_shm*`, the runtime no longer prints
  `libandroid_shmget` / `Found no drivers`, and bootstrap reaches the guest
  runtime streams. The next surviving failure is deeper:
  `winex11.drv` `PROCESS_ATTACH` returns `0`, `winewayland.drv` reports
  `status=c0000135`, and `nodrv_CreateWindow` still forces self-exit.
- Decision: inspect the pulled unix-side Wine ELFs directly and treat payload
  path hygiene as a first-class runtime contract. That static pass found stale
  absolute donor `RUNPATH` entries pointing at
  `/data/data/app.gamenative/files/imagefs/usr/lib` in `ntdll.so`,
  `win32u.so`, `winex11.so`, and `wineserver`. A new local Java-side
  sanitizer now rewrites absolute donor `RUNPATH` / `RPATH` entries in-place to
  a local `$ORIGIN` + relative imagefs `usr/lib` closure during runtime
  install and repair.
- Tradeoff: this adds a small ELF dynamic-section parser to the app and does
  one more filesystem hygiene pass over installed runtime roots. That is still
  the pragmatic choice here because the alternative was manual device-side
  `patchelf` surgery or repeated reinstallation without fixing the payload
  contract at its source.
- Verification: the runtime sanitizer is now wired into
  `ContentsManager.postProcessWineRuntimeInstall()` and the installed-runtime
  repair pass, so fresh installs and already-staged runtime roots go through
  the same repair logic.
- Next step: rebuild, reinstall, relaunch `Container-1`, and capture one fresh
  clean-session forensic bundle to confirm whether `winex11.drv` now survives
  past `PROCESS_ATTACH`.

### Entry 111: imagefs library closure and sanitizer marker tails are now closed; the blocker is a true winex11 attach-time failure

- Goal: stop treating rootfs payload hygiene as the active blocker once the
  installed runtime fix landed, and close the repeated full-tree sanitize tail
  that would have made every launch pay the same cost again.
- Context: fresh device proof after the first runtime-side sanitizer showed two
  concrete things. First, the remaining donor absolute paths were not only in
  `/opt/runtime-*`, but also throughout `imagefs/usr/lib`, including the X11
  closure (`libX11.so.6.4.0`, `libxcb.so.1.1.0`, `libXrandr.so.2.2.0`,
  `libxkbcommon.so.0.9.2`). Second, the initial imagefs sanitizer pass still
  reported `failed=1` because `imagefs/usr/lib/librt.so` is an 11-byte ASCII
  linker-script placeholder, not an ELF, so the marker file was never written
  and the app would rescan the tree on every launch.
- Decision: widen the Java-side sanitizer into `ImageFsInstaller` so the app
  repairs `imagefs/usr/lib` in-place during install and repair flows, add a
  versioned marker file under `.winlator`, and classify short/truncated
  non-ELF placeholders as `NOT_ELF` instead of `FAILED`. Keep the runtime-side
  sanitizer public so both `ContentsManager` and `ImageFsInstaller` share the
  same ELF rewrite logic.
- Tradeoff: startup still pays one broad first-pass scan when upgrading an old
  install, but subsequent launches stop rescanning the entire rootfs closure
  once the marker is written. That is the right trade here because path-hygiene
  repair is necessary exactly once per payload version, not once per launch.
- Verification: on-device proof now shows
  `files/imagefs/.winlator/.elf_runpath_sanitizer_version = 2`, the previous
  `failed=1` / `librt.so` tail is gone, and the imagefs X11/rootfs closure no
  longer depends on donor absolute paths. Fresh runtime logs on
  `wine_loader_2026-03-18_02-13-13.txt` still show
  `winex11.drv` loading successfully but failing during
  `MODULE_InitDLL(... PROCESS_ATTACH ...)`, followed by
  `winewayland.drv status=c0000135`, `nodrv_CreateWindow`, and
  `XSERVER_EXIT_REQUESTED`.
- Next step: instrument or statically audit the actual `winex11.drv`
  attach-time path. The active question is no longer payload closure or
  wrapper ABI, but what inside X11 driver initialization returns `0` after the
  unix-side driver is already loaded with a sane local library closure.

### Entry 112: the surviving bionic launch blocker was host-library shadowing, and the container now reaches live payload execution

- Goal: close the last real infrastructure blocker after the payload-side
  `RUNPATH` sanitizer landed and `winex11.drv` still failed during
  `PROCESS_ATTACH`.
- Context: static inspection of the live launcher path found a narrower bionic
  bug than the earlier donor-rootfs problem. `BionicProgramLauncherComponent`
  was prepending `files/imagefs/usr/lib/android-host` to `LD_LIBRARY_PATH`,
  which meant the android-host overlay copies of `libX11.so`, `libXext.so`,
  `libxcb.so`, and related X11 closure pieces could shadow the guest/runtime
  `usr/lib` closure that `winex11.so` was supposed to consume.
- Decision: keep the existing guest/runtime `LD_LIBRARY_PATH` head intact and
  append `android-host` as a fallback tail instead. Add a forensic marker
  `BIONIC_HOST_LIBPATH_ORDER_APPLIED` with the effective path head so future
  device traces can prove which ordering contract was used at launch time.
- Tradeoff: host overlay libraries remain available for bionic redirect mode,
  but only after the guest/runtime closure gets first resolution priority. That
  is the correct bias here because the host overlay exists to fill Android-side
  gaps, not to replace the Wine/X11 closure that ships with the selected
  runtime.
- Verification: fresh `assembleDebug`, reinstall on `10.0.0.1:40741`, and a
  clean `XServerDisplayActivity --ei container_id 1` launch on `2026-03-18`
  now emit `BIONIC_HOST_LIBPATH_ORDER_APPLIED` in `logcat`, show
  `MODULE_InitDLL(... winex11.drv ...) - RETURN 1` in the pulled
  `explorer_2026-03-18_09-38-38.txt` / `wine_loader_2026-03-18_09-38-38.txt`
  traces, and no longer reproduce the old
  `winewayland.drv status=c0000135` / `nodrv_CreateWindow` /
  `XSERVER_EXIT_REQUESTED` / `Found no drivers` chain. The same session keeps
  `wineserver`, `wfm.exe`, `war3.exe`, and `winecfg.exe` alive on-device.
- Next step: treat the remaining warnings as payload-specific tuning or
  optional-module follow-up, not as container bootstrap failure. The launch
  infrastructure blocker is closed.

### Entry 113: direct container launches now honor container framegen defaults, SoC classification is stable on SM8475, and Vulkan SDK proof is clean

- Goal: close the last misleading runtime-control tail after container bootstrap
  was already healthy: direct container launches were still silently falling
  back to global upscaler defaults instead of honoring `.container`
  `extraData`.
- Context: live forensic on `2026-03-18` showed an internal contradiction.
  `files/imagefs/home/xuser-1/.container` already carried
  `upscalerBackend=mobfgsr`, `upscalerFrameGeneration=1`, and
  `upscalerFramegenMode=low_latency`, but clean-session
  `UPSCALER_ROUTE_APPLIED` still reported `backend=off`. In the same traces the
  device also fell back to `soc_class=adreno-6xx-and-older` despite running on
  `SM8475 / taro`.
- Decision: parse upscaler/framegen launch settings for every container launch,
  not only when a shortcut is present; add source markers
  (`backend_source`, `preset_source`, `framegen_source`,
  `vk_validation_source`) to `UPSCALER_ROUTE_APPLIED`; and centralize SoC
  detection in `SocClassifier` with a Qualcomm platform fallback for renderer
  strings that omit the Adreno generation.
- Tradeoff: the app now trusts `.container extraData` during direct launches in
  the same way it already trusted shortcut overrides. That is the correct
  contract because container defaults are the only persisted runtime policy
  source when a user launches the container shell directly.
- Verification: fresh unit tests now cover `Adreno 730`, `SM8475/taro`,
  `Adreno 650`, `Mali-G715`, and `Xclipse 920`. Fresh device bundle
  `20260318_101550_direct_container_upscaler_retest` proves:
  `GRAPHICS_ROUTE_APPLIED` still resolves
  `VulkanSDK-1.4.341.1-arm64-1`, `UPSCALER_ROUTE_APPLIED` now reports
  `backend=mobfgsr`, `backend_source=container`, `framegen_enabled=1`,
  `framegen_source=container`, `preset_source=container`,
  `soc_class=adreno-7xx`, and the prior regression chain
  (`libandroid_shmget`, `Found no drivers`, `nodrv_CreateWindow`,
  `XSERVER_EXIT_REQUESTED`) remains absent.
- Remaining tail: the rootfs still contains no file matching `mobfgsr`,
  `dlssg`, or `fsr3`, so the runtime-payload consumption gate for rounds
  `R10-R14` is still genuinely open. The app-side contract is now correct and
  provable; the provider-side payload is still missing from the staged rootfs.

### Entry 114: the current runtime clean-pass is now about UI truthfulness and installer dispatch semantics, not bootstrap

- Goal: fold the latest marked screenshot batch and fresh `Prefix Pack`
  forensic into one coherent closure slice instead of treating them as
  unrelated nits.
- Context: the newest user-visible defects were not generic Android issues.
  They were precise surface mismatches: `Runtime Profiles` still showed the old
  dual-Box/FEX layout in the live build; runtime `Logs` still looked like a
  striped spreadsheet instead of the main forensic console; `Screen Effects`
  still burned too much vertical space on the profile row; `Task Manager` was
  already nearly done but still needed safe last-row slack; and `.NET`
  installer hand-off from `Prefix Pack` did launch a real GUI installer, yet
  the app kept treating that path as unproved and retried candidates anyway.
- Decision:
  `Runtime Profiles` is now route-aware in code and only surfaces the active
  family (`FEXCore` for `arm64ec`, `Box64` for `x86_64`) with runtime-colored
  spinners and compact version/preset rows;
  `Prefix Pack` direct GUI lanes now short-circuit the old detached-proof retry
  loop by writing a fresh launcher proof log as soon as detached guest launch
  succeeds; `Task Manager` bottom slack is now adaptive rather than a fixed
  dead tail; and roadmap upkeep is now explicit session discipline instead of
  a best-effort note.
- Tradeoff: direct GUI installer lanes now bias toward user-visible hand-off
  truth over aggressive automatic proof heuristics. That is the correct bias
  here because repeated retries were worse than a conservative `interactive`
  state when the installer was already on screen.
- Next proof burden: reinstall this batch on device and validate the newest
  live surfaces against the latest screenshot queue, especially
  `Runtime Profiles`, runtime `Logs`, `Screen Effects`, `Task Manager`
  bottom-row geometry, and `Prefix Pack` one-shot `.NET` launch.

### Entry 115: Ajay Prefix Pro v1.6 is now a verified donor base, but it does not replace Ae.solator's stricter install/state contract

- Goal: stop treating Ajay as a vague community reference and turn it into a
  verified donor baseline with explicit strengths, limits, and local paths.
- Context: the user asked for a wider donor search and for the latest Ajay
  prefix to be installed as a donor base. At the same time the active
  screenshot + forensic queue kept proving that our current blocker is not
  generic `exe` launch failure, but the much narrower
  `DXSDK Jun10 -> legacy .NET 2.0 -> error 51023` path plus live
  `Prefix Pack` UX tails.
- Decision: pull a broad GitHub donor pool first, then verify the latest
  official Ajay offline release directly from GitHub Releases, download the
  archive, validate its hash, fully extract the offline package, and inspect
  the nested `Setup`, `Start Menu`, and `Resources` payload layers before
  deciding what should be imported into `Ae.solator`.
- Verification: `Ajay Prefix Pro v1.6 Offline` is now staged under
  `/data/data/com.termux/files/home/donors/ajay-prefix/v1.6_offline`,
  archive hash
  `e4a23f89c8cc5944b87d7228d04a820e659b494a7e230498910f2c93a2305aa6`
  matches the official release metadata, and the extracted donor proves real
  coverage for:
  `Only Start Menu / Prefix / Both` install modes,
  visible save-data redirection,
  `Necessary_Components.bat`,
  `PhysX`, `XNA`, `OpenAL`, `FAudio/XAudio`, `VC` registry helpers,
  `DgVoodoo`, `DXVK/VKD3D` references, Wine tools, and GPU/API tests for
  `D3D8/9/10/11/12`, `DDraw`, `OpenGL`, and `nGlide`.
- Tradeoff: Ajay is strong on breadth and legacy helper scripts, but weak as a
  source-of-truth for our current installer closure model. Its scripts still
  rely heavily on direct `Start <exe>` launches, temp extraction, and looser
  state assumptions, which is exactly where `Ae.solator` needs stricter proof.
- Conclusion: Ajay is now the primary donor for coverage, test inventory,
  start-menu grouping, and script ideas; it is not the source-of-truth for the
  `Prepare -> Install -> State/Logs` contract or for resolving the current
  `DXSDK/.NET 2.0` proof-token gap. Those stay app-owned in `Ae.solator`.

### Entry 116: donor transfer can be UI-only, and the management surfaces need a stricter split-pane contract than Ajay or the older runtime dialogs

- Goal: honor the user's narrowed scope and transfer donor value only into the
  Android-side management surfaces, without reopening the start-menu lane or
  mixing in premature build/device claims.
- Context: after the broader donor audit the user explicitly constrained the
  active pass to `UI contract management only, no start menu`, while the latest
  screenshot batch still showed heavy `Prefix Pack` chrome, stacked runtime
  cards, and a nearly-finished `Task Manager` whose only remaining geometry
  risk was the last Linux row.
- Decision: treat that wording as a hard scope wall. Import donor structure,
  not donor volume:
  use split-pane management geometry, denser rows, slimmer headers, and a
  route-owned `Runtime Profiles` surface; keep `Task Manager` changes to the
  last-row guard only; and defer compile/device proof until the whole UI slice
  is ready because the user explicitly asked for no intermediate builds.
- Tradeoff: this pass does less immediate runtime validation in exchange for a
  cleaner batch boundary. That is acceptable because the user asked for one
  cohesive UI-only donor transfer rather than another half-built build/install
  loop.
- Next proof burden: when this donor-driven UI batch is complete, install once
  and validate the touched surfaces against the freshest user screenshot queue:
  `Prefix Pack`, `Runtime Profiles`, `Screen Effects`, `Task Manager`, and the
  shared runtime log/debug surface.

### Entry 117: once the user widened the donor UI pass again, the safe expansion path was adjacent management surfaces, not weaker install logic

- Goal: expand the current no-build donor batch without diluting the stricter
  runtime-management contract that the previous entries had just established.
- Context: after narrowing the pass to management UI only, the user explicitly
  allowed the scope to sprawl again, but only on the condition that logic
  would not degrade. That changed the problem: the scope wall was no longer
  absolute, but the contract wall became stricter.
- Decision: widen only along adjacent management surfaces that share the same
  runtime chrome family:
  nested preset editors behind `Runtime Profiles`,
  `Container Storage Info`,
  `Input Controls`,
  and the shared split-pane geometry language.
  Do not use that wider scope as permission to reopen start-menu cloning or
  relax `Prepare -> Install -> State/Logs` proof semantics.
- Tradeoff: the batch touches more XML/Java surfaces before the next build, so
  closure depends even more on a single careful final proof cycle. That is
  still the better trade because the user explicitly rejected intermediate
  build churn and the touched surfaces now form one coherent runtime-management
  family.
- Next proof burden: validate that the widened family still reads as one
  product on device:
  `Prefix Pack`, `Runtime Profiles`, preset editors, `Screen Effects`,
  `Container Storage Info`, `Task Manager`, and the runtime log/forensic
  dialogs must all hold the same contrast, spacing, and control logic without
  regressing installer or state semantics.

### Entry 118: the widened management pass still has to carry real installer truth, and the auxiliary Prefix Pack dialogs cannot fall back to old chrome

- Goal: keep the broadened UI-only batch honest by folding the freshest
  screenshot truths back into both layout work and the active logic tail list.
- Context: the latest manual screenshots (`2026-03-21 11:22-11:24`) did two
  things at once:
  they confirmed that the visible cache contract is real because
  `DXSDK_Jun10.exe`, `PhysX`, `Wine Gecko x86/x64`, and `Wine Mono` are all
  physically visible under `C:\AePrefixPack\cache`,
  and they also proved that the legacy DirectX path is still blocked by the
  live `DXSDK Jun10 -> .NET Framework 2.0 redist -> error 51023` chain.
- Decision: keep that logic tail live in the roadmap while continuing the
  widened management pass. Do not pretend a cache-visible installer is the same
  thing as a solved install flow. At the same time, bring the remaining
  `Prefix Pack` auxiliary dialogs (`Graphics Diagnostics`, lane `Info`) onto
  the same split-pane runtime-management contract so the toolkit stops
  regressing into older badge/button-wall chrome once the user drills deeper.
- Tradeoff: this increases the breadth of the no-build batch again, but still
  respects the user's rule because the expansion stays in adjacent management
  surfaces and does not relax `Prepare -> Install -> State/Logs` semantics.
- Next proof burden: once the management-family pass is complete, run one fresh
  device cycle and demand both truths at once:
  newer live screenshots must show the auxiliary dialogs on the same product
  surface, and a new forensic bundle must prove whether the `51023` path is
  closed or still the real blocker.

### Entry 119: the remaining legacy installer tail is a managed-runtime routing problem, not a generic cache or launcher problem

- Goal: collapse the last donor-backed installer tails into one coherent
  lane-owned contract before the next build/install proof cycle.
- Context: the freshest `2026-03-21 11:18-11:24` screenshots and
  `live_verify_20260321_122023_legacy_dxsdk` bundle already proved that the old
  model was wrong twice:
  `legacy_dx_sdk` was still redirecting into `dotnet_framework`, and `xna` was
  still being treated like a `.NET 4` problem even though Ajay's own XNA
  launchers call out `Wine Mono` first.
- Decision: keep `.NET Framework` as its own honest lane, but remove the last
  surprise redirects from adjacent lanes. `legacy_dx_sdk` stays lane-owned
  under the Mono/DLL-override guard, while `xna` repairs `Wine Mono` in-lane
  before running the XNA MSI payloads. The staged dispatch helper now also
  hides its own console so blank `cmd` windows stop polluting runtime UX.
- Tradeoff: this widens the no-build batch again, but still stays inside the
  user's scope wall:
  runtime management UI and installer truth only, no start-menu sprawl and no
  weakened `Prepare -> Install -> State/Logs` semantics.
- Next proof burden: the next closure cycle has to beat the historical control
  bundle. Success means:
  `legacy_dx_sdk` no longer leaves a fresh `dotnet_framework` redirect trace,
  `xna` no longer jumps into `.NET 4`,
  and `PhysX` / `GLview` no longer strand the user in empty helper windows plus
  a vague `queued` state.

### Entry 120: the pass needs a hard hold on the exact DXSDK continuation point so the next agent does not waste the one-build budget

- Goal: freeze the current omega pass on the real remaining blocker and make
  continuation deterministic even after a pause.
- Context: by the time the user asked for a hard hold, the pass had already
  consumed the single allowed final compile. Fresh screenshots then narrowed
  the live truth further:
  `legacy_dx_sdk` is no longer failing at the outer launcher layer. The lane
  can now open the real DirectX SDK setup and reach `Copying Files`, but then
  hangs or falls back into the legacy `.NET 2.0 / 51023` family.
- Decision: record a dedicated checkpoint and explicitly forbid the usual
  restart mistakes. The next resume must not begin with another compile,
  another donor sweep, or another broad UI pass. It must begin with a live
  rootfs sync of the newer unrebuilt
  `install-directx-sdk-tools.cmd`, then one focused `legacy_dx_sdk` rerun with
  fresh forensics.
- Tradeoff: this leaves one known script patch unapplied inside the live APK
  state until the next resume. That is still the right trade because it
  preserves the user's one-build constraint while keeping the next action
  precise and cheap.
- Next proof burden: once `adb` is restored, prove or disprove the patched
  live lane in one narrow cycle:
  rootfs sync -> rerun `legacy_dx_sdk` -> capture fresh state/log/screenshot.

### Entry 121: the Android build lane was cache-capable but still structurally non-reproducible, so the fix had to remove mutation and split authority instead of chasing cache folklore

- Goal: close the repo-local Android build audit instead of merely describing
  it.
- Context: the root Gradle lane could already store and reuse configuration
  cache entries, but the stronger audit showed that the real drift was
  elsewhere:
  `preBuild` still reached donor download/mutation logic,
  `app/build.gradle` still performed host-specific NDK/signing checks during
  eager configuration,
  and the repo still carried nested wrapper/property lanes that could
  undermine a single authoritative build path.
- Decision: fix the lane structurally.
  `preBuild` now verifies bundled imagefs/runtime assets and generated JNI
  state instead of downloading donor archives.
  NDK runtime resolution now happens through a task-time resolver rather than
  one eager `linux-x86_64` hard-code.
  Nested `app/gradle.properties` was neutralized and the nested wrapper was
  realigned to the root wrapper version.
- Tradeoff: this keeps an explicit manual donor-rootfs hydration task around,
  but only as a non-authoritative helper. That is the right trade because it
  preserves operator escape hatches without letting the default build lane
  mutate the source tree.
- Next proof burden: run the root lane again and demand the stronger truth:
  cache reuse still works,
  `preBuild` no longer drags network mutation into the task graph,
  and the remaining build tail is reduced to narrower Gradle/AGP cleanup
  instead of host-drift folklore.

### Entry 122: browser-interface migration restored full Codex context from split backup + rollout archives

- Goal: rehydrate operational memory and execution posture after moving from
  CLI sessions to browser sessions without losing Chapter 2 continuity.
- Context: external network egress in the runtime environment was proxy-blocked
  (`403 CONNECT`) for Drive/Mega/apt, so context restoration had to come from
  local split archives staged in repo root.
- Decision: reassemble and unpack `backup.zip.001..003` and
  `rollout.zip.001..011`, then rebuild active context from those artifacts
  before any new implementation pass.
- Tradeoff: ingesting very large rollout history is slower than starting a new
  lane, but it preserves contract truth, decision lineage, and defect-class
  memory, which is mandatory for Black Diamond closure quality.
- Verification: archive listing and extraction succeeded, and latest rollout
  stream confirms active forensic-led continuation context.
- Next step: run the next implementation pass against fresh user-sent forensic
  bundles, with `wcp-runtime-lanes` GitHub releases treated as the runtime
  build source of truth and no-`adb` intake as the default until restored.
