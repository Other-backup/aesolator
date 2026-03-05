# Round 8 Matrix: `coffincolors/winlator`

Date: `2026-03-05`  
Round state: `active`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/winlator`

Primary code surfaces:
- `app/src/main/java/com/winlator/*`
- `app/src/main/java/com/winlator/core/*`
- `app/src/main/java/com/winlator/xenvironment/*`
- `app/src/main/java/com/winlator/xserver/*`
- `app/src/main/java/com/winlator/contentdialog/*`
- `app/src/main/java/com/winlator/winhandler/*`
- `app/src/main/cpp/*`

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| cmod-bionic runtime identity/compat contracts | runtime/env and metadata lanes | runtime env + forensic metadata | `in_progress` | `integrate` |
| launcher/runtime startup discipline | `MainActivity`, `XServerDisplayActivity`, `xenvironment/components/*` | launchdeps + xenvironment contracts | `pending` | `integrate` |
| graphics/dialog contract deltas | `contentdialog/*`, settings/container UI hooks | Graphics center + settings contracts | `pending` | `integrate` |
| winhandler/task-manager behavioral deltas | `winhandler/*` | task manager + process diagnostics lane | `pending` | `integrate` |
| xserver/java extension deltas | `xserver/*` and extensions/requests | `cmod/xserver/*` | `pending` | `integrate` |
| native cpp patch-set and low-level runtime glue | `app/src/main/cpp/*` | native/runtime owner repos (outside app-tree) | `pending` | `reject_with_rationale` |

## Closure Criteria For Round 8

Round 8 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. App-tree integrations are contract-level ports (no blind donor copy).
3. Native runtime rows are explicitly bounded to owning repos/layers.
4. Round summary is committed with coverage closure.

## Progress Log

### 2026-03-05 / Pass 1

- Round 8 opened after Round 7 closure.
- Base transfer lanes initialized for cmod-bionic runtime contracts, launcher discipline, graphics dialog deltas, task-manager lane, and xserver deltas.
- Inventory anchors fixed from donor tree:
  - total files: `1132`
  - policy/code files: `684`
  - java mainline lane: `281`
  - native cpp lane: `209`
  - res/manifest lane: `440`
