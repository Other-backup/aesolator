# Round 9 Matrix: `GameHub-Lite-5.3.3-RC2.apk`

Date: `2026-03-05`  
Round state: `closed`

## Round Scope

Donor source:
- `/home/mikhail/Загрузки/GameHub-Lite-5.3.3-RC2.apk`

Primary analysis artifacts:
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/analysis-summary.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/jadx-class-index.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/jadx-relevant-class-candidates.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/dex-winmonitor-string-signals.txt`
- `/home/mikhail/work/apk-analysis/GameHub-Lite-5.3.3-RC2/reports/native-strings-scan.txt`

Primary integration surface:
- `app/src/main/java/com/winlator/cmod/core/ForensicRuntimeSnapshot.java`
- `app/src/main/java/com/winlator/cmod/core/ForensicIssueComposer.java`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| winmonitor process/thread contract signals (`ProcessListResponse`, `ThreadListResponse`, `owner_pid`, `thread_id`, `stack_frames`) | `com/winemu/core/server/winmonitor/*`, dex signal map | forensic issue-bundle runtime snapshot contract | `integrated` | `integrate` |
| perf/renderdoc diagnostics lane presence in runtime | `com/winemu/core/server/environment/plugins/PerfPlugin`, `RenderDocPlugin`, `server/perf/*`, `server/renderdoc/*` | forensic bundle now captures host/process runtime snapshot in addition to existing runtime logs | `integrated` | `integrate` |
| trans-layer template/config namespace presence | `com/winemu/core/trans_layer/*`, `openapi/GPUConfig`, `openapi/DirectRenderingMode` | existing runtime profile/env lanes remain authoritative; no conflicting donor override copied | `integrated` | `integrate` |
| native `.so` low-level runtime glue and embedded vendor internals | `libwinemu.so`, `libxserver.so`, `libvfs.so`, JNI symbols | app-tree boundary: native runtime owner repos only | `rejected` | `reject_with_rationale` |

## Closure Criteria For Round 9

Round 9 can be marked `closed` only when:
1. APK signal rows are finalized as `integrated` or `rejected` with rationale.
2. Integration is contract-level (no opaque donor binary transplant into app tree).
3. Forensic issue-bundle captures runtime/process state snapshot for postmortem.
4. Queue and roadmap are updated to `Round 9 = closed`.

## Progress Log

### 2026-03-05 / Pass 1

- Round 9 opened after Round 8 closure.
- APK analysis inventory confirmed:
  - `files_total=5793`
  - `dex_total=11`
  - `so_total=23`
  - `jadx_java_sources=20905`
  - `winemu_namespace_sources=171`
- High-signal namespaces fixed from reports:
  - `com/winemu/core/server/winmonitor/*`
  - `com/winemu/core/server/environment/plugins/*`
  - `com/winemu/core/trans_layer/*`
  - `com/winemu/openapi/*`

### 2026-03-05 / Pass 2

- Added `ForensicRuntimeSnapshot` contract in app tree:
  - captures host runtime snapshot (`/proc/stat`, `/proc/loadavg`, `/proc/meminfo`, `/proc/uptime`);
  - captures top process rows from `/proc/*/stat` + `/proc/*/cmdline`;
  - emits deterministic contract id: `apk_gamehub_winmonitor_perf_lane_v1`.
- `ForensicIssueComposer` now includes `runtime-snapshot.json` in every issue bundle.
- Forensic events added for capture result:
  - `FORENSIC_RUNTIME_SNAPSHOT_CAPTURED`
  - `FORENSIC_RUNTIME_SNAPSHOT_FAILED`

### 2026-03-05 / Closure

- Round 9 moved to `closed`.
- All transfer rows finalized as `integrated` or `rejected` with rationale.
