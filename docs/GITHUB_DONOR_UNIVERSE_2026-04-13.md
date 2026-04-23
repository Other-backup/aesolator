# GitHub Donor Universe 2026-04-13

Updated: `2026-04-13`

## Scope

This document expands the donor field beyond the old short list.

Hard reflection:

- "all of GitHub" is not a real engineering set,
- the real set is the active GitHub field that touches the Chapter 2 product,
- so this map covers the relevant `Winlator -> Wine/Proton -> WCP -> rootfs -> provider -> translator` surface,
- and splits live owners from mods, archives, and device-specific forks.

This is the donor-universe companion to:

- `docs/CHAPTER2_DONOR_FRONTIER_BATCH_ROADMAP_2026-04-13.md`

## Tiering Rules

### Tier A: transfer owners

Use these as active comparison and transfer donors.

### Tier B: comparative donors

Use these for specialized evidence, packaging discipline, device-specific behavior,
or regression checks, but not as broad source-of-truth owners.

### Tier C: archaeology or noise

Keep only for diffing, reverse archaeology, or negative proof.
Do not copy them as if they were current product owners.

## Frontier Matrix

| Frontier | Tier A donors | Tier B donors | Reject as primary owner |
| --- | --- | --- | --- |
| Source / runtime core | `ValveSoftware/Proton`, `AndreRH/wine`, `AndreRH/hangover` | `xodiosx/wine-stuff`, `atgehrhardt/wine-wcp-builder` | packaging hubs, app forks, archive mods |
| Translators / hybrid ABI | `FEX-Emu/FEX`, `ptitSeb/box64`, `AndreRH/hangover` | `wowbox64` package donors, device forks shipping translator payloads | APK/UI forks pretending to own translator semantics |
| App / launch / UX | `utkarshdalal/GameNative`, `brunodev85/winlator` | `winebox64/winlator`, `afeimod/winlator-mod` | archive forks and one-device mods |
| Package / feed / WCP | `The412Banner/Nightlies`, `Arihany/WinlatorWCPHub`, `Waim908/wine-winlator`, `Xnick417x/Winlator-Bionic-Nightly-wcp` | `moze30/winlator-wcp`, `Alexoqool/winlator-bionic-build` | source repos that do not own package freshness |
| Rootfs / imagefs | `Waim908/rootfs-winlator`, `Alexoqool/winlator-bionic-build` | `moze30/winlator-glibc`, `afeimod/winlator-mod` rootfs lanes | glibc-only donor assumptions on the bionic line |
| Graphics / provider / wrappers | `K11MCH1/AdrenoToolsDrivers`, `WearyConcern1165/ExynosTools` | `leegao/bionic-vulkan-wrapper`, community Turnip hubs | generic Winlator forks with stale copied drivers |
| Tooling / builder / automation | `lastmile-ai/mcp-agent`, `atgehrhardt/wine-wcp-builder` | official Codex use-cases, local CI helpers | APK/runtime forks treated as tooling donors |

## Donor Ledger

### Source and runtime-core donors

1. `ValveSoftware/Proton`
   Strongest freshness wall for current Wine-facing compatibility work.
   Transfer order on Chapter 2 is now `Proton -> AndreRH/wine -> local FreeWine11`.

2. `AndreRH/wine`
   Still the direct ARM64EC baseline for `FreeWine11`.
   Keep for ARM64EC / ARM64X / Android-facing Wine intent.

3. `AndreRH/hangover`
   Important hybrid donor for `ARM64EC + emulator DLL + WoW64 syscall breakout`.
   This is not a full app donor.
   It is a hybrid ABI / execution-model donor.

4. `xodiosx/wine-stuff`
   Strong Termux-oriented comparative donor for build/runtime harnesses.
   Keep as a secondary source/build donor, not the main owner.

### Translator and execution donors

1. `FEX-Emu/FEX`
   Primary translator donor for the `fexcore` lane.
   Use for translator semantics, not APK/UI behavior.

2. `ptitSeb/box64`
   Primary donor for `box64` behavior and expectations.
   Keep as translator/runtime donor, not as Wine/bootstrap owner.

3. `AndreRH/hangover`
   Crosses both source and translator classes because it codifies the
   ARM64EC+FEX hybrid execution model directly.

