# R10-R14 Status Snapshot

Date: `2026-03-13`

This snapshot normalizes the actual state of donor rounds `R10-R14`.

Important distinction:
- `integrated in tree` means the app-side transfer is already landed in `aeolator`;
- `round closed` means the donor is fully exhausted and all closure gates have passed.

For `R10-R14`, the app-side transfer is integrated, but the rounds are still not
closed. All five remain in `gate_hold`.

## Summary

| Round | Donor | App-Side State | Queue State | What Is Still Open |
|---:|---|---|---|---|
| 10 | `Mob-FGSR/MobFGSR` | integrated | `gate_hold` | runtime package consumption of `AERO_MOBFGSR_*` / preset env, per-game preset mapping policy, closure regression gates |
| 11 | `xXJSONDeruloXx/linux-fg` | integrated | `gate_hold` | package-side consumption of raw/effective pacing vars, DX-route behavior validation, closure regression gates |
| 12 | `Nukem9/dlssg-to-fsr3` | integrated | `gate_hold` | downstream wrapper consumption of `DLSSGTOFSR3_*`, DXVK/VKD3D FG-route validation, closure regression gates |
| 13 | `proqaz2-design/Frame-generation-` | integrated | `gate_hold` | runtime consumption of mode/thermal env, sustained thermal validation on device, closure regression gates |
| 14 | `optiscaler/OptiScaler` | integrated | `gate_hold` | wrapper consumption of FG source/output hints with preset markers, `dlssg_to_fsr3` route validation, closure regression gates |

## What Is Already Closed In Tree

### R10 `Mob-FGSR/MobFGSR`

Closed in tree:
- unified `AE Upscaler / Frame Generation` app-side lane;
- SR/FG mode split;
- generated frame count contract;
- render scale contract;
- threshold env placeholders for MobFGSR lane;
- runtime route guard and forensic emission;
- SoC-aware preset lane with requested/effective markers and preset-aware clamps.

Still open:
- runtime/archive package-side consumption of MobFGSR env;
- final per-game preset mapping policy;
- launch/runtime/graphics-center/forensic closure gates.

### R11 `linux-fg`

Closed in tree:
- `Target FPS`;
- `Interpolation Factor`;
- interpolation enable/disable bridge;
- pacing env export;
- preset-aware raw/effective pacing export;
- forensic extension for effective pacing.

Still open:
- package-side consumption of pacing vars;
- DX route guard validation for framegen/preset clamps;
- closure regression gates.

### R12 `dlssg-to-fsr3`

Closed in tree:
- debug overlay flag;
- debug tear-lines flag;
- interpolated-only flag;
- env-first `DLSSGTOFSR3_*` bridge model;
- preset-aware bridge visibility and forensic trace.

Still open:
- downstream runtime/wrapper consumption of bridge vars;
- DXVK/VKD3D FG-path validation;
- closure regression gates.

### R13 `Frame-generation-`

Closed in tree:
- framegen mode profile (`balanced`, `quality`, `low_latency`);
- thermal guard toggle;
- mode-based quality/model-scale/frame-budget env hints;
- preset-aware effective thermal/pacing export;
- forensic parity for raw/effective thermal fields.

Still open:
- runtime package consumption of mode/thermal env;
- sustained thermal/device validation;
- closure regression gates.

### R14 `OptiScaler`

Closed in tree:
- FG source / FG output split;
- source/output runtime env bridge;
- `AERO_DLSSG_TO_FSR3_BRIDGE`;
- preset-aware source/output routing with SoC trace markers.

Still open:
- wrapper consumption of FG source/output hints in package/runtime lanes;
- `dlssg_to_fsr3` DX route validation;
- closure regression gates.

## Event Timeline

- `2026-03-05`: rounds `R10-R14` were opened and app-side transfer matrices were initialized.
- `2026-03-05`: first-pass app-side transfers for each round landed in `aeolator`.
- `2026-03-06`: second-pass preset-aware / effective-policy integration landed for all five rounds.
- `2026-03-06`: all five rounds were frozen as `gate_hold` by owner override so execution could move forward without forcing final gate closure in that phase.

## Current Interpretation

The correct short status is:
- `R10-R14 app-side integrated`
- `R10-R14 donor rounds not closed`
- `R10-R14 still blocked on runtime/archive consumer validation + regression/device gates`
