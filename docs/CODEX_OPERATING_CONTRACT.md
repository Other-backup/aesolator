# Codex Operating Contract

Updated: `2026-03-18`

This document is the canonical operating contract for Codex inside
`aesolator`. It aligns repository rules, review workflow, build/runbooks, and
forensic process so the agent follows one coherent model instead of drifting
between ad-hoc prompts.

## Operating Modes

### 1. Autonomous Delivery Mode

Default mode for normal implementation work.

- Inspect, edit, build, test, use `adb`, and update docs when that is the
  shortest path to closure.
- Keep fixes minimal, reviewable, and source-backed.
- Sync process docs in the same pass whenever build, runtime, or forensic
  behavior changes.

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
- local build / migration:
  - `docs/TERMUX_LOCAL_BUILD.md`
  - `docs/DEVICE_MIGRATION_BOOTSTRAP.md`
  - `docs/ADB_WIFI_DEBUG.md`
- device forensic process:
  - `docs/ADB_HARVARD_DEVICE_FORENSICS.md`
  - `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
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
- runtime hygiene baseline:
  installed Wine/Proton unix-side ELFs must not keep donor absolute app-private
  `RUNPATH` / `RPATH`; post-install repair now rewrites those paths to local
  relative closure under `$ORIGIN` + imagefs `usr/lib`

## Documentation Rule

Do not leave repo docs describing a stale package id, stale failure point, or
stale toolchain baseline after the code/process moved. Repository guidance is
only useful if it matches the live build and forensic reality.
