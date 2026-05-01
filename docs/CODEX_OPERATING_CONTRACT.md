# Codex Operating Contract

Updated: `2026-04-16`

This document is the canonical operating contract for Codex inside
`aesolator`. It aligns repository rules, review workflow, build/runbooks, and
forensic process so the agent follows one coherent model instead of drifting
between ad-hoc prompts.

Migration addendum (2026-05-01): browser-interface continuity is active.
When backup/rollout archives are present, context restoration from those
artifacts is mandatory before continuing implementation.

## Master Engineering Directive

`docs/MASTER_ENGINEERING_DIRECTIVE.md` is a hard operating rule, not a prompt
or optional style preference.

The workspace Black Diamond execution layer at
`/data/data/com.termux/files/home/.codex/rules/OMEGA_BLACK_DIAMOND_MONOLITH.md`
is mandatory for high-impact Chapter 2 donor-transfer, graphics/runtime,
package-truth, device-proof, and provenance work. It adds hostile self-audit,
source-first compile stitching, evidence-ledger checkpoints, and explicit
accept / preserve / merge / synthesize / reject donor decisions.

For non-trivial app/runtime work, Codex must:

- default to mandatory systemic auto-fix execution when the environment allows
  direct remediation;
- build a root-cause and defect-class model before editing;
- inspect the affected app/runtime/product surface instead of one file;
- use source proof, device/runtime forensics, package/rootfs evidence, and
  donor/upstream comparison when relevant;
- prefer systemic repairs and pattern unification over ad hoc workarounds;
- avoid advisory-only completion for fixable technical defects;
- avoid manual-step recommendations when the agent can apply the change;
- make hidden app/runtime/device contracts explicit and testable where
  practical;
- collapse repeated launch/rootfs/provider repairs into shared helpers or
  validation when safe;
- use safe deep research for high-impact Android/Wine/donor/package/graphics
  decisions when it materially improves correctness, security, or reliability;
- treat processed local books and parsed Habr articles as unified reread +
  deep-research frontier inputs; extraction or scrape alone does not count as
  semantic integration;
- treat structural reread as insufficient for doctrine: run
  `/data/data/com.termux/files/home/tools/run_knowledge_semantic_read.py` and
  require byte/SHA proof, domain synthesis, skill promotion mapping, zero
  readable-source backlog, and honest residual gaps before creating/updating
  skills, rules, docs, or product doctrine;
- for the 2026-04-17 131-book corpus, pair relevant work with
  `book-corpus-graphics-frontier`, `book-corpus-native-runtime-frontier`,
  `book-corpus-android-re-frontier`, or
  `book-corpus-systems-language-frontier`;
- keep before/during/after reflection artifacts for reread/research execution
  and do not replace the work with optional feature suggestions;
- preserve security posture across secrets, app-private data, package trust,
  network boundaries, permissions, rollback safety, telemetry, and logging
  hygiene;
- expand context across UI, lifecycle, runtime binding, Contents,
  rootfs/imagefs, provider routing, command bridges, forensics, tests,
  configs, scripts, CI, package consumers, device state, and donor history
  before editing when the local evidence is insufficient;
- let broad build/parser/runtime harvests reach a real stop before log repair;
- treat a broad build harvest as a full-log/full-evidence repair surface from
  first line to last line, not a visible-tail excerpt;
- treat `%` or `как там` during an active build/runtime wave as a request for
  the global percent of remaining work across the active frontier, not only
  local tail progress, and mark estimates explicitly;
- for graphics/XServer donor transfers, require a deterministic shipped-binary
  census before claiming parity:
  renderer `.so` dependencies, exported JNI surface, hard-coded app-private
  paths, Vulkan-loader strings, and hex-anchored offsets where those explain
  ownership or routing;
- verify globally and report root cause, affected surface, defect class,
  systemic fix, verification, residual risk, and hardening follow-up.

## Operating Modes

### 1. Autonomous Delivery Mode

Default mode for normal implementation work.

- Inspect, edit, build, test, use `adb`, and update docs when that is the
  shortest path to closure.
- If `adb` is unavailable in the active lane, use app-owned external forensic
  artifacts under `/storage/emulated/0/Ae.solator/logs/` and user-provided
  forensic exports as the primary runtime truth until `adb` is restored.
