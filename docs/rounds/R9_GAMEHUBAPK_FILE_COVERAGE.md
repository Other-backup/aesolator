# Round 9 Coverage: `GameHub-Lite-5.3.3-RC2.apk` File-Level Control

Date: `2026-03-05`  
State: `closed`

## Source Inventory

APK root:
- `/home/mikhail/Загрузки/GameHub-Lite-5.3.3-RC2.apk`

Analysis workspace:
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2`

Current totals:
- files: `5793`
- dex: `11`
- native `.so`: `23`
- decompiled java sources: `20905`
- `winemu` namespace sources: `171`

## Module Buckets (Control Checklist)

Legend:
- `pending` = not yet swept in strict round mode
- `in_progress` = reflective sweep started
- `closed` = module fully covered (`integrate`/`reject_with_rationale` for each signal)

| Bucket | Scope | State | Notes |
|---|---|---|---|
| `apk_report_lane` | `analysis-summary`, class index, hotspot/signals reports | `closed` | signal inventory fixed and transferred into round matrix |
| `winmonitor_schema_lane` | `com/winemu/core/server/winmonitor/*` model/command signals | `closed` | integrated as forensic runtime snapshot contract |
| `perf_renderdoc_lane` | `server/perf/*`, `server/renderdoc/*`, plugin presence | `closed` | integrated through issue-bundle runtime snapshot + existing runtime log lanes |
| `trans_layer_lane` | `com/winemu/core/trans_layer/*`, `openapi/*` | `closed` | bounded integrate: existing Aeolator runtime/env profile lane retained as authoritative |
| `native_binary_lane` | `.so` internals and JNI/native runtime glue | `closed` | rejected for app-tree; bounded to runtime/native owner repos |

## Current strict-round note

- This file is Round 9 inventory control for APK donor exhaustion.
- Round closure authority is `R9_GAMEHUBAPK_MATRIX.md` plus final transfer decisions.

## Progress log

- `2026-03-05 / pass 1`:
  - Round 9 coverage file initialized.
  - APK report inventory locked to strict buckets.
- `2026-03-05 / pass 2`:
  - forensic runtime snapshot lane integrated in app tree and wired into issue-bundle path.
- `2026-03-05 / closure`:
  - all buckets moved to `closed`; Round 9 file-level coverage completed.
