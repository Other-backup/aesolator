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
| 1 | `utkarshdalal/GameNative` | `gate_hold` | owner override: moved to Round 2 without additional rerun; returns to `closed` later |
| 2 | `KreitinnSoftware/MiceWine-Application` | `active` | active round opened by owner override on `2026-03-05` |
| 3 | `Open-Wine-Components/umu-launcher` | `pending` | unlocks after Round 2 = `closed` |
| 4 | `khanhduytran0/ExagearAndroidX11Server` | `pending` | unlocks after Round 3 = `closed` |
| 5 | `olegos2/mobox` | `pending` | unlocks after Round 4 = `closed` |
| 6 | `ewt45/termux-x11-fork` | `pending` | unlocks after Round 5 = `closed` |
| 7 | `ewt45/winlator-fork` | `pending` | unlocks after Round 6 = `closed` |
| 8 | `coffincolors/winlator` | `pending` | unlocks after Round 7 = `closed` |
| 9 | `GameHub-Lite-5.3.3-RC2.apk` | `pending` | unlocks after Round 8 = `closed` |

## Active Round 2 (MiceWine) Immediate Workset

1. Rebuild strict file inventory for `MiceWine-Application` and lock module buckets.
2. Re-open transfer matrix by lanes (`task_manager`, `input`, `x11_surface`, `runtime_env`, `controller`).
3. Mark every signal `integrate` or `reject_with_rationale`.
4. Land only no-regression deltas in Aeolator tree with forensic visibility.
5. Prepare `active -> gate` checklist with runtime-smoke anchors for task manager + input lanes.

Round 2 control artifacts:
- `docs/rounds/R2_MICEWINE_MATRIX.md`
- `docs/rounds/R2_MICEWINE_FILE_COVERAGE.md`

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
