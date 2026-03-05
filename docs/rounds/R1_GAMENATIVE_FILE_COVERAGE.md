# Round 1 Coverage: `GameNative` File-Level Control

Date: `2026-03-05`  
State: `gate_hold`

## Source Inventory

Donor roots:
- `/home/mikhail/work/donor-analysis/src/GameNative/app/src/main/java/app/gamenative`
- `/home/mikhail/work/donor-analysis/src/GameNative/app/src/main/java/com/winlator`

Current file totals:
- `app/gamenative`: `325` source files (`.kt/.java`)
- `com/winlator`: `212` source files (`.kt/.java`)

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

### `app/gamenative` buckets

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `ui` | 130 | `in_progress` | includes settings dialogs, graphics/contents/wine-proton UX |
| `ui/component` | 58 | `in_progress` | dialog-level transfer candidates |
| `ui/screen` | 39 | `in_progress` | manager screens and high-level runtime orchestration |
| `utils` | 51 | `in_progress` | includes `launchdependencies` donor contracts |
| `data` | 35 | `pending` | process/gesture/model transfer candidates |
| `db` | 22 | `pending` | persistence contracts (likely selective) |
| `service` | 37 | `pending` | store-specific lanes need integrate/reject decisions |
| `gamefixes` | 15 | `pending` | per-title fixup semantics |
| `api` | 4 | `pending` | remote contract wrappers |
| `externaldisplay` | 5 | `pending` | external display behavior deltas |

### `com/winlator` buckets

| Bucket | Files | State | Notes |
|---|---:|---|---|
| `contents` | 3 | `closed` | taxonomy + content contract parity |
| `xserver` | 92 | `pending` | x11/dri3 behavior sweep still required |
| `fexcore` | 3 | `pending` | preset contract parity |
| `core` | 33 | `in_progress` | runtime/env and diagnostics deltas |
| `contentdialog` | 3 | `pending` | dialog-level config semantics |
| `widget` | 4 | `in_progress` | touchpad/input behavior mapping |
| `inputcontrols` | 10 | `in_progress` | pointer cleanup + dispatch guardrails |
| `xenvironment` | 16 | `in_progress` | launcher/runtime path (active) |

## Current strict-round note

- This document is a control surface for donor inventory depth and backlog hygiene.
- Round closure authority is the signal matrix (`R1_GAMENATIVE_MATRIX.md`) + regression gates.
- Buckets marked `pending` here represent deep-sweep opportunities beyond the now-completed signal transfer matrix.
- Current owner override keeps Round 1 in `gate_hold` while Round 2 is active.

## Progress log

- `2026-03-05 / pass 5`:
  - `xenvironment` launch preflight strengthened with explicit dependency gate results and emulator runtime checks.
  - `contents` parity improved for Wine/Proton profile metadata (`proton` section support + shared install validation path).
- `2026-03-05 / pass 6`:
  - `xenvironment` launch preflight extended with wrapper payload validation (`dxvk/vkd3d/dgvoodoo`).
