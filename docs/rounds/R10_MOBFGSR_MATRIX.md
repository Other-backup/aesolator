# Round 10 Matrix: `Mob-FGSR/MobFGSR`

Date: `2026-03-05`  
Round state: `gate_hold`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/MobFGSR`

Primary donor anchors:
- `README.md` (mode and parameter contract)
- `src/offscreen_renderer.h` (runtime knobs: SR/FG mode, generated frames, render scale, thresholds)
- `src/offscreen_renderer.cpp` (execution sequencing: rendered vs generated frames, jitter, interpolation path)

## Transfer Matrix

| Signal Cluster | Donor Anchor | Aeolator Target | Status | Decision |
|---|---|---|---|---|
| SR/FG mode split (`enableSuperResolution`, `enableInterpolation`) | `offscreen_renderer.h` | shortcut upscaler contract (`backend/effect/framegen`) | `integrated` | `integrate` |
| Generated frame count contract | `generatedFramesCount` | `upscalerGeneratedFrames` shortcut key + runtime env | `integrated` | `integrate` |
| Render scale contract | `upsampleScale` | `upscalerScale` shortcut key + runtime env | `integrated` | `integrate` |
| Shader threshold knobs (`depth/color thresholds`) | threshold fields in `offscreen_renderer.h` | runtime env placeholders for MobFGSR lane | `integrated` | `integrate` |
| Frame pipeline sequencing (`rendered`/`generated` cycle) | `offscreen_renderer.cpp::execute()` | runtime env guard for framegen on DXVK route | `integrated` | `integrate` |
| Offline IO pipeline (PNG sequence, OpenGL compute app) | `load()/save()` and standalone renderer architecture | app-tree runtime | `rejected` | `reject_with_rationale` |

## Progress Log

### 2026-03-05 / Pass 1

- Rebuilt upscaler from scratch in app tree:
  - replaced legacy `vkBasalt sharpness-only` UI section with unified `AE Upscaler / Frame Generation` section;
  - added backend/effect/scale/frame-generation/generated-frames controls with backward-compatible persistence.
- Runtime lane integrated:
  - `XServerDisplayActivity` now parses new upscaler contract from shortcut extras;
  - exports unified env contract (`AERO_UPSCALER_*`, `AERO_FRAMEGEN_*`) and MobFGSR-specific placeholders (`AERO_MOBFGSR_*`);
  - keeps vkBasalt bridge active when `backend=vkbasalt`.
- Forensic and signal lane integrated:
  - added `UPSCALER_ROUTE_APPLIED` forensic event with resolved contract fields;
  - bound upscaler guard reason into `RuntimeSignalContract` (framegen guard on non-DXVK route).

### 2026-03-06 / Pass 2

- Added explicit upscaler preset contract in shortcut/runtime lane:
  - `upscalerPreset` (`auto`, `conservative`, `balanced`, `aggressive`).
- Added SoC-aware preset auto-resolver in runtime:
  - `auto -> conservative/balanced/aggressive` by detected SoC class.
- Added effective policy export for runtime consumers:
  - `AERO_UPSCALER_PRESET_REQUESTED`;
  - `AERO_UPSCALER_PRESET_EFFECTIVE`;
  - `AERO_UPSCALER_SOC_CLASS`;
  - `AERO_MOBFGSR_PRESET`;
  - `AERO_MOBFGSR_SOC_CLASS`.
- Added preset-aware clamp lane for framegen:
  - effective generated-frames / target-fps / interpolation / thermal-guard;
  - preset-tuned MobFGSR thresholds.
- Extended forensic marker `UPSCALER_ROUTE_APPLIED`:
  - requested/effective preset, SoC class, raw/effective framegen values.

## Open For Round Closure

1. Validate runtime consumption lane for `AERO_MOBFGSR_*` and preset env (`AERO_UPSCALER_PRESET_*`) in runtime packages (`wcp archive`), not only env emission.
2. Finalize package-side per-game preset mapping policy for runtime consumers (app-tree emit is complete).
3. Run regression gates on launch/runtime route + graphics center + forensic issue-bundle completeness.
