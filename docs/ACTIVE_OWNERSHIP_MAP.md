# Active Ownership Map

Date: `2026-03-13`

This is the live ownership split for current work.

## Areas

| Area | Primary Owner | Execution Space | Status | Overlap Policy |
|---|---|---|---|---|
| `freewine11` runtime source | Developer A | main runtime trees | active | no app-owner edits without handoff |
| `wcp-runtime-lanes` CI/archive | Developer A | main runtime trees | active | no app-owner edits without handoff |
| local `build-wine` | Developer A | main runtime trees | active | no app-owner edits |
| `aeolator` UI/layout/theme | Developer B | `/home/mikhail/worktrees/aeolator-ui` | active | runtime owner touches only by explicit handoff |
| `Contents` / source provenance | Developer B | `/home/mikhail/worktrees/aeolator-ui` | active | runtime owner defines contracts only |
| shipped device preset packs | Developer B | `/home/mikhail/worktrees/aeolator-ui` | active | runtime owner may request contracts, not own app implementation |
| `R10-R14` app-side continuation | Developer B | `/home/mikhail/worktrees/aeolator-ui` | active | runtime owner validates closure gates |
| `R10-R14` runtime/archive/wrapper closure | Developer A | main runtime trees | active | app owner consumes final contract |

## Current Assumptions

- Developer A is the primary runtime/build owner.
- Developer B is the full app owner.
- Mixed-mode workflow is active.
- The app-owner worktree is provisioned at:
  - `/home/mikhail/worktrees/aeolator-ui`

## Collision Rule

If two developers need the same area:
1. update this map first
2. assign one temporary owner
3. finish the overlap quickly
4. restore the normal split
