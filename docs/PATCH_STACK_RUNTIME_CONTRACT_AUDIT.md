# Winlator Patch Stack Runtime Contract Audit

Generated: `2026-03-04T21:10:02Z`

## Scope

- Patch files scanned: `9`
- Target groups: `XServerDisplayActivity`, `GuestProgramLauncherComponent`, `RuntimeSignalContract`
- Contract: forensic telemetry + reason markers + runtime guard markers

## Results

### XServerDisplayActivity

- `telemetry_calls`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `reason_markers`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `fallback_guardrails`: `ok` (2 patches) -> `0001-mainline-full-stack-consolidated.patch`, `0006-mainline-dgvoodoo-contents-dxvk-route.patch`
- `external_signal_inputs`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `launch_env_signal_fields`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `contract_helper_usage`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`

### GuestProgramLauncherComponent

- `telemetry_calls`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `reason_markers`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `runtime_contract_markers`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `external_signal_markers`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`
- `contract_helper_usage`: `ok` (1 patches) -> `0001-mainline-full-stack-consolidated.patch`

### RuntimeSignalContract

- `policy_markers_constants`: `ok` (1 patches) -> `0003-mainline-add-missing-runtime-bridge-classes.patch`
- `input_markers_constants`: `ok` (1 patches) -> `0003-mainline-add-missing-runtime-bridge-classes.patch`
- `policy_hashing`: `ok` (1 patches) -> `0003-mainline-add-missing-runtime-bridge-classes.patch`

## Contract Summary

- All required runtime-contract checks are present in current patch stack.

