# Donor Round Queue (Strict Mode)

Date: `2026-03-05`

Execution mode: `1 round = 1 donor` (sequential by default; owner override is allowed and must be documented).

## Round States

- `pending`: donor not started.
- `active`: donor round in progress.
- `gate`: implementation done, regression gates running.
- `gate_hold`: round is frozen in gate (owner override, no additional reruns in this phase).
- `closed`: donor fully exhausted (`integrated` or `rejected-with-reason` for every signal).

## Round Closure Checklist (Required)

1. Donor line-manifest and file inventory updated.
2. Module-level behavior map created.
3. Transfer candidates mapped to Aeolator targets.
4. Every candidate marked `integrate` or `reject` with rationale.
5. Integrated changes include forensic/runtime visibility where applicable.
6. Regression gates passed:
   - container create/update/start
   - launch path (Wine/Proton/route selection)
   - X11 input/orientation
   - contents install/update/replace
   - forensic issue-bundle completeness
7. No unresolved conflicts in donor transfer map.
8. Donor round summary committed to docs.
9. Queue state updated (`active -> gate -> closed`).
10. Only then next donor round may start unless owner override is explicitly declared.

## Queue

| Round | Donor | State | Notes |
|---:|---|---|---|
| 1 | `utkarshdalal/GameNative` | `gate_hold` | soft-handoff completed; closure deferred by owner decision |
| 2 | `KreitinnSoftware/MiceWine-Application` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 3 | `Open-Wine-Components/umu-launcher` | `active` | opened after Round 2 closure |
| 4 | `khanhduytran0/ExagearAndroidX11Server` | `pending` | unlocks after Round 3 = `closed` |
| 5 | `olegos2/mobox` | `pending` | unlocks after Round 4 = `closed` |
| 6 | `ewt45/termux-x11-fork` | `pending` | unlocks after Round 5 = `closed` |
| 7 | `ewt45/winlator-fork` | `pending` | unlocks after Round 6 = `closed` |
| 8 | `coffincolors/winlator` | `pending` | unlocks after Round 7 = `closed` |
| 9 | `GameHub-Lite-5.3.3-RC2.apk` | `pending` | unlocks after Round 8 = `closed` |

## Round 3 (umu-launcher) Immediate Workset

1. Rebuild strict file inventory for `umu-launcher` and lock module buckets.
2. Re-open transfer matrix by lanes (`runtime_fetch`, `runtime_route`, `provenance`, `delta_update`, `plugins`).
3. Mark every signal `integrate` or `reject_with_rationale`.
4. Land only no-regression deltas in Aeolator tree with forensic visibility.
5. Prepare `active -> gate` checklist with runtime update + provenance anchors.

Round 3 control artifacts:
- `docs/rounds/R3_UMU_MATRIX.md`
- `docs/rounds/R3_UMU_FILE_COVERAGE.md`

## Round 1 -> Round 2 Soft Handoff (Smoothed Transition)

What was added to smooth the transition:
1. Round 1 kept as `gate_hold` instead of forcing artificial `closed`.
2. Round 2 opened with explicit owner-override record and no hidden state jumps.
3. Carry-over technical prerequisites from Round 1 were preserved:
   - launch dependency contracts remained active;
   - runtime/version selector normalization remained in place;
   - no rollback of gate-level fixes before Round 2 start.
4. Round 2 scope explicitly excluded Round 1 CI rerun dependency to avoid blocking momentum.

## Round 1 Hold Snapshot

- `2026-03-05`:
  - Round 1 transfer matrix is completed and remained in gate.
  - owner requested transition to Round 2 without additional rerun in this phase.
  - Round 1 state was frozen as `gate_hold`.

## Round 2 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 2 formally opened (`active`) via owner override.
  - MiceWine matrix + file coverage controls initialized.
- `2026-03-05` pass 2:
  - first donor transfer landed in task manager lane: windows rows now expose `RAM + CPU` live metrics.
- `2026-03-05` pass 3:
  - task manager refresh cadence aligned with donor baseline (`750ms`) for tighter realtime updates.
- `2026-03-05` pass 4:
  - orientation/config change path hardened (`XServerView` relayout + overlay relayout request).
- `2026-03-05` pass 5:
  - clipboard/browser bridge env guards hardened and explicit runtime markers added.
- `2026-03-05` closure:
  - Round 2 moved to `closed`.

## Round 3 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 3 opened as `active`.
  - `R3_UMU_MATRIX` and `R3_UMU_FILE_COVERAGE` initialized.