### App and launch donors

1. `utkarshdalal/GameNative`
   Still the strongest live app/runtime behavior donor:
   launch routing, store-aware configs, storage/download recovery,
   intent flow, and runtime dependency behavior.

2. `brunodev85/winlator`
   Upstream app baseline donor.
   Use it as baseline app/reference truth, especially when a community fork
   adds behavior that diverges from the original owner model.

3. `winebox64/winlator`
   Comparative app fork.
   Useful for integration ideas and packaging/app coupling, but not a broad
   owner above `GameNative` or upstream.

4. `afeimod/winlator-mod`
   Comparative mod donor.
   Useful for rootfs/GStreamer/package heuristics and community packaging
   signals.
   Do not treat it as a clean owner for app architecture.

### Package and feed donors

1. `The412Banner/Nightlies`
   Fastest freshness wall for packaged components.

2. `Arihany/WinlatorWCPHub`
   Feed/index donor.
   Important for remote-hydration completeness and fallback cataloging.

3. `Waim908/wine-winlator`
   Strong packaging/layout donor on the runtime side.

4. `Xnick417x/Winlator-Bionic-Nightly-wcp`
   Strong bionic feed/package donor.

5. `moze30/winlator-wcp`
   Comparative WCP/package donor with a glibc bias.

### Rootfs and imagefs donors

1. `Waim908/rootfs-winlator`
   Strongest current rootfs/imagefs donor in the visible community wall.

2. `Alexoqool/winlator-bionic-build`
   Primary donor for bionic-oriented packaged runtime/rootfs aggregation.

3. `moze30/winlator-glibc`
   Comparative donor only.
   Useful for glibc behavior and completeness checks.
   Not the owner of our Chapter 2 `bionic` line.

4. `afeimod/winlator-mod`
   Secondary rootfs donor because its releases explicitly carry GStreamer /
   rootfs customization.

### Graphics, provider, and wrapper donors

1. `K11MCH1/AdrenoToolsDrivers`
   Strong current driver/package donor for Winlator Bionic and adjacent Android
   emulator ecosystems.

2. `WearyConcern1165/ExynosTools`
   Important non-Adreno donor.
   Keeps the provider frontier from collapsing into a Snapdragon-only model.

3. `leegao/bionic-vulkan-wrapper`
   Narrow wrapper/provider donor only.
   Keep it limited to wrapper/provider analysis.

### Device-specific or specialized comparative donors

1. `Honkonx/winlator-honkon`
   VirGL-focused fork.
   Good for non-Adreno / OpenGL / reduced-feature comparative checks.
   Not a general owner.

2. `Fcharan/WinlatorMali`
   Mali-focused fork.
   Good for device-specific constraints and negative proof.
   Not a general owner.

3. `MrPhryaNikFrosty` Winlator forks
   Comparative community signal only.
   Use for archaeology or targeted diffing where a known community behavior
   originated.

## Transfer Rules By Donor Class

### Source donors

Transfer:

- Wine runtime logic,
- Android-facing Wine patches,
- ARM64EC / ARM64X logic,
- loader/bootstrap safety,
- hybrid ABI logic,
- translator coupling where it is source-owned.

Do not transfer:

- APK UI behavior,
- feed JSON hacks,
- package-hub naming folklore.

### App donors

Transfer:

- launch/dependency behavior,
- config import,
- working-dir and install-path logic,
- intent/download/storage recovery,
- app/runtime diagnostics.

Do not transfer:

- stale source patches embedded in app forks,
- random mod toggles without owner semantics.

### Package and rootfs donors

Transfer:

- package completeness checks,
- feed scoring,
- layout normalization,
- rootfs/provider metadata truth,
- `arm64-v8a` subtree/package compatibility rules.

Do not transfer:

- glibc assumptions into the bionic product line,
- stale mirrored directory folklore as if it were runtime truth.

### Provider donors

Transfer:

- driver package completeness logic,
- companion package rules,
- wrapper/provider detection,
- GPU-family-specific diagnostic branches.

Do not transfer:

- broad runtime policy from one GPU family,
- stale copied driver bundles.

## Highest-Signal New Donors Beyond The Old Short List

These were missing or underweighted in the old donor map:

