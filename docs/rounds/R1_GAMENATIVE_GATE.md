# Round 1 Gate Checklist (`GameNative`)

Date: `2026-03-05`  
Round state: `gate_hold`

## Gate Rule

Round 1 can move `gate -> closed` only after these checks are green on CI/runtime validation.

## CI Checks (mandatory)

1. Workflow `Build Aesolator APK (native source tree)` succeeds on current `main`.
2. Release publish step updates `winlator-latest` with current APK/SHA/docs assets.
3. No new runtime/package regressions in workflow logs (`contents`, `wine/proton`, `dxvk/vkd3d/dgvoodoo` paths).

## Runtime Smoke Checks (mandatory)

1. Container create/edit:
   - Wine/Proton selector is populated correctly.
   - Edit mode allows runtime lane change when packages exist.
2. Launch preflight:
   - Missing runtime/wrapper payloads are blocked early with explicit message.
   - Forensic events contain dependency failure reason and dependency id.
3. Wrapper lanes:
   - `dxvk+vkd3d` route launches when payload is present.
   - `dgvoodoo` route reports missing package clearly if not installed.
4. Task manager:
   - Windows + Linux tabs update in real time.
   - Process details open and show telemetry fields.
5. DRI3 controls:
   - forensic/settings toggles propagate to runtime env and persist.

## Current note

- Local build execution intentionally skipped in this pass (CI-only build policy).
- Code and docs moved to `gate`; closure awaits CI + runtime smoke confirmation.
- Owner override switched execution to Round 2; this round is frozen as `gate_hold` until closure pass resumes.

## Soft Handoff To Round 2

To avoid hard stop between rounds, the following continuity contract is fixed:
1. Launch dependency framework from Round 1 remains active as baseline for Round 2+.
2. Runtime selector and version normalization from Round 1 are not reverted during Round 2 work.
3. Round 1 CI closure is deferred, not discarded; `gate_hold` preserves completion context.