- Keep fixes minimal, reviewable, and source-backed.
- Sync process docs in the same pass whenever build, runtime, or forensic
  behavior changes.
- Keep the active roadmap live during the same session:
  when the user sends a new screenshot batch, marked defect, crash, or
  forensic bundle, append it to the current tail list and carry it forward
  until a fresher live proof disproves it.
- Default to deep-search / omega-closure for non-trivial runtime, installer,
  forensic, and UI/UX tails:
  do not choose a fix from one symptom alone; correlate screenshot proof,
  forensic proof, code-path audit, and relevant donor/upstream references
  before closure.
- During session migration/restoration, add rollout/history continuity proof as
  a fourth evidence class alongside screenshot + forensic + code-path proof.
- If the user declares the graphics/XServer donor lane a one-batch transfer,
  keep Java/Kotlin, JNI/native, XServer seams, assets, shipped-binary census,
  and rule/docs sync in one closure lane with no intermediate app build or APK
  install.

### 2. Approval-Gated Staff Review Mode

Activate this mode when the user explicitly asks for review-only /
consultative work, or uses the approval phrases
`APPROVED: IMPLEMENT` / `APPROVED: EXECUTE PLAN` as gates.

Before `APPROVED: IMPLEMENT`, Codex may:

- inspect the codebase,
- search and read files,
- summarize findings and risks.

Before `APPROVED: IMPLEMENT`, Codex may not:

- edit/create/delete files,
- run builds or tests,
- install dependencies,
- use `adb` or other device actions,
- access the network or `gh`,
- push or perform any other stateful change.

If action beyond reading is needed, Codex must first provide:

1. why it is needed,
2. concrete tradeoffs,
3. an opinionated recommendation,
4. a direct approval request.

## Start Question

If the user did not already answer, ask:

`Is this a BIG change or a SMALL change?`

- `BIG`
  - review all sections step-by-step,
  - highlight the top 3-4 issues per section.
- `SMALL`
  - keep it concise,
  - ask one focused question per section.

## Proposed Plan

If the user did not provide an implementation plan, first produce a short
`Proposed Plan` with:

- ordered steps,
- touched components / files,
- test strategy.

Do not implement anything in this stage.

## Required Review Sequence

Run sections in this exact order:

1. Architecture Review
2. Code Quality Review
3. Test Review
4. Performance Review

After each section, stop and ask for feedback before moving to the next.

## Issue Format

For every issue found, provide:

1. clear description of the problem,
2. why it matters,
3. 2-3 options, including `do nothing` when reasonable,
4. for each option:
   - effort,
   - risk,
   - impact,
   - maintenance cost,
5. the recommended option and why.

Use prioritized `P0/P1/P2` findings with concrete file paths or symbols when
possible.

## Implementation Gates

Implementation is gated twice:

1. `APPROVED: IMPLEMENT`
   - only after all review sections are complete.
2. `APPROVED: EXECUTE PLAN`
   - only after Codex presents a short execution plan covering:
     - files to change,
     - steps,
     - test plan,
     - rollout / rollback.

Only after both approvals may Codex edit files or run builds/tests/device
commands.

## Repository Alignment Matrix

When build, runtime, forensic, or process contracts change, update the
relevant docs in the same pass:

- repository rules:
  - `AGENTS.md`
- docs index and entry points:
  - `README.md`
  - `docs/README.md`
  - `docs/MASTER_ENGINEERING_DIRECTIVE.md`
- local build / migration:
  - `docs/TERMUX_LOCAL_BUILD.md`
  - `docs/DEVICE_MIGRATION_BOOTSTRAP.md`
  - `docs/ADB_WIFI_DEBUG.md`
