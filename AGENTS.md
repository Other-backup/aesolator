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

## Main Docs

- `README.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/REPO_SPLIT_TOPOLOGY.md`
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md`
- `docs/CONTENTS_QA_CHECKLIST.md`
- `docs/SECOND_DEV_ROADMAP.md`
- `docs/SECOND_DEV_REFLECTIVE_JOURNAL.md`
