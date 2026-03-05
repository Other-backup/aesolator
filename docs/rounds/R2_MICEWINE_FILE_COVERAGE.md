# Round 2 Coverage: `MiceWine-Application` File-Level Control

Date: `2026-03-05`  
State: `active`

## Source Inventory

Donor roots:
- `/home/mikhail/work/donor-analysis/src/MiceWine-Application/app/src/main/java/com/micewine`

Current file totals:
- `com/micewine` total (`.java/.kt`): `86`

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `emu/input` | 7 | `in_progress` | touch dispatch and stale-pointer release contracts |
| `emu/adapters` | 15 | `in_progress` | process rows, inline actions, UX density |
| `emu/fragments` | 36 | `in_progress` | task manager and runtime settings surfaces |
| `emu/core` | 7 | `pending` | env var and package manager semantics |
| `emu/activities` | 9 | `pending` | emulation lifecycle and overlay hooks |
| `emu/controller` | 3 | `pending` | controller utility contracts |
| `emu/views` | 4 | `pending` | virtual control rendering/input coupling |
| `emu/utils` | 3 | `pending` | generic helpers (candidate selective adoption) |
| `emu/LorieView.java` | 1 | `in_progress` | X11 surface lifecycle/clipboard/IME bridge |

## Current strict-round note

- This file is Round 2 inventory control for full donor exhaustion.
- Round closure authority is `R2_MICEWINE_MATRIX.md` plus regression gates.
- `pending` buckets stay open until explicit integrate/reject decisions are recorded.

## Progress log

- `2026-03-05 / pass 1`:
  - bucket map initialized and linked to active matrix.
- `2026-03-05 / pass 2`:
  - `emu/adapters` process-row metric density pattern started (`RAM + CPU` in windows list rows).
- `2026-03-05 / pass 3`:
  - `emu/fragments` task-manager live loop cadence aligned to donor (`750ms` refresh interval).
