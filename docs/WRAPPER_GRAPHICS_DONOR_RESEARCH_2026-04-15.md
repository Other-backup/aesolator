# Wrapper Graphics Donor Research 2026-04-15

## Scope

Class under review:
- `Ae.solator` graphics-driver compatibility and wrapper/provider routing
- donor drift from official `Winlator`, `GameNative`, and active community forks

Goal:
- transfer only real graphics-driver logic into the local wrapper-first product line
- reject false equivalence between provider aliases and separate renderer routes
- keep graphics/XServer donor transfer as one closure lane when the user asks
  for a whole batch:
  source, assets, JNI/native, XServer/GLX seams, forensic/env routing, and
  shipped binary evidence all move together instead of as isolated patches

## Transfer Method

This lane does not count as transferred from source-only notes or UI-only
parity.

Required evidence surface:
- donor Java/Kotlin source
- donor/native app assets
- shipped renderer and launcher binaries:
  `libwinlator.so`, `libvortekrenderer.so`, `libvirglrenderer.so`,
  `libgladiorenderer.so`
- local `Ae.solator` Java/native/XServer integration points
- binary-visible truth:
  sections, imports/exports, strings, symbol surface, and hex-anchored
  offsets when they explain route ownership or runtime behavior

Canonical census for the current pass:
- `docs/GRAPHICS_XSERVER_BINARY_CENSUS_2026-04-16.md`

Execution rule:
- if the transfer is declared a one-batch graphics/XServer pass, do not reopen
  intermediate `gradlew` or device-validation loops until the whole
  source/binary/asset/XServer surface is reconciled
- source-only parity or UI-only parity does not count as transfer for this
  lane; the shipped donor binary surface must also be reconciled

## Donor Findings

### 1. Official Winlator keeps `Vortek` as a real graphics-driver route

Evidence:
- official Winlator 9 release notes mention `Vortek (Experimental)` and `VirGL (Universal) Graphics Driver`
- official Winlator issue reports still describe `Graphics Driver: VORTEK` as a live selected route with DXVK/VKD3D/WineD3D behavior differences

Implication:
- `Vortek` and `VirGL` are not the same class as `Turnip`, `Gladio`, or plain wrapper-provider aliases

### 2. GameNative moved graphics logic toward a richer wrapper/runtime surface

Evidence:
- `GameNative` release `v0.7.1` states:
  - `Added new wrapper - improved support/performance, especially for PowerVR/mali users`
  - `Added ability to download and apply drivers/contents/proton directly in the dropdown`

Implication:
- donor direction is not "one graphics driver string"; it is a composed lane:
  wrapper route + downloadable drivers + runtime/package routing

### 3. Community bionic/glibc forks treat wrapper and driver defaults as brittle owner classes

Evidence:
- `Succubussix/winlator-bionic-glibc` release notes mention:
  - fixed bug where games would not launch unless the Turnip version was manually changed
  - fixed stuck graphic-driver option
  - added experimental GPU drivers for newer Adreno classes

Implication:
- default driver selection and persisted graphics-driver state are still active bug surfaces in donor forks

### 4. Adreno driver distribution still assumes wrapper-first routing on Android

Evidence:
- `K11MCH1/AdrenoToolsDrivers` release notes explicitly say the drivers are compatible only through `Adrenotools settings` and the `Wrapper graphics driver`

Implication:
- on the local `Ae.solator` product line, `Turnip` and related Adreno provider packages belong under wrapper/provider routing, not as separate top-level renderer identities

## Local Product Decision

The local product remains wrapper-first for Android driver/provider management.

That means:
- keep `turnip`, `gladio`, `gladium`, `zink`, and `llvmpipe` as legacy compatibility inputs that normalize into the wrapper-controlled route
- do not silently relabel `Vortek` or `VirGL` as if they were the same thing as `freedreno-opengl`
- if legacy donor configs request `Vortek` or `VirGL`, degrade explicitly and preserve route identity in forensics/env

## Code Consequence

Implemented policy:
- `graphicsDriver` normalization stays compatible with wrapper-first runtime ownership
- `AERO_GRAPHICS_LEGACY_HINT` now separates:
  - `turnip-vulkan`
  - `freedreno-opengl`
  - `zink-opengl`
  - `llvmpipe-software`
  - `vortek-route`
  - `virgl-route`
- `AERO_GRAPHICS_LEGACY_POLICY` now separates:
  - `provider-compat`
  - `software-fallback`
  - `route-degraded`
- legacy donor route intent now survives normalized save/import cycles through
  `graphicsDriverConfig` metadata:
  - `legacyRequestedDriver`
  - `legacyProviderHint`
  - `legacyPolicy`
- runtime/env/forensics now treat imported `Vortek` or `VirGL` as explicit
  degraded external renderer requests, not as silent wrapper aliases

## Donor Reflection

### Official Winlator / Winlator 11 decompile

- confirmed that `Vortek` / `VirGL` remain renderer-route identities, not just
  provider aliases
- did **not** justify literal donor route copy into the local product because
  the current `Ae.solator` runtime contract is wrapper-first and the local
  `VirGL/Vortek` components are not the active app-owned route selector today

### GameNative

- confirmed donor direction toward composed graphics routing:
  wrapper lane + downloadable drivers + runtime/package coupling
- strongest transferable idea was not another renderer enum but preserving
  route intent and provider provenance through the launch contract

### Succubussix / community bionic-glibc forks

- confirmed that graphics defaults and persisted driver selection are still
  live bug surfaces
- code donor itself is weaker than local `Ae.solator` because it still splits
  wrapper driver version from the main graphics contract instead of keeping one
  provider-driven route owner

Why:
- prevents false policy like `gladio == vortek == virgl == wrapper`
- keeps donor compatibility where the local product has a real owner
- exposes degraded donor routes honestly for forensics and later route-specific work

## Sources

- https://github.com/brunodev85/winlator/releases/tag/v9.0
- https://github.com/brunodev85/winlator/issues/628
- https://github.com/utkarshdalal/GameNative/releases/tag/v0.7.1
- https://github.com/K11MCH1/AdrenoToolsDrivers/releases/tag/v25.2.0_r1
- https://github.com/Succubussix/winlator-bionic-glibc/releases
- https://github.com/Winlator-Random/Mobox-Ludashi-Glibc/releases/tag/v2.0
