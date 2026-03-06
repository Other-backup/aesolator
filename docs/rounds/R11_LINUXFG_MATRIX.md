# Round 11 Matrix: `xXJSONDeruloXx/linux-fg`

Date: `2026-03-05`  
Round state: `gate_hold`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/linux-fg`

Primary donor anchors:
- `readme.md` (runtime flags and target-fps/interpolation contract)
- `src/main.cpp` (CLI arg parsing and frame pacing loop)
- `src/scaler.hpp` (`ScalerConfig` contract: `targetFps`, `enableInterpolation`, `interpolationFactor`)
- `src/frame_manager.hpp` (interpolation pipeline contract)

## Transfer Matrix

| Signal Cluster | Donor Anchor | Aeolator Target | Status | Decision |
|---|---|---|---|---|
| Target FPS contract | `readme.md`, `src/main.cpp` (`--target-fps`) | shortcut slider `upscalerTargetFps` + runtime env | `integrated` | `integrate` |
| Interpolation factor contract | `readme.md`, `src/main.cpp` (`--interpolation-factor`) | shortcut slider `upscalerInterpolationFactor` + runtime env | `integrated` | `integrate` |
| Interpolation enable/disable toggle | `src/main.cpp` (`--no-interpolation`) | existing frame-generation toggle bridge | `integrated` | `integrate` |
| Frame pacing policy | `src/main.cpp` (`frameDelay = 1000 / targetFps`) | env policy export + forensic runtime trace | `integrated` | `integrate` |
| Standalone X11 capture and Vulkan compute app | `window_capture.*`, `vulkan_context.*`, `scaler.*` | app-tree runtime | `rejected` | `reject_with_rationale` |
| Direct donor shader runtime import | `shaders/*` | app-tree | `rejected` | `reject_with_rationale` |

## Progress Log

### 2026-03-05 / Pass 1

- Extended `AE Upscaler / Frame Generation` UI contract:
  - added `Target FPS` and `Interpolation Factor` controls;
  - persisted values in shortcut keys (`upscalerTargetFps`, `upscalerInterpolationFactor`).
- Runtime lane integrated in `XServerDisplayActivity`:
  - parse from shortcut and clamp values for runtime use;
  - export env vars:
    - `AERO_UPSCALER_TARGET_FPS`
    - `AERO_FRAMEGEN_INTERPOLATION_FACTOR`
    - `AERO_MOBFGSR_TARGET_FPS`
    - `AERO_MOBFGSR_INTERPOLATION_FACTOR`
- Forensic lane updated:
  - `UPSCALER_ROUTE_APPLIED` includes `target_fps` and `interpolation_factor`.

### 2026-03-06 / Pass 2

- Added preset-aware FPS/interpolation policy lane:
  - framegen values are now resolved as `raw -> effective` by `upscalerPreset` and SoC class.
- Added runtime preset env for linux-fg-like consumers:
  - `AERO_UPSCALER_PRESET_REQUESTED`;
  - `AERO_UPSCALER_PRESET_EFFECTIVE`;
  - `AERO_UPSCALER_SOC_CLASS`.
- Forensic lane extended with effective pacing fields:
  - `target_fps_effective`;
  - `interpolation_factor_effective`.

## Open For Round Closure

1. Wire package-side consumers for both raw and effective pacing vars in `wcp archive` lanes.
2. Validate policy matrix behavior for `framegen off/on` guards and preset clamps by DX route.
3. Run closure regression gates for launch/runtime/graphics-center/forensic issue bundle.
