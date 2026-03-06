# Round 12 Matrix: `Nukem9/dlssg-to-fsr3`

Date: `2026-03-05`  
Round state: `gate_hold`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/dlssg-to-fsr3`

Primary donor anchors:
- `README.md` (drop-in interposer behavior and runtime log/debug model)
- `resources/dlssg_to_fsr3.ini` (debug keys)
- `source/maindll/Util.cpp` (env-first setting resolution via `DLSSGTOFSR3_*`)
- `source/maindll/FFFrameInterpolator.cpp` (debug overlay/tear-lines/interpolated-only dispatch toggles)

## Transfer Matrix

| Signal Cluster | Donor Anchor | Aeolator Target | Status | Decision |
|---|---|---|---|---|
| Debug overlay flag | `EnableDebugOverlay` | shortcut contract + runtime env bridge | `integrated` | `integrate` |
| Debug tear-lines flag | `EnableDebugTearLines` | shortcut contract + runtime env bridge | `integrated` | `integrate` |
| Interpolated-only flag | `EnableInterpolatedFramesOnly` | shortcut contract + runtime env bridge | `integrated` | `integrate` |
| Env-first settings override model (`DLSSGTOFSR3_*`) | `Util.cpp::GetSetting` | compatibility env bridge in upscaler runtime lane | `integrated` | `integrate` |
| DLL interposer/hooking architecture (`version/winhttp/dbghelp`) | `README.md`, `source/wrapper_*` | app-tree java runtime | `rejected` | `reject_with_rationale` |
| Direct NGX/DX/Vulkan wrapper implementation | `source/maindll/*`, `source/wrapper_*` | app-tree | `rejected` | `reject_with_rationale` |

## Progress Log

### 2026-03-05 / Pass 1

- Added debug controls to `AE Upscaler / Frame Generation` UI:
  - `Framegen Debug Overlay`;
  - `Framegen Debug Tear Lines`;
  - `Show Interpolated Frames Only`.
- Persisted shortcut keys:
  - `upscalerDebugOverlay`;
  - `upscalerDebugTearLines`;
  - `upscalerInterpolatedOnly`.
- Runtime/export integration in `XServerDisplayActivity`:
  - unified env lane:
    - `AERO_FRAMEGEN_DEBUG_OVERLAY`
    - `AERO_FRAMEGEN_DEBUG_TEAR_LINES`
    - `AERO_FRAMEGEN_INTERPOLATED_ONLY`
  - MobFGSR lane:
    - `AERO_MOBFGSR_DEBUG_OVERLAY`
    - `AERO_MOBFGSR_DEBUG_TEAR_LINES`
    - `AERO_MOBFGSR_INTERPOLATED_ONLY`
  - Translator-compatible bridge:
    - `DLSSGTOFSR3_EnableDebugOverlay`
    - `DLSSGTOFSR3_EnableDebugTearLines`
    - `DLSSGTOFSR3_EnableInterpolatedFramesOnly`
- Forensic event `UPSCALER_ROUTE_APPLIED` extended with debug fields.

### 2026-03-06 / Pass 2

- Integrated preset-aware bridge control:
  - `DLSSGTOFSR3_*` bridge lane now runs against effective framegen policy
    (`preset + SoC` resolved values), not only raw shortcut values.
- Added explicit preset trace markers for bridge diagnostics:
  - `AERO_UPSCALER_PRESET_REQUESTED`;
  - `AERO_UPSCALER_PRESET_EFFECTIVE`;
  - `AERO_UPSCALER_SOC_CLASS`.

## Open For Round Closure

1. Validate downstream runtime package consumption of `DLSSGTOFSR3_*` bridge vars under preset-aware policy.
2. Verify end-to-end behavior in DXVK/VKD3D wrapper path with FG toggled on/off and preset switching.
3. Run closure regression gates for graphics center + launch route + forensic issue bundle completeness.