- device forensic process:
  - `docs/ADB_HARVARD_DEVICE_FORENSICS.md`
  - `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
- prefix-pack / supply-chain tooling:
  - `docs/PREFIX_PACK_TOOLKIT.md`
- active work tracking:
  - `docs/SECOND_DEV_ROADMAP.md`
  - `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`

## Current Build / Forensic Baselines

- package id:
  `com.winlator.cmod`
- local Android SDK root:
  `/data/data/com.termux/files/home/android-sdk`
- pinned host compiler lane:
  `/data/data/com.termux/files/home/.toolchains/llvm-22.1.1-termux`
- default on-device build flow:
  `sh tools/bootstrap-termux-host.sh`
  -> `. tools/env-android-local.sh`
  -> `./gradlew --no-daemon assembleDebug`
- default device forensic flow for a single runtime incident:
  `ci/winlator/forensic-adb-issue-capture.sh`
- default matrix/parsing flow for wide analysis:
  `ci/winlator/forensic-adb-harvard-suite.sh`
  + `forensic-runtime-log-assembler.py`
  + `forensic-runtime-mismatch-matrix.py`
  + `forensic-runtime-conflict-contour.py`
- default extra-runtime / prefix-pack source policy:
  prefer source-backed upstream manifests and pinned release URLs
  (`abbodi1406/vcredist`, `dl.winehq.org`) over mirror pages such as
  `TechPowerUp`
- default prefix-pack loader contract:
  each runtime entry must keep `download_url`, `source_page_url`,
  `source_label`, and `install_cmd` aligned across:
  `tools/prefix-pack/fetch-cache.sh`,
  `/opt/ae/prefix-pack/bin/prefixpack-loader.sh`,
  `Z:\\opt\\ae\\prefix-pack\\windows\\prefix-pack-loader.cmd`,
  plus the start menu bindings
- default prefix-pack cache/install contract:
  `Prepare` keeps verified payloads under `Z:\\opt\\ae\\prefix-pack\\cache`,
  mirrored runnable installers stay under `C:\\AePrefixPack\\cache`,
  Windows-side staging stays under `C:\\AePrefixPack\\staging`,
  and install state/logs stay under
  `C:\\users\\xuser\\Documents\\AePrefixPack\\save_data`
- default prefix-pack validation rule:
  zero-byte or partially staged payloads do not count as ready in repo cache,
  device cache, or Windows-visible cache; verify size first and checksum when
  one is pinned
- default large-payload device-stage rule:
  prefer an explicit temp bridge such as
  `/data/local/tmp/<package>-prefix-pack-stage -> run-as cp`
  with byte-count verification over direct stdin streaming into
  `run-as cat > file`
- default prefix-pack update rule:
  whenever toolkit assets change, bump
  `app/src/main/assets/prefixpack/VERSION`
  so the live `imagefs` copy refreshes on device
- default grouped-closure rule:
  if the user asks for a full clean pass without intermediate builds, batch the
  requested code/assets/docs changes first and only then reopen one final
  build/install/device-forensics cycle
- default graphics/XServer donor-closure rule:
  if the user asks for a whole graphics/XServer donor transfer in one batch,
  keep the entire lane closed to intermediate `gradlew`/install/device loops
  until Java/Kotlin, JNI/native, bundled assets, XServer/GLX seams,
  forensic/env routing, and donor binary evidence are reconciled together
- default binary-census rule:
  when new renderer or launcher `.so` files are part of the transfer lane,
  maintain a deterministic reverse-engineering dossier over sections,
  imports/exports, strings, symbol surface, and hex-anchored offsets where
  they materially explain behavior or ownership
- default prefix-pack scope boundary:
  do not move dedicated `Contents` payload families into `prefix-pack`.
  `DXVK`, `VKD3D`, `DgVoodoo`, `VulkanSDK`, graphics-driver payloads, and
  similar runtime packages remain separate content lanes even if donor bundles
  such as `Ajay Prefix Pro` expose shortcuts or wrappers around them
- runtime hygiene baseline:
  installed Wine/Proton unix-side ELFs must not keep donor absolute app-private
  `RUNPATH` / `RPATH`; post-install repair now rewrites those paths to local
  relative closure under `$ORIGIN` + imagefs `usr/lib`
- runtime command-bridge baseline:
  long runtime actions such as `cmd.exe /c Z:\\opt\\ae\\prefix-pack\\...`
  must be proven safe against the current `WinHandler.exec()` transport with
  the full real command length, not a short placeholder
- live roadmap baseline:
  during an active runtime/UI session, fresh screenshot batches and forensic
  bundles are not side notes. They must update `docs/SECOND_DEV_ROADMAP.md`
  before the pass is considered closed, so the next agent inherits the actual
  tail inventory instead of a stale snapshot

## Documentation Rule

Do not leave repo docs describing a stale package id, stale failure point, or
stale toolchain baseline after the code/process moved. Repository guidance is
only useful if it matches the live build and forensic reality.
