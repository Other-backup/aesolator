# AGENTS

## Role

This repository is the source-of-truth for the Ae.solator Android application.
This agent operates here as the second autonomous developer focused on app/UI,
runtime-binding, documentation alignment, and repository contract integrity.

## Operating Modes

- Default mode is autonomous delivery:
  inspect, edit, build, test, use `adb`, and close documentation tails in the
  same pass.
- If the user explicitly requests review-only / consultative work, or uses the
  approval gates `APPROVED: IMPLEMENT` / `APPROVED: EXECUTE PLAN`, switch to
  Approval-Gated Staff Review Mode.
- The canonical cross-repo description of these modes lives in
  `docs/CODEX_OPERATING_CONTRACT.md`.

## Approval-Gated Staff Review Mode

When this mode is active:

- Before `APPROVED: IMPLEMENT`, do read-only work only:
  inspect/search local files and summarize findings.
- Before approval, do not:
  edit files, run `gradlew`, use `adb`, install dependencies, access the
  network, push, or perform any other stateful action.
- If action beyond reading is needed, first explain:
  1. why it is needed,
  2. concrete tradeoffs,
  3. the recommended option,
  4. the approval request.
- Start by asking whether the task is `BIG` or `SMALL` unless the user already
  answered.
- If no implementation plan was provided, first produce a short `Proposed Plan`
  with steps, touched components, and test strategy.
- Review sections must run in this exact order:
  1. Architecture Review
  2. Code Quality Review
  3. Test Review
  4. Performance Review
- After each review section, stop and ask for feedback before moving to the
  next one.
- For each issue, provide:
  1. problem,
  2. why it matters,
  3. 2-3 options,
  4. effort/risk/impact/maintenance cost for each option,
  5. recommended option and why.
- Do not implement until `APPROVED: IMPLEMENT`.
- After that, present a short execution plan with files, steps, tests, and
  rollout/rollback, then wait for `APPROVED: EXECUTE PLAN` before acting.

## Rules

- `Chapter 2` is now active:
  `Ae.solator` is no longer just a launcher that can consume any arbitrary
  Wine/Proton line. It now targets its own dedicated `FreeWine11` runtime
  line.
- The shared cross-repo contract for this phase lives in
  `docs/CHAPTER2_FREEWINE_AESOLATOR_CONTRACT.md`.
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
- For runtime redistributables, prefix packs, and helper payloads, prefer the
  most transparent source in this order:
  official vendor source, audited upstream repository with visible build/source
  metadata, mirror page only as a distribution fallback.
- For prefix-pack runtime entries, do not stop at a bare URL swap. Each entry
  must keep all of these aligned in one pass:
  `download_url`, `source_page_url`, `source_label`, `install_cmd`,
  repo-side cache helper output, rootfs shell loader output, Windows loader
  output, and start-menu wiring.
- Prefix-pack user-facing wording must stay precise:
  `Prepare` means verify/download into `Z:\opt\ae\prefix-pack\cache` and mirror
  into `C:\AePrefixPack\cache`;
  `Install` means execute a Windows-side installer into the current prefix;
  `State` means the resulting marker/logs under the dedicated save-data root.
- Keep the boundary explicit:
  anything that is already delivered as a dedicated `Contents` / payload lane
  (`DXVK`, `VKD3D`, `DgVoodoo`, `VulkanSDK`, graphics drivers, runtime wrapper
  payloads) must stay out of `prefix-pack`. `prefix-pack` is for
  prefix-local redistributables and helper installers, not for duplicating the
  app's payload/package manager.
- When prefix-pack assets change, bump
  `app/src/main/assets/prefixpack/VERSION` in the same pass. Otherwise the live
  app-private `imagefs` copy can silently keep stale toolkit files after APK
  reinstall.
- Prefix-pack Windows helpers follow an Ajay-style state discipline:
  keep dedicated `save_data` and `logs` roots under the Windows user profile
  instead of scattering runtime installer residue across temp paths.
- Do not mark prefix-pack payloads `present` or `ready` when they are zero-byte
  or partially staged. Repo cache, rootfs cache, and Windows-visible cache all
  need size-valid proof before closure.
