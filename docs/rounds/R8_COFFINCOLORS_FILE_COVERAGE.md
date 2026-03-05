# Round 8 Coverage: `coffincolors/winlator` File-Level Control

Date: `2026-03-05`  
State: `active`

## Source Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/winlator`

Current file totals:
- total files: `1132`
- policy/code files (java/kt/cpp/c/h/xml/sh/json/yml/md): `684`

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `java_mainline_lane` | 281 | `in_progress` | app flow, container/detail/settings, runtime bridges |
| `native_cpp_lane` | 209 | `pending` | xserver/runtime glue and low-level native lanes |
| `res_manifest_lane` | 440 | `pending` | resources/layouts/menus/preferences/manifest |
| `aux_plugins_lane` | 51 | `pending` | `input_controls/*` + `audio_plugin/*` |
| `workflow_root_lane` | 11 | `pending` | root metadata/scripts (no `.github/workflows` in donor) |

## Current strict-round note

- This file is Round 8 inventory control for full donor exhaustion.
- Round closure authority is `R8_COFFINCOLORS_MATRIX.md` plus final transfer decisions.

## Progress log

- `2026-03-05 / pass 1`:
  - Round 8 coverage file initialized.
  - base bucket map created from donor inventory.
