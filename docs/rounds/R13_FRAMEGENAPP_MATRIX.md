# Round 13 Matrix: `proqaz2-design/Frame-generation-`

Date: `2026-03-05`  
Round state: `gate_hold`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/Frame-generation-`

Primary donor anchors:
- `README.md` (mode model and Android runtime constraints)
- `app/src/main/java/com/framegen/app/MainActivity.kt` (mode/quality/thermal UI contracts)
- `app/src/main/java/com/framegen/app/engine/FrameGenEngine.kt` (mode + quality + target FPS bridge)
- `app/src/main/cpp/framegen_types.h` (mode enum, thermal_protection, quality/model_scale)
- `app/src/main/cpp/pipeline/timing_controller.*` (frame budget + thermal adaptation strategy)

## Transfer Matrix

| Signal Cluster | Donor Anchor | Aeolator Target | Status | Decision |
|---|---|---|---|---|
| Framegen mode profile (`balanced/quality/low latency`) | `MainActivity.kt`, `framegen_types.h` | shortcut + runtime env policy lane | `integrated` | `integrate` |
| Thermal guard toggle | `MainActivity.kt`, `timing_controller.*` | shortcut + runtime env policy lane | `integrated` | `integrate` |
| Mode-based quality/scale/budget policy | `framegen_types.h`, `timing_controller.cpp` | runtime env hints for MobFGSR lane | `integrated` | `integrate` |
| Native Vulkan layer interception and JNI engine | `cpp/vulkan/*`, `framegen_jni.cpp`, `FrameGenEngine.kt` | app-tree | `rejected` | `reject_with_rationale` |
| Shizuku/ADB layer control app flow | `MainActivity.kt`, Android services | app-tree | `rejected` | `reject_with_rationale` |

## Progress Log

### 2026-03-05 / Pass 1

- Added new upscaler/framegen controls:
  - `Framegen Mode` (`balanced`, `quality`, `low_latency`);
  - `Thermal Guard` (boolean).
- Persisted new shortcut keys:
  - `upscalerFramegenMode`;
  - `upscalerThermalGuard`.
- Runtime lane updated in `XServerDisplayActivity`:
  - parse + normalize mode and thermal guard;
  - export generic env:
    - `AERO_FRAMEGEN_MODE`
    - `AERO_FRAMEGEN_THERMAL_GUARD`
  - export MobFGSR policy env:
    - `AERO_MOBFGSR_MODE`
    - `AERO_MOBFGSR_THERMAL_GUARD`
    - `AERO_MOBFGSR_MODEL_SCALE`
    - `AERO_MOBFGSR_QUALITY`
    - `AERO_MOBFGSR_FRAME_BUDGET_MS`
- Forensic lane updated:
  - `UPSCALER_ROUTE_APPLIED` includes `framegen_mode` and `thermal_guard`.

### 2026-03-06 / Pass 2

- Added SoC-aware framegen preset lane (`upscalerPreset`) and runtime auto-resolution.
- Added effective thermal/pacing policy export:
  - `AERO_UPSCALER_PRESET_REQUESTED`;
  - `AERO_UPSCALER_PRESET_EFFECTIVE`;
  - `AERO_UPSCALER_SOC_CLASS`;
  - effective framegen values are clamped by preset before env export.
- Forensic marker updated with raw/effective thermal + pacing fields for debug parity.

## Open For Round Closure

1. Validate runtime package consumption of mode/thermal vars under preset-aware policy.
2. Confirm profile behavior under sustained thermal load on device across preset lanes.
3. Run closure regression gates on launch route + graphics center + forensic issue bundle.
