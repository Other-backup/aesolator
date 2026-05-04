# LSFG Vulkan Color Route Audit

Date: 2026-05-04

## Objective

Prevent unstable channel-swap style regressions by introducing a guarded, observable color-format fix lane rather than direct donor renderer mutation.

## Audit checkpoints

1. **Format ownership**
   - Identify actual swapchain/resource formats seen on device.
   - Separate render-target format logic from presentation-layer assumptions.

2. **Pipeline stage ownership**
   - Record where channel manipulation occurs (if any): shader pass, copy pass, compositor pass, or swapchain presentation route.

3. **Driver lane guards**
   - Gate behavior by GPU/driver route families (Adreno/Mali; Turnip/Zink).

4. **Fallback policy**
   - Clean path remains default.
   - Color-fix route activates only when explicitly enabled and compatibility checks pass.

## Required telemetry

- `vulkan_color_fix_enabled`
- `vulkan_detected_format`
- `vulkan_color_fix_guard_lane`
- `vulkan_color_fix_fallback_reason`

## Rollout policy

- Stage 0: telemetry only, no behavior change.
- Stage 1: opt-in per shortcut/container.
- Stage 2: optional default-on only after multi-device proof.

## Evidence bundle before widening

- screenshot proof (before/after),
- runtime forensic proof,
- code-path proof with exact guarded branch.
