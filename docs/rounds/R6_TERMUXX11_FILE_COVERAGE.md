# Round 6 Coverage: `termux-x11-fork` File-Level Control

Date: `2026-03-05`  
State: `active`

## Source Inventory

Donor root:
- `/home/mikhail/work/donor-analysis/src/termux-x11-fork`

Current file totals:
- total files: `152`
- policy/code files (java/cpp/xml/sh/json/yml/md): `79`

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `java_input_lane` | 7 | `in_progress` | touch strategy, gesture dispatch, input sender |
| `java_x11_core` | 5 | `in_progress` | `MainActivity`, `CmdEntryPoint`, `LorieView`, prefs/view |
| `java_utils_lane` | 5 | `in_progress` | `KeyInterceptor`, fullscreen/toolbar helpers |
| `native_cpp_lane` | 36 | `pending` | lorie + xorg patch set (boundary-heavy lane) |
| `shell_loader_lane` | 12 | `pending` | shell loader and wrapper build contracts |
| `res_manifest_lane` | 50 | `pending` | resource/config manifests and preference xml |
| `other_repo_files` | 37 | `pending` | build/workflow/docs/metadata |

## Current strict-round note

- This file is Round 6 inventory control for full donor exhaustion.
- Round closure authority is `R6_TERMUXX11_MATRIX.md` plus final transfer decisions.

## Progress log

- `2026-03-05 / pass 1`:
  - Round 6 coverage file initialized.
  - base bucket map created from donor inventory.
