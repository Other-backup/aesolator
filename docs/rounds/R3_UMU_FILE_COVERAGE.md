# Round 3 Coverage: `umu-launcher` File-Level Control

Date: `2026-03-05`  
State: `active`

## Source Inventory

Donor roots:
- `/home/mikhail/work/donor-analysis/src/umu-launcher/umu`

Current file totals:
- `umu` Python files: `14`

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `runtime_orchestration` | 3 | `in_progress` | `umu_run.py`, `umu_runtime.py`, `umu_proton.py` |
| `delta_update` | 1 | `pending` | `umu_bspatch.py` |
| `plugin_layer` | 1 | `pending` | `umu_plugins.py` (likely reject lane) |
| `logging` | 1 | `pending` | `umu_log.py` forensic parity |
| `consts/util` | 2 | `pending` | `umu_consts.py`, `umu_util.py` |
| `vdf_glue` | 2 | `pending` | `umu/vdf/*` (likely reject lane) |
| `tests_entrypoints` | 4 | `pending` | `umu_test*.py`, `__main__.py`, `__init__.py` |

## Current strict-round note

- This file is Round 3 inventory control for full donor exhaustion.
- Round closure authority is `R3_UMU_MATRIX.md` plus regression gates.

## Progress log

- `2026-03-05 / pass 1`:
  - bucket map initialized and linked to active matrix.
- `2026-03-05 / pass 2`:
  - `runtime_orchestration` bucket started with runtime provenance source markers in wrapper contract lane.
