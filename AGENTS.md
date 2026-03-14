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

## Main Docs

- `README.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/DONOR_BANNERS_COMPONENT_INJECTOR_AUDIT.md`
- `docs/SECOND_DEV_ROADMAP.md`
- `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`
