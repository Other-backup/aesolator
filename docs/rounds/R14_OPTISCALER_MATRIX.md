# Round 14 Matrix: `optiscaler/OptiScaler`

Date: `2026-03-05`  
Round state: `gate_hold`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/OptiScaler`

Primary donor anchors:
- `README.md` (Inputs/Outputs and FG Source/FG Output model)
- `Config.md` (runtime configuration contract mindset)
- `OptiScaler.ini` (overlay/runtime option routing)
- `Features.md` (wrapper-routing features)

## Transfer Matrix

| Signal Cluster | Donor Anchor | Aeolator Target | Status | Decision |
|---|---|---|---|---|
| Input/Output routing model | `README.md` (`Inputs -> OptiScaler -> Outputs`) | upscaler shortcut/runtime contract | `integrated` | `integrate` |
| FG Source / FG Output split | `README.md` (separated FG inputs/outputs) | `upscalerFgSource/upscalerFgOutput` + env bridge | `integrated` | `integrate` |
| Translator bridge routing | `Features.md` (dlssg-to-fsr3 integration) | `AERO_DLSSG_TO_FSR3_BRIDGE` + conditional `DLSSGTOFSR3_*` | `integrated` | `integrate` |
| Overlay/hook/runtime DLL architecture | `OptiScaler/*`, hooks, wrappers, proxies | app-tree java runtime | `rejected` | `reject_with_rationale` |
| API hooking engines (DX11/12/Vulkan wrappers) | `OptiScaler/inputs/*`, `hooks/*` | app-tree | `rejected` | `reject_with_rationale` |

## Progress Log

### 2026-03-05 / Pass 1

- Added framegen routing controls:
  - `FG Source`: `native`, `opti_fg`;
  - `FG Output`: `auto`, `mobfgsr`, `dlssg_to_fsr3`.
- Persisted new shortcut keys:
  - `upscalerFgSource`;
  - `upscalerFgOutput`.
- Runtime lane updates in `XServerDisplayActivity`:
  - normalized source/output values;
  - resolved `auto` output to active backend route;
  - exported env:
    - `AERO_FRAMEGEN_SOURCE`
    - `AERO_FRAMEGEN_OUTPUT`
    - `AERO_DLSSG_TO_FSR3_BRIDGE`
    - `AERO_MOBFGSR_FG_SOURCE`
    - `AERO_MOBFGSR_FG_OUTPUT`
- Forensic lane updated:
  - `UPSCALER_ROUTE_APPLIED` includes `fg_source` and `fg_output`.

### 2026-03-06 / Pass 2

- Integrated FG route resolution with preset-aware lane:
  - source/output routing now executes under effective preset policy (`preset + SoC`).
- Added explicit preset/env visibility for runtime wrappers:
  - `AERO_UPSCALER_PRESET_REQUESTED`;
  - `AERO_UPSCALER_PRESET_EFFECTIVE`;
  - `AERO_UPSCALER_SOC_CLASS`.
- Preset-aware MobFGSR threshold export added for route-side tuning.

## Open For Round Closure

1. Validate runtime wrappers consume FG source/output hints together with preset markers in `wcp archive`.
2. Confirm `dlssg_to_fsr3` output route behavior on FG-enabled DXVK path across preset lanes.
3. Run closure regression gates on graphics-center + launch route + forensic issue-bundle.
