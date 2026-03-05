# Round 5 Coverage: `mobox` File-Level Control

Date: `2026-03-05`  
State: `active`

## Source Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/mobox`

Current file totals:
- total files: `21`
- script/patch/docs policy files: `12`

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `bootstrap_script` | 1 | `in_progress` | `install` orchestration and runtime/bootstrap semantics |
| `box_path_patches` | 2 | `in_progress` | `box64-setdirs.patch`, `box86-setdirs.patch` |
| `wine_runtime_patches` | 2 | `in_progress` | `fix-address-space.diff`, `ge-8-25.patch` (boundary-check lane) |
| `binary_components` | 6 | `pending` | apk/deb distribution model and trust/provenance policy |
| `docs_readme` | 9 | `in_progress` | behavior semantics from README family |
| `metadata` | 1 | `pending` | `.github/FUNDING.yml` |

## Current strict-round note

- This file is Round 5 inventory control for full donor exhaustion.
- Round closure authority is `R5_MOBOX_MATRIX.md` plus final transfer decisions.

## Progress log

- `2026-03-05 / pass 1`:
  - Round 5 coverage file initialized.
- `2026-03-05 / pass 2`:
  - donor inventory fixed (`21` files total).
  - high-signal buckets opened (`bootstrap_script`, `box_path_patches`, `wine_runtime_patches`, `docs_readme`).
