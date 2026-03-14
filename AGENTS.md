# AGENTS

## Role

This repository is the source-of-truth for the Ae.solator Android application.
This agent operates here as the second autonomous developer focused on app/UI,
runtime-binding, documentation alignment, and repository contract integrity.

## Rules

- Keep app/UI/runtime-binding logic here.
- Do not treat `wcp-runtime-lanes` as an app source repository.
- Do not collapse `WCP Archive` and `WCPHub` provenance in docs or UI logic.
- Keep roadmap and docs aligned with the actual split model and active runtime
  line (`FreeWine 11`).
- Do not store secrets, access tokens, cookies, or credentials in tracked files,
  docs, prompts, patches, or commit messages.
- GitHub authentication must come from local runtime state only
  (`GITHUB_TOKEN`/credential helper/local shell session), never from repository
  files such as `AGENTS.md`.
- Every coherent implementation pass should end with a git commit unless the
  user explicitly asks to keep the tree uncommitted.

## Working Contract

- Maintain the execution roadmap in `docs/SECOND_DEV_ROADMAP.md`.
- Maintain a reflective work journal in `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`.
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
- If a download completes but install state does not materialize, treat that as
  a first-class contract failure between intake UI and local package indexing,
  not as a cosmetic device quirk.
- When a runtime picker uses `Contents` entry labels such as
  `Wine-<ver>-<arch>-<verCode>` or `Proton-<ver>-<arch>-<verCode>`, downstream
  consumers must resolve that entry through `ContentsManager` profile metadata
  before falling back to generic runtime parsing. Do not assume the visible
  picker string is already a canonical `wine-...` / `proton-...` identifier.
- For `New Container` regressions, capture both events
  `NEW_CONTAINER_RUNTIME_SCAN` and `NEW_CONTAINER_RUNTIME_RESOLVE`, then
  verify the selected entry, resolved runtime path, and resulting
  `/files/imagefs/home/xuser-*` container root on device before declaring the
  flow fixed.

## Main Docs

- `README.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/DONOR_BANNERS_COMPONENT_INJECTOR_AUDIT.md`
- `docs/DONOR_THE412BANNER_REPO_MAP.md`
- `docs/FREEWINE_BUILD_AGENT_HANDOFF.md`
- `docs/IMAGEFS_REVERSE_MAP.md`
- `docs/SECOND_DEV_ROADMAP.md`
- `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`
