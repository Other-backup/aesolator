# LSFG Runtime Contract (Canonical)

Date: 2026-05-04

## Scope

Canonical LSFG/framegen contract across:
- global profile storage,
- container overrides,
- shortcut overrides,
- launcher env export,
- forensic snapshotting.

## Canonical fields

- `lsfg_backend`: backend identifier.
- `lsfg_framegen_enabled`: boolean.
- `lsfg_mode`: normalized mode enum.
- `lsfg_source_fps`: integer.
- `lsfg_target_fps`: integer.
- `lsfg_generated_frames`: integer.
- `lsfg_thermal_guard`: enum/threshold preset.
- `lsfg_debug_flags`: bitset/string list.

## Override precedence

1. Shortcut override (highest)
2. Container override
3. Global profile (default)

## Normalization requirements

- Boolean values must stay boolean through the full path (no ad-hoc `"0"/"1"` re-encoding without explicit mapper).
- Mode values must be enum-normalized once in shared mapper, not duplicated per UI.
- Numeric ranges validated at input + export boundaries.

## Legacy compatibility

- Read legacy keys via explicit mapping table.
- Write canonical keys only.
- Emit one deprecation forensic marker when legacy keys were consumed.

## Forensics contract

Before container launch, emit one structured snapshot:
- `event=lsfg_config_effective`
- resolved values for all canonical fields,
- source of each field (`global/container/shortcut/default`),
- compatibility markers (`legacy_key_used=true/false`).

## Security and safety

- No untrusted free-form env keys pass-through.
- Only whitelisted LSFG keys exported to launcher env.
- Debug flags gated to avoid accidental high-overhead defaults.
