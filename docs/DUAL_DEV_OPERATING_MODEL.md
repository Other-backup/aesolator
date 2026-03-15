# Dual Developer Operating Model

Date: `2026-03-13`

This repository now assumes a mixed-mode development model with two autonomous
Codex developers.

## Roles

### Developer A / Primary Runtime Owner

Owns:
- `freewine11`
- `wcp-runtime-lanes`
- local `build-wine`
- runtime CI / exact-commit validation
- archive/release orchestration

Expected behavior:
- monitors runtime/build logs continuously
- fixes runtime/build failures without waiting for manual dispatch
- defines runtime-side contracts for app consumption
- validates end-to-end runtime behavior after app-side implementation

### Developer B / Full App Owner

Owns:
- `aeolator`
- UI/layout/theme
- `Contents`
- runtime-binding
- shipped preset packs
- app docs / roadmap / ownership docs
- device-debug and app-side validation

Expected behavior:
- works autonomously inside `aeolator`
- keeps app docs current with every meaningful app-side architecture or workflow change
- implements app-side contracts exposed by runtime/build work

## Execution Model

Mixed mode:

- `Developer A` works in the main runtime trees.
- `Developer B` works in a dedicated app worktree:
  - `/home/mikhail/worktrees/aeolator-ui`

The goal is to avoid overlap by default, not to coordinate after a collision.

## Shared Boundary

Shared boundary is small and explicit:

- runtime contract definition
- app consumption of runtime env / metadata / package state
- donor-round closure gates where app and runtime both participate

Rule:
- runtime owner defines the contract
- app owner implements the app-side behavior
- runtime owner validates end-to-end behavior

## Overlap Policy

Default rule: no overlap without claim.

### Allowed without handoff

- runtime owner edits `freewine11` and `wcp-runtime-lanes`
- app owner edits `aeolator`

### Requires explicit handoff

- runtime owner needs to touch `aeolator`
- app owner needs to touch `freewine11` or `wcp-runtime-lanes`
- both developers need the same app-side subsystem

When that happens:
1. update `ACTIVE_OWNERSHIP_MAP.md`
2. record who owns the area now
3. record why overlap is needed
4. keep the overlap temporary

## Active App-Side Priority Areas

Current app-owner zones:
- `Contents`
- `Graphics Center`
- `Settings`
- `ContainerDetailFragment`
- `ShortcutSettingsDialog`
- device-targeted shipped preset packs
- `R10-R14` app-side follow-up state

Current provisioned execution space:
- `/home/mikhail/worktrees/aeolator-ui`

Current runtime-owner zones:
- `FreeWine 11.4` build stabilization
- `build-wine`
- runtime CI exact-commit workflows
- archive/runtime publishing
- wrapper/package consumer validation for `R10-R14`

## R10-R14 Ownership Split

Current normalized status:
- `R10-R14` app-side work is already integrated in tree
- `R10-R14` are still `gate_hold`

Ownership for the remaining work:
- app-side continuation -> app owner
- runtime/archive/wrapper closure gates -> runtime owner

## Documentation Discipline

Every meaningful step must keep these aligned:
- `AGENTS.md`
- `docs/README.md`
- `docs/DONOR_REFLECTIVE_ROADMAP.md`
- `docs/DONOR_ROUND_QUEUE.md`
- `docs/ACTIVE_OWNERSHIP_MAP.md`

No hidden ownership changes.
