# AGENTS

## Role

This repository is the source-of-truth for the Ae.solator Android application.
This agent operates here as the second autonomous developer focused on app/UI,
runtime-binding, documentation alignment, and repository contract integrity.

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
- Do not store secrets, access tokens, cookies, or credentials in tracked files,
  docs, prompts, patches, or commit messages.
- GitHub authentication must come from local runtime state only
  (`GITHUB_TOKEN`/credential helper/local shell session), never from repository
  files such as `AGENTS.md`.
- Local host compiler lane is pinned to `LLVM 22.1.1` when a dedicated
  source-built toolchain is present under
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux`.
  Treat older Termux LLVM packages only as bootstrap tools, not the target
  compiler baseline for future local `wine` or runtime builds.
- The preferred heavy build path for the host compiler lane is GitHub Actions,
  not on-device compile. Publish `LLVM 22.1.1` as a separate host-toolchain
  release asset and hydrate it locally through
  `tools/fetch-host-llvm-release.sh` using `GITHUB_TOKEN` or
  `AEO_GITHUB_TOKEN` from runtime state, never from tracked files.
- Every coherent implementation pass should end with a git commit unless the
  user explicitly asks to keep the tree uncommitted.
- If the user explicitly says not to compile or build until a transfer lane is
  complete, do not run `gradlew`, Android builds, or APK installs in the
  middle of that lane. Keep transfer passes code-and-docs only until the user
  reopens verification.
- If the user says the full donor transfer should be one batch, keep the whole
  active donor-transfer lane as one cumulative commit target instead of
  fragmenting it into per-subsystem commits. Stage and document intermediate
  steps, but do not cut a new transfer commit until the batch reaches an
  agreed closure point.

## Working Contract

- Maintain the execution roadmap in `docs/SECOND_DEV_ROADMAP.md`.
- Maintain a reflective work journal in `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`.
- Maintain donor binary/source boundary notes in:
  `docs/GAMENATIVE_RUNTIME_GAP_INVENTORY.md` and
  `docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md`.
- Keep documentation aligned with actual code, runtime contracts, and split repo
  ownership before calling a task complete.
- Report changes to the first developer as concise implementation notes with:
  scope, files touched, contract impact, verification status, and open risks.
- Do not leave accidental build-side tails in the tree; restore or explain
  every unexpected delta before handoff/commit.
- For every substantial change, record:
  goal, context, decision, tradeoff, verification status, and next step.
- Prefer reflective, contract-first integration over blind donor copying or
  cosmetic-only edits without documentation sync.
- Treat donor repositories such as `Gamehub-Components` and
  `BannersComponentInjector` as audited input, not as automatic source-of-truth:
  borrow feed logic, UX patterns, and package-link discovery only after
  verifying they fit `Ae.solator` provenance and install contracts.
- Treat `GameNative` as the primary donor reference for desktop cursor/input
  semantics. If `Ae.solator` desktop taps stop hitting live shell targets,
  compare `TouchpadView` / pointer dispatch against `GameNative` before
  inventing new async cursor queues or touch-surrogate fallbacks.
- When a donor repo appears to expose more package links than the current app,
  document the delta first, then integrate it behind explicit source labeling
  and install verification instead of silently reshaping existing lanes.
- Treat `GameNative` donor work as a layered audit, not a bulk merge. For any
  X11 / renderer / driver pass, classify donor code into three buckets first:
  `import now`, `adapt later`, and `do not blind-copy`, then record that in
  `docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md` before widening the patch.
- For `GameNative` imports, prefer foundational infrastructure over surface
  churn: GPU/Vulkan probe helpers, fd/socket hygiene, renderer plumbing, and
  component install routing come before wholesale UI or gesture-stack copying.
- Treat a user request for a "full transfer" as a no-partial-closure lane:
  keep moving subsystem by subsystem, but do not reset the lane to
  UI-only/device-debug-only work until the written transfer matrix says the
  requested donor execution stack has actually been imported or consciously
  held.
- Donor manifest-driven package installs must reuse the local trusted install
  bridges:
  `ContentsManager.extraContentFile -> finishInstallContent` for content
  payloads and `AdrenotoolsManager.installDriver()` for graphics drivers.
  Do not introduce a parallel raw-extract path that bypasses existing trust,
  staging, or metadata persistence.
- A broad `GameNative` transfer request must first become a written transfer
  matrix in `docs/GAMENATIVE_FULL_TRANSFER_MATRIX.md`. Track donor subsystems
  there by exact source files and one of:
  `already imported`, `next import`, `adapt later`, `hold`.
- When the user asks for a "full transfer", treat that as a runtime-wide scope:
  payload/install logic, package placement, container routing, launcher flow,
  Wine/Proton handling, X11/renderer/driver policy, and supporting docs.
  Do not collapse that request into just UI or just X11.
- Treat donor rootfs work as its own lane. `GameNative` `ubuntufs` /
  `imagefs_*` archives are input for a hybrid `Ae.solator` rootfs, not a blind
  replacement. Before changing `imagefs`, record:
  donor archive source, overlay archives, preserved paths, library deltas, and
  which layer each file belongs to: base rootfs, overlay, runtime payload, or
  container-time mutable state.
- Rootfs-transfer closure is not just `imagefs_*` extraction. Do not call the
  lane ready for compile until these donor-linked pieces are accounted for
  together and documented:
  base archive delivery/fallback, `imagefs_patches_gamenative.tzst`,
  `container_pattern_gamenative.tzst`, donor pulseaudio overlay, and
  `prefixPack.tzst` / `prefixPack.txz` fallback handling.
- Before the first honest post-transfer compile, keep
  `docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md` current and classify every live
  rootfs-linked artifact as one of:
  base rootfs, overlay, runtime payload, prefix/bootstrap layer, or orphan
  baggage. If a fragment such as `imagefs.txz.02` exists but is unreferenced
  and fails archive validation, document it as orphan data instead of quietly
  treating it as a valid shard.
- When donor rootfs becomes canonical, keep
  `docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md` current too. Record which
  concrete libraries and tools come from donor base, donor overlays, APK guest
  helper mirroring, local compatibility bridges, or legacy hold. Do not call
  the rootfs lane compile-ready until both the layer table and the per-library
  table agree with the staged code.
- Do not leave live `legacy` provider/layout behavior in the runtime path.
  If old `.provider` / `.layout` markers are encountered, normalize them to
  `gamenative` / `ubuntufs` instead of letting old rootfs state silently
  re-activate in launch code.
- Runtime-package transfer closure is not just feed parsing. Before calling the
  donor runtime lane ready for compile, account for all three runtime-placement
  facts together:
  package-root resolution from `wineBinPath` / `wineLibPath` /
  `winePrefixPack`, post-install `lib/wine` normalization, runtime-root
  `prefixPack` availability, and executable-bit repair for installed `Wine` /
  `Proton` binaries.
- Runtime-package closure also requires one explicit metadata contract:
  `runtimeModel`. Do not treat `Wine` and `Proton` payloads as disambiguated by
  filename folklore alone. Keep `Contents`, `ContainerDetailFragment`,
  `ImageFsInstaller`, `XServerDisplayActivity`, `WineInfo`, and
  `GuestProgramLauncherFactory` aligned on the same `glibc` / `bionic`
  runtime-model field or inference rule.
- Do not reintroduce silent cross-family fallback between `Wine` and `Proton`
  when resolving installed runtimes. If a requested `Proton` package is not
  present, that is a missing-runtime state, not permission to silently resolve
  it as `Wine`, and vice versa.
- `Vulkan SDK` is not a random per-arch pile. Keep it as one coherent
  version-group coverage contract: launcher selection must prefer one SDK
  group, report whether the selected layout is `bundle`, `split`, or `single`,
  and avoid mixing unrelated versions across architecture lanes.
- `dgVoodoo` runtime closure requires architecture-aware validation. Dependency
  checks and stage-time routing must validate the arch that will actually be
  staged, not merely that some `dgVoodoo` package exists somewhere in
  `Contents`.
- Legacy rootfs archives are no longer allowed inside `app/src/main/assets`.
  If old `imagefs` payloads must be kept for archaeology, move them outside the
  asset tree and document them as archived residue rather than live APK input.
- Runtime transfer closure is also not code-only. Before calling the donor
  runtime lane ready for compile, check that the donor payload lanes are
  actually staged locally too:
  `graphics_driver`, `dxwrapper`, `fexcore`, `wowbox64`, `steampipe`,
  `wincomponents`, `box86_64`, `steaminput`, `steam_regions.json`, and
  `box86_env_vars.json`.
- Donor `libwinlator_11.so` must not be adopted as a blind binary upgrade.
  If donor-native behavior appears stronger, first record the source-backed
  reconstruction status in `docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md`,
  then transfer only the reconstructible pieces into local source/assets.
- Before the first honest post-transfer compile, rerun a file-level donor
  inventory under `GameNative/app/src/main/java/com/winlator` versus
  `aesolator/app/src/main/java/com/winlator/cmod` and record the result in
  `docs/GAMENATIVE_RUNTIME_GAP_INVENTORY.md`.
- Before the first honest post-transfer compile after the broad
  `app/gamenative/*` sweep, record which second-sweep donor foundations are
  already staged locally versus still open. At minimum classify:
  widened `Container` state, `TouchGestureConfig`,
  `PhysicalControllerHandler`, and the external-display foundation separately
  from the donor app-routing layer
  (`ContainerUtils`, `IntentLaunchManager`, `ContainerMigrator`,
  `ContainerConfigTransfer`).
- When donor container-routing code expects storefront-style string `appId`
  identities but the local tree still uses numeric `ContainerManager` IDs, do
  not mutate the local identity model blindly. Bridge donor `appId` semantics
  through `Container.sessionMetadata` first, then document the adaptation in
  the runtime gap inventory and second-sweep inventory.

## Execution Priority

- When the user sends many goals in one burst, normalize them into an ordered
  backlog in `docs/SECOND_DEV_ROADMAP.md` instead of switching reactively
  between unrelated tasks.
- Default execution order:
  1. close requested product/UI/content tasks,
  2. close documentation and handoff tails,
  3. run debugging/forensics after the current list pass is closed.
- Exception: if a crash, build failure, or runtime defect directly blocks the
  next listed task, fix that blocker first, then return to the list.
- Do not lose deferred work:
  every context switch must leave a written note in the reflective journal with
  the blocked item, why it was deferred, and what resumes next.
- Keep one active implementation lane at a time and explicitly mark the rest as
  queued, not forgotten.
- Before ending a pass, ensure the working tree, roadmap, and reflective
  journal all agree on what is done, what is open, and what is next.

## Mobile UI / Device Pass

- Treat real device screenshots as UI ground truth whenever polishing mobile
  surfaces or validating a suspected visual regression.
- Use an iterative ADB loop for UI work:
  capture screen, interpret layout, identify interaction target, execute the
  next action, capture the new state, then update the working hypothesis.
- When running an unattended device pass, reserve a short exclusive device
  window first. If foreground interference from Termux/Telegram/launcher
  invalidates the pass, record that as test contamination in the journal
  instead of treating it as app proof.
- Prioritize concrete UX issues over vague styling notes:
  hierarchy, spacing rhythm, readability, tap target clarity, wording, action
  discoverability, and flow friction.
- When a UI fix also touches runtime stability, close the crash tail first,
  then return to the visual pass on the stabilized screen.
- For meaningful device-led passes, record:
  screen observed, issue found, code decision, verification evidence, and any
  remaining risk for the next pass.
- When the screen is unfamiliar or the user asks for broad UX cleanup, run a
  compact audit cycle:
  screen identification, layout map, detected controls, likely interactions,
  visual hierarchy, UX/accessibility issues, implementation decision, and next
  ADB action.
- Prefer global control-system fixes over one-off pixel edits when the same
  issue appears across activities, dialogs, selectors, settings rows, or
  preference surfaces.
- For runtime/install/container blockers, capture the full forensic bundle
  before changing code:
  `logcat`, in-app forensic jsonl, current screenshot/UI dump, and a
  `run-as` snapshot of `files/contents` / relevant app-private state.
- Configuration dialogs must not depend on crash-prone native probes during
  open/render. If runtime/native discovery is unstable, move the heavy probe
  out of the dialog path, use a safe cached or catalog-backed fallback, and
  log which data source populated the UI.
- When simulating a device flow such as `Contents -> install -> New Container`,
  do not stop at the first symptom; carry the pass through until the expected
  local package root, UI state, and downstream consumer (`SWineVersion`,
  runtime picker, driver picker, etc.) either agree or produce a recorded
  mismatch with evidence.
- For no-shortcut container desktop launches, verify both layers of shell
  readiness:
  1. app-side bootstrap command / deferred-exit flow,
  2. prefix-side registry contract under
  `HKCU\Software\Wine\Explorer\Desktops\shell` (`EnableShell`, geometry,
  systray/taskbar intent).
  A mapped `explorer.exe` window without those registry keys is not enough to
  call the desktop fixed.
- For no-shortcut desktop sessions, verify the input contract separately from
  shell readiness:
  a live desktop is not complete unless the session enters an explicit pointer
  interaction model (`simulateTouchScreen`, cursor-touchpad, or equivalent)
  and accepts real pointer hits on device, not just window maps in forensic
  logs.
- Default no-shortcut desktop behavior should prefer a visible cursor with
  cursor-touchpad semantics over a touch-surrogate cursor that jumps directly
  to the finger. If the user asks for desktop-style interaction, do not let the
  cursor merely duplicate the touch point.
- For desktop cursor work, validate the transport layer separately from shell
  readiness: a direct `xServer.injectPointer*` probe proving `Start` opens is
  not enough. Closure requires the same hit target to open through the live
  `TouchpadView` path as well.
- If desktop icons such as `Computer` fail while `Start` and ordinary windows
  still open, treat that first as a desktop-icon double-click contract issue,
  not as a missing runtime/package issue. Verify it with a two-tap probe
  against the live shell before touching `Contents`. Do not keep experimental
  anchored-tap targeting in `TouchpadView` unless it survives a live device
  pass without degrading ordinary desktop clicks.
- Treat duplicate-cursor reports as cursor-ownership bugs, not mere cosmetics.
  Inspect at least these layers before changing behavior:
  Android/system pointer icon, `GLRenderer` root/X11 cursor fallback, and any
  guest-owned software cursor in fullscreen/non-shell application windows.
- When a duplicate cursor appears inside a fullscreen-like guest window, do not
  limit the fix to `rootCursorDrawable`. Audit the whole compositor cursor path
  in `GLRenderer.renderCursor()`, including `pointWindow.attributes.getCursor()`,
  because guest-owned software cursors can duplicate against both the root
  fallback and the normal X cursor layer.
- On this shared Termux device, do not treat a screenshot as ground truth for a
  Winlator desktop pass unless `dumpsys activity top` still shows
  `XServerDisplayActivity` in foreground at the time of capture. If `Termux`
  or the launcher regains focus first, mark the screenshot contaminated and use
  log/forensic evidence instead of drawing UI conclusions from the image.
- For the default no-shortcut desktop path, treat input closure as two separate
  requirements:
  1. cursor movement remains desktop-like and does not mirror every touch point,
  2. a single tap still produces a click at the current cursor location unless
     the user explicitly asks for touch-surrogate behavior.
  If the cursor moves but desktop controls stay inert, fix the trackpad-style
  click contract before revisiting runtime/bootstrap code.
- For no-shortcut desktop sessions, prefer a single visible cursor owner:
  keep the shell/root fallback cursor for desktop surfaces, but suppress that
  fallback in fullscreen-like non-shell app windows when the guest is likely
  already painting its own cursor.
- If a donor comparison shows the working input path uses direct pointer
  injection on the active UI thread, prefer matching that proven contract over
  background move/button queues unless fresh device forensics prove the direct
  path is the source of the freeze.
- On this shared device, do not debug desktop regressions by repeatedly
  foreground-launching `Ae.solator` from the same Termux/Codex session.
  Prefer passive ADB forensics, `run-as` inspection, and rebuild/install loops
  that do not steal focus from Termux.
- Treat `wfm.exe` as optional, not canonical. Before routing shell bootstrap or
  the `Computer` menu entry through it, verify that the active container rootfs
  actually ships `wfm.exe`; otherwise fall back to `explorer.exe`.
- If a download completes but install state does not materialize, treat that as
  a first-class contract failure between intake UI and local package indexing,
  not as a cosmetic device quirk.
- When a runtime picker uses `Contents` entry labels such as
  `Wine-<ver>-<arch>-<verCode>` or `Proton-<ver>-<arch>-<verCode>`, downstream
  consumers must resolve that entry through `ContentsManager` profile metadata
  before falling back to generic runtime parsing. Do not assume the visible
  picker string is already a canonical `wine-...` / `proton-...` identifier.
- Default device-debug posture is now full autonomous forensics first. When the
  user reports a runtime/container/UI failure, capture a complete ADB forensic
  bundle (`ci/winlator/forensic-adb-issue-capture.sh`) plus immediate
  crash/logcat/app-private evidence before asking the user for more detail.
  Only ask for another manual reproduce after the current bundle has already
  been harvested and analyzed.
- On the shared phone, autonomous forensics must still respect environment
  contamination: do not repeatedly foreground-launch `Ae.solator` from the same
  Termux/Codex session if that destroys the observation surface. Prefer
  passive bundle capture, `run-as` inspection, quiet rebuild/install, and then
  a single targeted reproduce on the freshly installed build.
- For `New Container` regressions, capture both events
  `NEW_CONTAINER_RUNTIME_SCAN` and `NEW_CONTAINER_RUNTIME_RESOLVE`, then
  verify the selected entry, resolved runtime path, and resulting
  `/files/imagefs/home/xuser-*` container root on device before declaring the
  flow fixed.
- If `/files/imagefs/home/xuser-*` exists with a real `.wine` prefix but the
  `.container` file is missing, treat it as an orphaned recoverable container,
  not as an unknown launch crash. Recover the config from the best locally
  installed `Proton`/`Wine` runtime, persist a new `.container`, and record a
  forensic event before revisiting desktop/input code.
- During donor native transfer, do not blindly replace local source-backed
  `libwinlator.so` behavior with donor `libwinlator_11.so`. That donor library
  is binary-only in-repo, maps to the older native
  `XInputStream`/`XOutputStream` path, and should stay classified as an opaque
  reference unless a source-backed equivalence is proven.
- For donor runtime parity, track helper libraries explicitly:
  `libevshim.so`, `libdummyvk.so`, `libvirglrenderer.so`,
  `libvortekrenderer.so`. Record whether each belongs in APK `jniLibs`,
  guest `usr/lib`, or both before calling the lane closed.

## Main Docs

- `README.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/DUAL_DEV_OPERATING_MODEL.md`
- `docs/ACTIVE_OWNERSHIP_MAP.md`
- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/DONOR_BANNERS_COMPONENT_INJECTOR_AUDIT.md`
- `docs/DONOR_THE412BANNER_REPO_MAP.md`
- `docs/FREEWINE_BUILD_AGENT_HANDOFF.md`
- `docs/GAMENATIVE_FULL_TRANSFER_MATRIX.md`
- `docs/GAMENATIVE_RUNTIME_GAP_INVENTORY.md`
- `docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md`
- `docs/IMAGEFS_REVERSE_MAP.md`
- `docs/IMAGEFS_HYBRID_PLAN.md`
- `docs/SECOND_DEV_ROADMAP.md`
- `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`