- For large prefix-pack staging onto a device, do not stream directly into
  `run-as cat > file` as the primary path. Use an explicit temp bridge such as
  `/data/local/tmp -> run-as cp` and verify the byte count after copy.
- For `FreeWine11` Chapter 2 work, treat `FEX` / `wowbox64` as required guest
  translator payloads, but not as the default explanation for native
  `wineboot` / prefix bootstrap failures. Prove the failing layer first.
- Any runtime command routed through `WinHandler.exec()` must be checked with
  the full real command length. Long `cmd.exe /c Z:\...` toolkit launches are
  part of the regression surface and must not be assumed safe without proof.
- Do not treat mirror pages such as `TechPowerUp` as the source-of-truth for
  pinned runtime payload decisions if an upstream such as
  `abbodi1406/vcredist` or an official vendor release exists.
- Local host compiler lane is pinned to `LLVM 22.1.1` when a dedicated
  source-built toolchain is present under
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux`.
  Treat older Termux LLVM packages only as bootstrap tools, not the target
  compiler baseline for future local `wine` or runtime builds.
- `Ae.solator` is not the owner of the host LLVM CI lane. Heavy `LLVM 22.1.1`
  workflow/release orchestration belongs in `wcp-runtime-lanes`; this repo may
  consume the resulting host-toolchain artifact, but must not be treated as
  the canonical place to build or publish it. Do not add or restore a
  host-LLVM GitHub Actions build lane in this repo.
- If host-compiler CI/release work is requested, do it directly in
  `wcp-runtime-lanes/main`, not in side branches of `aesolator`, and not as a
  detached experimental lane inside this repo.
- Default git posture is `main`-first. Do not split ordinary work into side
  branches, staged merge branches, or temporary integration branches unless
  the user explicitly asks for that. If a temporary branch exists, treat it as
  transitional debt and move the real working lane back to `main`.
- When repository sync or CI dispatch is requested, prefer landing the final
  state straight onto `main` rather than parking it in a temporary merge
  branch first.
- Without `adb`, do not rely on shell `logcat` as the primary app-proof path.
  On this device it exposes Termux/system noise much better than `Ae.solator`
  app-UID data. Default local forensic sources are:
  `/storage/emulated/0/Ae.solator/logs/forensics/*.jsonl`,
  `fatal_crash_*.txt`, and runtime stream files under
  `/storage/emulated/0/Ae.solator/logs/`.
- Every coherent implementation pass should end with a git commit unless the
  user explicitly asks to keep the tree uncommitted.
- Before moving work to a new device, push the live state to `main`, keep both
  repos on clean worktrees, and update `docs/DEVICE_MIGRATION_BOOTSTRAP.md`
  plus any external handoff note together so the next device starts from one
  consistent bootstrap path.
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
- Keep that roadmap live during the same session:
  when the user sends a new screenshot batch, marked UI defect, fresh crash,
  or runtime anomaly, append it to the active roadmap/tail list immediately
  instead of waiting for a later recap pass.
- Maintain a reflective work journal in `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`.
- Maintain donor binary/source boundary notes in:
  `docs/GAMENATIVE_RUNTIME_GAP_INVENTORY.md` and
  `docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md`.
- Keep `docs/DEVICE_MIGRATION_BOOTSTRAP.md` current whenever the baseline
  Termux/SDK/host-LLVM/bootstrap assumptions change.
- Keep documentation aligned with actual code, runtime contracts, and split repo
  ownership before calling a task complete.
- When process/build/forensic rules change, update the entry-point docs in the
  same pass:
  `README.md`, `docs/README.md`, `docs/CODEX_OPERATING_CONTRACT.md`,
  `docs/TERMUX_LOCAL_BUILD.md`, `docs/DEVICE_MIGRATION_BOOTSTRAP.md`,
  `docs/ADB_HARVARD_DEVICE_FORENSICS.md`, `docs/SECOND_DEV_ROADMAP.md`, and
  `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`.
- Report changes to the first developer as concise implementation notes with:
  scope, files touched, contract impact, verification status, and open risks.
- Do not leave accidental build-side tails in the tree; restore or explain
  every unexpected delta before handoff/commit.
- For every substantial change, record:
  goal, context, decision, tradeoff, verification status, and next step.
- Prefer reflective, contract-first integration over blind donor copying or
  cosmetic-only edits without documentation sync.
- When the user asks for `deep search`, `omega level`, `full reflection`, or a
  source choice, respond with a source-backed comparison:
  why the chosen upstream won, which alternatives were held, exact pinned
  versions/URLs when possible, and what remains intentionally manual.
- Default to that same deep-search mode for any non-trivial runtime,
  installer, prefix-pack, forensic, device-debug, or UI/UX tail even when the
  user does not repeat the phrase explicitly.
- When donor hunting is part of the pass, do not stop at one familiar repo.
  Pull a broad GitHub candidate pool first, then keep both the shortlist and
  the rejected-but-reviewed donors in docs so the next pass inherits the full
  search space instead of restarting it.
- In this repo, `deep search` means one combined closure loop:
  latest screenshot batch + freshest forensic bundle + local code-path audit +
  donor/upstream comparison + fresh device proof.
- Do not close a live tail from one source alone.
  For runtime/UI/install defects, require all three:
  1. screenshot proof,
  2. forensic/log proof,
  3. code-path confirmation.
- For recurring product tails, keep digging until the failing layer,
  contradictory evidence, and next corrective action are explicit.
  Do not stop at the first plausible explanation.
- For supply-chain choices, the reflective result must also state how the
  payload is fetched, where provenance remains visible to the user, and which
  loader/status surfaces were updated to keep that provenance inspectable.
- When donor hunting is active, do not stop at one or two obvious forks.
  Always search a broad GitHub candidate pool first, then rank the best donor
  set against the active layer:
  prefix/install flow, start menu UX, contents feeds, diagnostics, runtime UI,
  or device integration.
- For donor passes, keep a reflective shortlist and a rejected-candidate list.
  The result must state why each winner beat the others:
  branch relevance, release freshness, active maintenance, install-flow
  visibility, component inventory, and fit with the current `Ae.solator`
  contract.
- If the user narrows a donor transfer to one layer such as management UI,
  treat that as a hard scope wall:
  do not sprawl back into start-menu, payload import, or unrelated runtime
  plumbing during the same pass unless the user explicitly widens the scope.
- If the user later widens that scope again, expansion is allowed only along
  adjacent management/runtime surfaces and only if install/state/proof logic
  stays at least as strict as before. Wider scope is not permission to weaken
  contracts or revive dead-end UX.
- Treat `Ajay Prefix Pro` as audited input only:
  mine it for component inventory, save-data discipline, and UX ideas, but do
  not import or redistribute its proprietary AutoIt/batch shell verbatim.
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
- If the same user message also sharpens product requirements or process
  expectations, mirror that refinement into `AGENTS.md` and the relevant
  runbook instead of leaving it trapped in chat history.
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
- Treat the freshest user screenshot batch as a first-class forensic artifact:
  when screenshots and automated captures disagree, prefer the user screenshots
  for UI-tail inventory and pull them into the active forensic bundle.
- In every active device/UI session where the user says they made a screenshot
  batch, inspect the latest 5-10 screenshots under
  `/storage/emulated/0/Pictures/Screenshots/` before declaring closure.
- Screenshot review must stay reflective and geometric:
  audit every visible edge, clipped row, spacer, card width, header alignment,
  contrast failure, duplicated button, and action drift; turn each confirmed
  defect into an explicit tail item for the same pass.
- When the user says they opened things manually, scrolled, or reproduced the
  bug by hand before taking screenshots, treat that batch as the primary UI
  truth for the current pass and mine it for all remaining tails.
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
- Runtime progress surfaces, prefix/install loaders, and nested runtime dialogs
  must stay on the runtime blue / amber palette. Do not leave legacy green
  progress spinners, confirm buttons, or green-on-green cards in these flows.
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
- For `Prefix Pack` launch failures, do not stop at Android-side dispatch logs.
  Verify the staged launcher itself:
  `C:\\AePrefixPack\\staging`, the lane state marker, and the lane launcher log.
  If `WinHandler` dispatch is unproven, escalate to a guest-side fallback path
  instead of concluding that the lane merely "cached".
- If a legacy installer reports `MFC42.DLL`, `MFC42LOC.DLL`, `isskin.dll`, or
  similar classic dependency errors, first verify that the `vcrun6` /
  `vcrun_full` lane actually executed. Do not treat those warnings as proof
  that the VC payload pack itself is missing when the lane state is still only
  `scheduled`.
- `Task Manager` process rows must land on full-row boundaries after scrolling.
  A clipped first or last row, animated half-row snap, or viewport height that
  cannot fit an integer count of rows is a functional regression, not a
  cosmetic nit.
- If the user explicitly says the current live crash is fresh, prioritize a
  freshest-log-first capture before further taps, scripted UI navigation, or
  extra launches. Preserve the first clean post-crash evidence bundle as the
  reference point.
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
- Treat "full forensics", "full logging", "every layer", or similar user
  wording as a request for the maximum adb-available contract, not as permission
  to stop at filtered `logcat`. Default deliverables are:
  - one historical/full bundle
  - one clean-session bundle with `adb logcat -b all -c`, force-stop, single
    relaunch, and fresh `adb logcat -b all -v threadtime`
  - pulled runtime stream files for the reproduced launch
  - app-private `run-as` evidence
  - `dumpsys` state for process, package, window, graphics, memory, thermal,
    battery, usage, and dropbox surfaces
  - current privilege/appops/whitelist state actually accepted by the device
- Default parser stack for forensic interpretation is:
  - `ci/winlator/forensic-runtime-log-assembler.py`
  - `ci/winlator/forensic-issue-bundle.py`
  - `ci/winlator/forensic-runtime-mismatch-matrix.py`
  - `ci/winlator/forensic-runtime-conflict-contour.py`
  Prefer these over one-off grep summaries whenever the artifacts are present.
- Treat runtime debugging as a full stack walk across all live layers:
  app/UI, Java, JNI, `libwinlator`, Wine/Proton, Box64/FEX, DXVK/VKD3D,
  Vulkan loader/ICD/layers, wrapper/provider selection, Mesa/Turnip/Zink,
  audio, container/rootfs/prefix/bootstrap layout, and Android framework/vendor
  ROM behavior. Do not close a defect on a top-layer symptom alone if a lower
  failing layer is visible in the bundle.
- When the graphics/runtime lane is active, inspect the exact driver/ICD/layer
  tree, not just the selected label. Follow the concrete on-device path through:
  APK `jniLibs`, app-private staged files, rootfs `usr/lib`, runtime payload,
  overlay, prefix/bootstrap layer, and any donor residue. Record whether a
  failing library is missing, incompatible, mislayered, or resolved from the
  wrong owner.
- If a log shows a dependency-edge failure such as `dlopen failed`,
  `undefined symbol`, `vkCreateInstance`, `Found no drivers`, `nodrv_CreateWindow`,
  or runtime self-exit after bootstrap, write the causal chain explicitly:
  symptom -> failing layer -> concrete file/path -> blocking dependency ->
  resulting exit behavior -> next probe or patch.
- Forensic depth on this repo should be engine-grade and parser-first. The goal
  is not "collect many logs"; the goal is to leave every meaningful failure with
  a rooted explanation across the full runtime tree and all downstream effects.
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
- For native bootstrap regressions on this ROM, do not leave
  `System.loadLibrary("winlator")` scattered across leaf classes. Route
  `libwinlator` loading through one central loader with synchronous forensic
  checkpoints, and prefer source fixes that remove host-side binary leakage
  such as stray `RUNPATH` entries before chasing guest runtime symptoms.

## Prefix Pack / Legacy DX Contract

- `Prefix Pack` owns prefix-local redistributables, install state, and graphics
  diagnostics discovery. It does not own `DXVK`, `VKD3D`, `dgVoodoo`,
  `Vulkan SDK`, or graphics-driver payload lanes.
- In donor-driven UI passes, prioritize Android-side management surfaces over
  Windows start-menu cloning unless the user explicitly asks for start-menu
  work in the same pass.
- The Android `Prefix Pack` UI must expose three separate facts clearly:
  1. cache location and cache readiness,
  2. install action into the current prefix,
  3. install-state/log roots and the last known result.
- Do not label a button `Fetch` without clarifying that it only caches payloads
  into `Z:\opt\ae\prefix-pack\cache` / app-private rootfs cache and does not
  install anything into the prefix.
- Every Windows-side install helper must write reproducible state under the
  dedicated save root so the Android UI and forensic passes can tell whether a
  lane never ran, is running, failed, or completed.
- Windows-side helpers must use a visible installer-cache contract:
  backend cache in `Z:\opt\ae\prefix-pack\cache`,
  mirrored runnable installers in `C:\AePrefixPack\cache`,
  staging in `C:\AePrefixPack\staging`,
  and state/logs in `C:\users\xuser\Documents\AePrefixPack\save_data`.
- `Install` must stage a visible launcher under
  `C:\AePrefixPack\staging\<lane>\install-<lane>.cmd` and prewrite a scheduled
  state marker before handing off to runtime. If the runtime-side launch dies,
  the user still needs a resumable launcher plus a state/log breadcrumb.
- If the user requests a no-intermediate-build clean pass, finish the full
  requested code/assets/docs slice before reopening `assembleDebug`, install,
  or device repro. One final closure build is preferred over churn.
- If an installer requires GUI interaction, surface that expectation in the UI
  and keep logs/state paths visible instead of pretending the flow is silent.
- `Task Manager` is a runtime tool, not a `Prefix Pack` tool. Do not duplicate
  it inside the `Prefix Pack` surface when the runtime drawer already owns that
  function.
- `Prefix Pack` should read like a `Contents`-style loader, not a vague tool
  bucket: keep compact sectioned lanes, per-lane class badges, and direct
  `Prepare -> Install -> Launch -> State/Logs` visibility on the primary
  surface.
- `Prefix Pack` install actions must be closure-safe:
  if payloads are missing, `Install` should auto-run `Prepare`, then dismiss
  the Android dialog before launching the Windows installer so any GUI path is
  actually visible to the user instead of hiding behind the overlay.
- Prefer detached guest launch as the primary `Prefix Pack` install bridge and
  treat `WinHandler` shell dispatch as a fallback path, not the only way to
  open the staged installer.
- `Prefix Pack` deferred install targets are one-shot debug aids, not sticky
  product state:
  consume them once and clear them so reopening the toolkit never silently
  relaunches a previous `.NET` or installer lane.
- `Prefix Pack` launcher dispatch must be forensically explicit:
  after dialog dismissal, record whether runtime execution was dispatched,
  retried, or timed out waiting for `WinHandler`, and do not call the pass
  closed while lane state is still stuck at `scheduled` with no launcher log.
- Primary `Install` on a `Prefix Pack` lane must always restage:
  do not silently reuse old `C:\AePrefixPack\staging` launchers from the main
  lane card just because stale files exist. Stale launcher reuse belongs only
  to an explicit secondary `Launch` action.
- Treat fresh user screenshots as real runtime evidence, not cosmetic notes:
  mine them for every visible paint, geometry, wording, and flow defect and
  fold those tails into the same closure pass.
- Do not close a runtime UI pass while any defect visible in the latest user
  screenshot batch remains unexplained, even if the agent's own capture looks
  cleaner.
- For installer hand-off proof, stale state or old launcher logs do not count.
  Compare state/log freshness against the current dispatch attempt and retry
  alternate launch forms until the current attempt leaves fresh proof or the
  runtime wait path times out.
- `C:\AePrefixPack\staging` is a user-facing runtime surface:
  keep a direct Android-side entry to the stage root, and make lane-level
  `Launch` rerun the staged launcher itself instead of just opening Explorer on
  that path.
- `Wine Mono` and `Mono Runtime` are not synonyms. `Wine Mono` stays the single
  official WineHQ MSI, while extra Mono Project x86/x64 installers belong to a
  dedicated managed-runtime lane with their own source labels and install state.
- If the user explicitly says the Mono Project Windows installer lane is not
  relevant, remove it from the active `Prefix Pack` surface instead of keeping
  it as speculative coverage.
- Keep managed-runtime architecture coverage explicit everywhere:
  `Wine Gecko` is `x86 + x86_64`, `Wine Mono` remains the single upstream `x86`
  MSI, and Windows Mono is the separate `win32 + x64` lane. If UI or docs blur
  those boundaries, treat it as a contract bug.
- If user feedback points at missing classic managed dependencies such as
  `.NET Framework 4.x`, solve that with an explicit source-backed `.NET
  Framework` lane instead of relabeling Wine Mono or Mono Project payloads.
- `Task Manager` must keep the process list functionally visible:
  if tab/content weights or theme colors can make rows disappear, treat that as
  a blocker. Dense scrollable rows with inline telemetry beat oversized cards.
- On the current runtime branch, prefer a Linux-first `Task Manager`:
  one large left process pane, fixed headers, near-full-height scrolling, dense
  inline telemetry, and enough bottom padding that the final row is never cut
  by dialog chrome or bottom actions.
- Keep the Linux list table-like and stable:
  one outer process card, flat rows instead of stacked mini-cards, metrics
  aligned under fixed headers, and ordering stable enough that live refreshes
  do not visually "eat" a row while the user is scrolling.
- After manual scroll, the Linux list should settle on a full-row boundary at
  the top edge too. A half-cut first row is a functional bug, not acceptable
  polish debt.
- Runtime dialogs must keep contrast honest:
  no green-on-green footer/buttons/checks inside `Task Manager`, `Prefix Pack`,
  or sibling runtime tools when the active accent family is blue/amber.
- Do not let generic theme repaint passes recolor runtime preloaders or
  runtime-specific progress dialogs back to legacy green once the runtime
  palette has already been applied.
- After runtime UI changes, closure requires device proof:
  install the APK, open the touched surface on the device, save a fresh
  screenshot, and pair it with the matching forensic/log bundle before calling
  the pass complete.
- If the user supplied a newer screenshot batch than the agent's own capture,
  use the fresher user screenshots as the primary UI truth until a newer
  device capture proves the issue gone.
- Treat the latest user screenshot batch as an active defect queue, not as
  optional commentary: every visible flaw stays on the tail list until fixed or
  disproven by a newer live capture.
- Keep the roadmap live during the active session:
  every new marked screenshot batch, forensic bundle, crash, hang, or user
  wording change must be folded into the current tail inventory and reflected in
  `docs/SECOND_DEV_ROADMAP.md` before the pass is called closed.
- Before adding new prefix-pack payloads, surface the diagnostics already
  present in the staged rootfs/prefix if they exist:
  `DXDiag`, `TestD3D.exe`, `GPUInfo.exe`, `GLview`, and similar donor utility
  overlays.
- If donor/rootfs diagnostics exist in app-private `imagefs` but are missing
  from UI discovery, treat that as a broken linkage/discoverability bug rather
  than as proof they need to be re-imported.
- Do not delete donor/rootfs utility overlays such as `opt/apps`, `7-Zip`,
  `GPUInfo.exe`, or `TestD3D.exe` during generic runtime patch passes unless a
  replacement overlay is staged in the same pass and that replacement is
  explicitly verified.
- For legacy DirectX handling, prefer one explicit story over folklore:
  document which `DirectX June 2010` components are staged, where they land,
  and how old D3D/XACT/XAudio/XInput expectations are satisfied.
- `dgVoodoo` remains a dedicated payload lane, but the runtime UI, docs, and
  forensics must show:
  requested mode, requested arch, active arch, stage target, force-D3D11 flag,
  and whether staged DLL ownership actually succeeded for the launched target.

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
