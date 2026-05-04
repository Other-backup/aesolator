# Winlator-Ludashi Transfer Matrix (Black Diamond)

Date: 2026-05-04  
Source branch: `The412Banner/Winlator-Ludashi@lsfg-vk-color-fix` (16 commits target lane)  
Target repo: `aesolator`

## Purpose

Formalize commit-by-commit transfer decisions for LSFG/Vulkan/UI launcher work with explicit risk controls, rollback readiness, and forensic observability.

## Decision legend

- **import now**: direct, contract-safe integration candidate for immediate implementation.
- **adapt later**: useful concept; requires contract refactor/feature-flag/ABI proof first.
- **hold**: blocked by missing evidence or runtime validation.
- **reject**: out-of-scope or contract-conflicting for `aesolator`.

## Commit matrix

| SHA | Commit summary | Type | Subsystem | Product relevance | Decision | Rationale / notes |
|---|---|---|---|---|---|---|
| `a4412bc` | `feat(lsfg-vk): add manager classes + launcher env hook` | feat | launcher env, runtime contract | high | adapt later | High-value donor logic, but must be normalized through local `RuntimeSignalContract` + LSFG schema parity before merge. |
| `42ae6e8` | `feat(lsfg-vk): add in-game quick menu + global DLL import UI` | feat | UI, runtime toggles | high | adapt later | Useful UX surface, but only after unified Global/Container/Shortcut override contract and forensic proof events are in place. |
| `7d788ac` | `fix(lsfg-vk): use existing FieldSet style` | fix | UI style system | medium | import now | Low-risk UI consistency improvement if mapped to current style primitives without introducing duplicate widgets. |
| `05c0c07` | `fix(lsfg-vk): replace layer .so with color-format-fix build` | fix | native `.so`, Vulkan layer | high | hold | Binary swap prohibited until ABI/loader dossier and staged rollout gates pass. |
| `e6351bd` | `revert: vulkan swap-channel patch (partial)` | revert | renderer path | high | import now (policy only) | Revert history is evidence that prior layer-level channel manipulation was unstable; transfer as safety rule and guard strategy, not as blind code copy. |
| `a6b10ce` | `revert: vulkan swap-channel patch (follow-up)` | revert | renderer path | high | import now (policy only) | Same as above; preserve clean fallback route and per-device flagging policy. |
| `TBD-01` | workflow/trigger utility commit | ci | workflow | low | reject | Non-product CI glue from donor must not override local pipeline discipline. |
| `TBD-02` | workflow/trigger utility commit | ci | workflow | low | reject | Same as above. |
| `TBD-03` | merge/sync helper commit | chore | repo hygiene | low | reject | Not user-visible runtime behavior. |
| `TBD-04` | lsfg-vk incremental tweak | fix | lsfg runtime | medium | hold | Needs exact diff capture + compatibility proof. |
| `TBD-05` | lsfg-vk incremental tweak | fix | lsfg runtime | medium | hold | Needs exact diff capture + compatibility proof. |
| `TBD-06` | lsfg-vk incremental tweak | feat | lsfg runtime | medium | hold | Needs exact diff capture + compatibility proof. |
| `TBD-07` | lsfg-vk incremental tweak | feat | lsfg runtime | medium | hold | Needs exact diff capture + compatibility proof. |
| `TBD-08` | lsfg-vk incremental tweak | fix | renderer | medium | hold | Requires Vulkan format-route audit first. |
| `TBD-09` | lsfg-vk incremental tweak | fix | renderer | medium | hold | Requires Vulkan format-route audit first. |
| `TBD-10` | misc housekeeping | chore | docs/meta | low | reject | No runtime value for app contract closure. |

> `TBD-*` rows are intentionally unresolved until branch-dump evidence is captured locally and SHA-verified in the next harvest.

## Target mapping for import/adapt lanes

### A) LSFG runtime contract unification

Candidate target files:
- `app/src/main/java/com/winlator/cmod/contentdialog/AdrenotoolsFragment.java`
- `app/src/main/java/com/winlator/cmod/contentdialog/ContainerDetailFragment.java`
- `app/src/main/java/com/winlator/cmod/core/ShortcutSettingsDialog.java`
- `app/src/main/java/com/winlator/cmod/core/UpscalerProfileStore.java`
- `app/src/main/java/com/winlator/cmod/core/RuntimeSignalContract.java`

Required contract dependencies:
- launcher env export must match canonical field set,
- Global/Container/Shortcut override precedence,
- legacy key migration mapping,
- forensic snapshot emission before container start.

### B) Vulkan color-format guard lane

Candidate target files:
- `app/src/main/java/com/winlator/renderer/VulkanRenderer.java` (or current renderer owner path)
- runtime launch path + diagnostics surfaces under `cmod`.

Required gates:
- per-container/per-shortcut feature flag,
- GPU/driver-route checks (Adreno/Mali, Turnip/Zink),
- fallback to clean route by default,
- explicit reason logging on fallback.

### C) Native layer binary lane

Candidate target files/artifacts:
- loader entry integration path in `WinlatorNative` and packaging metadata.

Required gates:
- ABI match (arch set + symbol dependencies),
- imported/exported symbol ledger,
- NEEDED chain validation,
- rollout flag + rollback switch.

## Revert root-cause capture rules

For every reverted renderer change from donor history, record:
1. intended fix objective,
2. exact pipeline level touched (swapchain format, shader swizzle, compositor pass, etc.),
3. observed regression signature,
4. why fallback path remained stable,
5. how local guard prevents repetition.

## Black Diamond Evidence Ledger

### Source proof (must have)
- donor SHA list and per-commit diff snapshots,
- mapping from donor path to local owner path,
- explicit accept/preserve/merge/synthesize/reject decision per commit.

### Runtime proof (must have)
- launch-time `lsfg_config_effective` forensic snapshot,
- renderer-route telemetry (`vulkan_color_fix_enabled`, format, fallback reason),
- before/after device screenshot + runtime log pairing.

### Risk class
- **High**: native `.so` replacement, renderer format route mutations.
- **Medium**: launcher env normalization + quick-menu integration.
- **Low**: pure style/UI consistency changes.

### Rollback plan
- keep feature flags default-off for new renderer/native routes,
- preserve fallback launch/env path,
- one-command disable gate for LSFG Vulkan color fix and new binary lane,
- documented rollback checklist in release notes before enabling by default.