1. `brunodev85/winlator`
   Needed as upstream app baseline.

2. `AndreRH/hangover`
   Needed as hybrid execution donor, not just `AndreRH/wine`.

3. `FEX-Emu/FEX`
   Needed as the actual translator donor for `fexcore`.

4. `ptitSeb/box64`
   Needed as the actual translator donor for `box64`.

5. `K11MCH1/AdrenoToolsDrivers`
   Needed as live driver/package donor for Winlator Bionic.

6. `WearyConcern1165/ExynosTools`
   Needed to stop the provider frontier from becoming Snapdragon-only.

7. `Honkonx/winlator-honkon`
   Needed as VirGL / reduced-graphics comparative donor.

8. `Fcharan/WinlatorMali`
   Needed as Mali-specific comparative donor.

## Donors To Downrank

These are still useful, but should not sit above the stronger owners:

1. `Ludashi` archive forks
   archaeology only.

2. `leegao/bionic-vulkan-wrapper`
   wrapper-only donor.

3. `moze30/winlator-glibc`
   glibc comparative donor only.

4. device-specific Winlator APK mods
   comparative only unless the frontier is exactly their hardware/runtime lane.

## Batch Consequence For Chapter 2

The expanded donor universe changes the order of work:

1. `aesolator` batch must now compare against `GameNative + upstream Winlator`,
   not `GameNative` alone.
2. `FreeWine11` source batch must compare against `Proton + AndreRH/wine +
   AndreRH/hangover`, not `AndreRH/wine` alone.
3. translator and provider batches must compare against real translator/driver
   owners (`FEX`, `box64`, `AdrenoToolsDrivers`, `ExynosTools`) instead of
   only package hubs.
4. package/rootfs batches must compare against `Nightlies/WCPHub/Waim/Xnick`
   as freshness owners and against `Alexoqool/Waim` as runtime-layout owners.

## Sources

- `ValveSoftware/Proton`:
  https://github.com/ValveSoftware/Proton
- `AndreRH/wine`:
  https://github.com/AndreRH/wine
- `AndreRH/hangover`:
  https://github.com/AndreRH/hangover
- `utkarshdalal/GameNative`:
  https://github.com/utkarshdalal/GameNative
- `brunodev85/winlator`:
  https://github.com/brunodev85/winlator
- `winebox64/winlator`:
  https://github.com/winebox64/winlator
- `afeimod/winlator-mod`:
  https://github.com/afeimod/winlator-mod
- `The412Banner/Nightlies`:
  https://github.com/The412Banner/Nightlies
- `Arihany/WinlatorWCPHub`:
  https://github.com/Arihany/WinlatorWCPHub
- `Waim908/wine-winlator`:
  https://github.com/Waim908/wine-winlator
- `Waim908/rootfs-winlator`:
  https://github.com/Waim908/rootfs-winlator
- `Alexoqool/winlator-bionic-build`:
  https://github.com/Alexoqool/winlator-bionic-build
- `Xnick417x/Winlator-Bionic-Nightly-wcp`:
  https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp
- `moze30/winlator-glibc`:
  https://github.com/moze30/winlator-glibc
- `moze30/winlator-wcp`:
  https://github.com/moze30/winlator-wcp
- `xodiosx/wine-stuff`:
  https://github.com/xodiosx/wine-stuff
- `atgehrhardt/wine-wcp-builder`:
  https://github.com/atgehrhardt/wine-wcp-builder
- `FEX-Emu/FEX`:
  https://github.com/FEX-Emu/FEX
- `ptitSeb/box64`:
  https://github.com/ptitSeb/box64
- `K11MCH1/AdrenoToolsDrivers`:
  https://github.com/K11MCH1/AdrenoToolsDrivers
- `WearyConcern1165/ExynosTools`:
  https://github.com/WearyConcern1165/ExynosTools
- `Honkonx/winlator-honkon`:
  https://github.com/Honkonx/winlator-honkon
- `Fcharan/WinlatorMali`:
  https://github.com/Fcharan/WinlatorMali
- `lastmile-ai/mcp-agent`:
  https://github.com/lastmile-ai/mcp-agent
- official Codex use-cases:
  https://developers.openai.com/codex/use-cases
