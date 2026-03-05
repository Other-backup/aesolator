# Round 3 Matrix: `Open-Wine-Components/umu-launcher`

Date: `2026-03-05`  
Round state: `closed`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/umu-launcher`

Primary code surfaces:
- `umu/umu_run.py`
- `umu/umu_runtime.py`
- `umu/umu_proton.py`
- `umu/umu_bspatch.py`
- `umu/umu_plugins.py`
- `umu/umu_log.py`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| Resume-safe runtime fetch/install protocol | `umu_runtime.py`, `umu_run.py` | `ContentsManager`, `Downloader`, package lanes | `integrated` | `integrate` |
| Proton route and passthrough semantics | `umu_proton.py`, `umu_run.py` | `XServerDisplayActivity`, launch env route contracts | `integrated` | `integrate` |
| Runtime provenance and structured log markers | `umu_log.py`, `umu_run.py` | `ForensicLogger`, issue-bundle metadata | `integrated` | `integrate` |
| Delta update / bspatch flow | `umu_bspatch.py` | package update lanes in `ContentsManager` | `rejected` | `reject_with_rationale` |
| Plugin execution model | `umu_plugins.py` | Aeolator runtime hooks | `rejected` | `reject_with_rationale` |
| Steam/VDF-specific glue | `umu/vdf/*` | launcher metadata parsing | `rejected` | `reject_with_rationale` |

## Closure Criteria For Round 3

Round 3 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. Regression gates pass for touched lanes (`contents_update`, `runtime_route`, `forensic_provenance`).
3. Round summary is committed with explicit donor coverage report.

## Progress Log

### 2026-03-05 / Pass 1

- Round 3 opened after Round 2 closure.
- Matrix initialized from local donor mirror with strict lane mapping.

### 2026-03-05 / Pass 2

- Runtime provenance lane started:
  - wrapper env/contract ingestion now stamps explicit source markers:
    - `AERO_RUNTIME_WRAPPER_ENV_SOURCE`
    - `AERO_RUNTIME_WRAPPER_PACKAGE`
    - `AERO_RUNTIME_WRAPPER_VERSION`

### 2026-03-05 / Pass 3

- Runtime contract forensic markers added:
  - `RUNTIME_WRAPPER_CONTRACT_APPLIED` event now captures package/version/lane/route/no-proton/passthrough markers.

### 2026-03-05 / Closure

- Round 3 closed.
- Rejected signal rationale:
  - `umu_bspatch.py` lane rejected: Aeolator uses full package replacement + install-stage recovery; binary delta patching adds high corruption risk on Android storage and gives low value for current WCP lanes.
  - `umu_plugins.py` rejected: no dynamic external plugin execution contract in Aeolator runtime model.
  - `umu/vdf/*` rejected: Steam/VDF metadata parsing is outside Aeolator launcher scope.
