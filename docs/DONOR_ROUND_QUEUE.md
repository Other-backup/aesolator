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
| 3 | `Open-Wine-Components/umu-launcher` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 4 | `khanhduytran0/ExagearAndroidX11Server` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 5 | `olegos2/mobox` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 6 | `ewt45/termux-x11-fork` | `closed` | donor sweep completed (`integrated/rejected` finalized) |
| 7 | `ewt45/winlator-fork` | `active` | opened after Round 6 closure |
| 8 | `coffincolors/winlator` | `pending` | unlocks after Round 7 = `closed` |
| 9 | `GameHub-Lite-5.3.3-RC2.apk` | `pending` | unlocks after Round 8 = `closed` |

## Round 6 (termux-x11-fork) Historical Workset (Completed)

1. Rebuilt strict file inventory for `termux-x11-fork` and locked module buckets (`app/java`, `app/cpp`, `shell-loader`, `res`).
2. Re-opened transfer matrix by lanes (`loader_trust`, `touch_input_strategy`, `clipboard_sync`, `key_interceptor`, `native_xtrans`).
3. Marked each signal `integrate` or `reject_with_rationale`.
4. Landed no-regression app-tree deltas as contracts (no blind donor copy).
5. Completed round closure (`active -> closed`) with X11 launch/input/clipboard forensic anchors.

Round 6 control artifacts:
- `docs/rounds/R6_TERMUXX11_MATRIX.md`
- `docs/rounds/R6_TERMUXX11_FILE_COVERAGE.md`

## Round 7 (winlator-fork) Immediate Workset

1. Rebuild strict file inventory for `winlator-fork` and lock module buckets (`ewt45 overlay`, `java mainline`, `native cpp`, `res`).
2. Open transfer matrix by lanes (`xserver_lifecycle`, `diagnostics_hooks`, `storage_obb`, `key_input`, `xserver_ext`, `native_boundary`).
3. Mark each signal `integrate` or `reject_with_rationale`.
4. Land only no-regression deltas in Aeolator app-tree as contracts (no blind donor copy).
5. Prepare `active -> gate` checklist with launch/PiP/input/forensic anchors.

Round 7 control artifacts:
- `docs/rounds/R7_WINLATORFORK_MATRIX.md`
- `docs/rounds/R7_WINLATORFORK_FILE_COVERAGE.md`

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
- `2026-03-05` pass 2:
  - runtime provenance markers added for wrapper contract/env source tracking.
- `2026-03-05` pass 3:
  - runtime contract forensic marker event landed (`RUNTIME_WRAPPER_CONTRACT_APPLIED`).
- `2026-03-05` closure:
  - Round 3 moved to `closed`.

## Round 4 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 4 opened as `active`.
  - `R4_EXAGEAR_MATRIX` and `R4_EXAGEAR_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - concrete donor anchors fixed for touch fan-out, gesture FSM, transform lane, X11 lifecycle lane.
- `2026-03-05` pass 3:
  - xserver lock/focus hardening landed (`XServer`, `WindowManager`, `DesktopHelper`).
- `2026-03-05` closure:
  - Round 4 moved to `closed`.

## Round 5 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 5 opened as `active`.
  - `R5_MOBOX_MATRIX` and `R5_MOBOX_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - donor anchors fixed for `install`, `patches/*`, `components/*`.
  - boundary rule recorded: runtime patch rows routed to runtime/build repos, not app-tree.
- `2026-03-05` pass 3:
  - launcher runtime bootstrap/path contract landed in `GuestProgramLauncherComponent`.
- `2026-03-05` closure:
  - Round 5 moved to `closed`.

## Round 6 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 6 opened as `active`.
  - `R6_TERMUXX11_MATRIX` and `R6_TERMUXX11_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - launch trust-state forensic markers landed in `XServerDisplayActivity`.
  - clipboard policy forensic marker lane landed.
- `2026-03-05` closure:
  - Round 6 moved to `closed`.

## Round 7 Progress Snapshot

- `2026-03-05` pass 1:
  - Round 7 opened as `active`.
  - `R7_WINLATORFORK_MATRIX` and `R7_WINLATORFORK_FILE_COVERAGE` initialized.
- `2026-03-05` pass 2:
  - PiP lifecycle continuity lane landed in `XServerDisplayActivity` (no runtime pause on PiP transition).
  - forensic marker `XSERVER_PIP_CONTINUITY` added for traceability.
- `2026-03-05` pass 3:
  - key-input dispatch fanout hardened in `XServerDisplayActivity` to prevent controller-event drop on chained negative logic.
