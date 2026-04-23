# Chapter 2 Donor Frontier Batch Roadmap 2026-04-13

Updated: `2026-04-13`

## Scope

This roadmap is the current whole-product donor map for the active
`Ae.solator -> FreeWine11 -> WCP` line.

Expanded donor-universe companion:

- `docs/GITHUB_DONOR_UNIVERSE_2026-04-13.md`
- `docs/GITHUB_DONOR_UNIVERSE_FREEZE_2026-04-13.json`

Goal:

- stop treating all donors as one blob,
- separate live donors from archaeology and packaging noise,
- map each donor to a real owner frontier,
- and define one single closure batch order instead of random tail-chasing.

Method:

- local codebase reflection against current owner seams in `aesolator`,
- current donor metadata from live GitHub repo heads/releases on `2026-04-13`,
- official Codex use-cases for workflow/tooling habits,
- and Chapter 2 product rules already codified in this workspace.

## Hard Reflection

The frontier is not "copy everything from everybody".

The real split is:

1. `FreeWine11` source logic owners:
   `AndreRH/wine`, `ValveSoftware/Proton`, selected `xodiosx/wine-stuff`,
   selected `atgehrhardt/wine-wcp-builder`.
2. `Ae.solator` app/runtime behavior owners:
   `GameNative`, selected `Alexoqool`, selected `Ludashi` archaeology,
   selected community UX/runtime donors.
3. Feed/package freshness owners:
   `The412Banner/Nightlies`, `Arihany/WinlatorWCPHub`,
   `Waim908/wine-winlator`, `Xnick417x/Winlator-Bionic-Nightly-wcp`,
   `moze30/winlator-wcp`.
4. Rootfs/imagefs owners:
   `Waim908/rootfs-winlator`, `moze30/winlator-glibc`,
   `Alexoqool/winlator-bionic-build`.
5. Provider/build/tooling owners:
   `leegao/bionic-vulkan-wrapper`, `lastmile-ai/mcp-agent`,
   official Codex use-cases.

The strict mistake to avoid:

- importing app logic into source glue,
- importing feed/catalog logic into runtime source,
- importing glibc donor assumptions into the `bionic` Chapter 2 line,
- or importing archaeology donors as if they were live owners.

## Donor Ledger

| Donor | Current truth on 2026-04-13 | Class | Product value | Transfer policy |
| --- | --- | --- | --- | --- |
| `AndreRH/wine` | default branch `arm64ec`, head `04d384823c75`, visible March 8 source wave, repo updated on Apr 12 | source donor | still the direct ARM64EC source baseline for `FreeWine11` | keep as baseline source comparison donor, but not the fastest freshness wall |
| `ValveSoftware/Proton` | `bleeding-edge` active on Apr 12; default line `proton_10.0`; latest release `proton-10.0-4` on Jan 26 | source/runtime donor | highest-signal current compatibility/runtime source donor | compare against Proton first for fresh Android-adjacent Wine/runtime classes, then port cleanly into `FreeWine11` |
| `utkarshdalal/GameNative` | master active on Apr 13; latest release `v0.9.0` on Apr 8 | app/runtime donor | strongest live app/runtime behavior donor | port only through honest `aesolator` owner seams |
| `The412Banner/Nightlies` | latest nightly `nightly-20260413-190849` on Apr 13 | feed/component donor | fastest component freshness wall for Box64/FEX/DXVK/VKD3D/Turnip | use as freshness oracle, not source owner |
| `Arihany/WinlatorWCPHub` | refreshed multiple times on Apr 13 | feed/index donor | package index and fallback catalog donor | merge as catalog/feed signal only |
| `Waim908/wine-winlator` | active on Apr 13; release `10.14-r1-arm64ec` on Apr 13 | glibc package donor | strong packaging discipline and `arm64ec` runtime donor | compare packaging/layout, not app logic |
| `Waim908/rootfs-winlator` | active on Apr 13; release `rootfs-g7.1.6-1.1` on Apr 10 | rootfs donor | strongest live rootfs/imagefs donor in current community wall | compare rootfs/imagefs payload/layout/metadata |
| `moze30/winlator-glibc` | active on Apr 13 | glibc app donor | glibc app integration signal | use for glibc comparative behavior only |
| `moze30/winlator-wcp` | active on Apr 13; `Wine 10.15 R3` update on Apr 13 | glibc WCP donor | package/update lane donor | compare WCP packaging/update discipline |
| `Alexoqool/winlator-bionic-build` | release `2026-04-12` on Apr 12 | bionic runtime donor | bionic APK/runtime aggregation donor | use for bionic package/runtime/UI env-var seams |
| `Xnick417x/Winlator-Bionic-Nightly-wcp` | active on Apr 13; hub auto-updates same day | bionic WCP/feed donor | fast bionic WCP freshness donor | use for bionic feed/package verification |
| `atgehrhardt/wine-wcp-builder` | last source wave on Mar 18 | builder donor | automated Wine/WCP builder donor, switched to `GameNative/wine` | use for builder patterns and packaging automation, not as live source leader |
| `xodiosx/wine-stuff` | active on Apr 13; release `wine-termux-4-20260413-140706` | build/runtime donor | strong Termux/ARM64CE workflow donor | compare build harness and Termux runtime surface |
| `leegao/bionic-vulkan-wrapper` | wrapper branch still old 2025 source surface | provider donor | targeted Vulkan-on-Vulkan/provider donor | narrow provider comparison only, not broad runtime donor |
| `safaaking554-maker/Winlator-ciore-cmod-ludashi-` | archived snapshot, Feb 20 release | archaeology donor | only useful for feature archaeology and diffing | never treat as primary live owner |
| `lastmile-ai/mcp-agent` | main head `f62d84935081`, tags up to `v0.2.6` | tooling donor | durable MCP-native orchestration patterns | use for donor refresh / forensic automation / durable task tooling |
| `OpenAI Codex use cases` | current official page on `developers.openai.com` | workflow donor | official agent workflow habits | convert to repo habits, not product features |

## High-Signal Findings By Frontier

### 1. Source Frontier: `FreeWine11` core and build glue

Primary live donors:

- `ValveSoftware/Proton`
- `AndreRH/wine`
- selective `xodiosx/wine-stuff`

Why:

- `AndreRH` remains the direct ARM64EC source baseline.
- `Proton bleeding-edge` is the freshest source wall:
  on `2026-04-12` it carried `lsteamclient` SDK 1.64 work,
  `vrclient` Vulkan/OpenVR device-extension work,
  and `ddraw` preference fixes.
- `xodiosx/wine-stuff` is currently a live Termux/ARM64CE builder lane, useful
  for build-harness and Termux runtime glue.

What this means:

- the source-side review order is no longer `AndreRH only`;
  it must be `Proton bleeding-edge -> AndreRH arm64ec -> local FreeWine11`.
- use `AndreRH` to protect ARM64EC/ARM64X intent,
  use `Proton` to catch fresh runtime/source classes we have not transferred yet.

Transfer targets:

- `dlls/`
- `libs/`
- `programs/`
- `server/`
- build glue and packaging-adjacent build scripts

### 2. App Runtime Frontier: `aesolator`

Primary live donor:

- `GameNative`

Evidence:

- `GameNative` master is still moving on `2026-04-13`.
- `v0.9.0` release on `2026-04-08` explicitly includes:
  store-specific best configs,
  download/storage manager work,
  launch dependency fixes,
  Steam branch/version logic,
  intent-launch fixes,
  working-dir and cloud-save fixes,
  and a Ludashi-derived effects lane.

What this means:

- the app-side frontier is no longer mostly cosmetic.
- the strongest still-open owner classes in `aesolator` are:
  - launch path normalization,
  - working-directory and install-path truth,
  - store-aware config import,
  - download/storage recovery,
  - prefix dependency discovery,
  - frontend/offline/intent launch edge cases,
  - cloud-save and staged-path persistence.

### 3. Feed / Package Frontier

Primary live donors:

- `Nightlies`
- `WCPHub`
- `Waim908/wine-winlator`
- `Xnick417x/Winlator-Bionic-Nightly-wcp`
- `moze30/winlator-wcp`

Evidence:

- `Nightlies` latest build on `2026-04-13` exposes current component truth for
  Box64, FEX, DXVK, VKD3D-Proton, Turnip.
- `WCPHub` refreshes repeatedly on `2026-04-13`.
- `Waim wine-winlator` published `10.14-r1-arm64ec` on `2026-04-13`.
- `moze30/winlator-wcp` updated to `Wine 10.15 R3` on `2026-04-13`.
- `Xnick` updates bionic WCP hub content on `2026-04-13`.

What this means:

- feed logic must stop assuming one donor feed and one package layout.
- runtime hydration must score feeds by:
  freshness,
  runtime model,
  Wine family,
  component class,
  and package completeness.

### 4. Rootfs / Imagefs Frontier

Primary live donors:

- `Waim908/rootfs-winlator`
- `Alexoqool/winlator-bionic-build`
- `moze30/winlator-glibc`

What this means:

- rootfs/imagefs cannot stay pinned to one synthetic provider identity.
- the app must preserve:
  provider,
  layout,
  package origin,
  and payload completeness
  instead of collapsing them into one old `gamenative/ubuntufs` assumption.

### 5. Provider / Wrapper Frontier

Primary donors:

- `Nightlies`
- `leegao/bionic-vulkan-wrapper`
- current component lanes inside `WCP`

What this means:

- `leegao` still matters only as a narrow provider donor.
- do not inflate it into a full runtime donor.
- provider selection must stay evidence-driven:
  Vulkan, OpenGL companion package, wrapper manifest, ICD/layer visibility.

### 6. Tooling Frontier

Primary donors:

- `lastmile-ai/mcp-agent`
- official Codex use-cases

What this means:

- donor refresh and forensic closure should become durable tools, not manual
  ritual.
- the most relevant official Codex habits for this repo are:
  - review pull requests faster,
  - create a CLI Codex can use,
  - save workflows as skills,
  - iterate on difficult problems,
  - understand large codebases.

These map directly onto:

- donor frontier refresh CLI,
- package/runtime comparison CLI,
- build-log and forensic bundle reducers,
- repeatable skills for Chapter 2 closure classes.

## Donors To Reject As Primary Owners

These are still valid references, but not live primary owners:

1. `Ludashi archive`
   archaeology only.
2. `leegao/bionic-vulkan-wrapper`
   provider-only donor.
3. `atgehrhardt/wine-wcp-builder`
   builder-pattern donor, not the current source leader.
4. `moze30/winlator-glibc`
   glibc comparative donor, not owner of the `bionic` Chapter 2 line.
5. `Nightlies`
   component freshness oracle, not source logic owner.

## Single-Batch Closure Order

This is the one-batch order that should be followed instead of random drift
fixes.

### Batch 0: Freeze donor truth

- capture current donor SHA/release truth for all live donors,
- pin source/package/feed comparisons to that snapshot,
- stop mixing old folklore with new donor state.

### Batch 1: App path/address truth in `aesolator`

Owner files:

- runtime selection and launch routing
- `WineInfo`
- `ContentsManager`
- `ContainerManager`
- `ImageFs` / `ImageFsInstaller`
- direct launch / prefix-pack / working-dir / install-path surfaces

Why first:

- if runtime path and install-path truth drift here,
  every later donor transfer gets re-bound incorrectly.

### Batch 2: Feed and package normalization

Owner files:

- `RuntimeFeedRegistry`
- `RemoteProfileFeedMerger`
- `RemoteFeedPayloadLoader`
- runtime presence checks
- profile synthesis / layout normalization

Target:

- one scored donor feed contract for:
  `bionic`,
  `glibc`,
  `FreeWine11`,
  `Proton`,
  component WCPs.
- one central `GitHub releases -> normalized payload -> source metadata` lane
  with explicit `403/rate-limit -> atom/expanded-assets fallback` ownership,
  instead of separate feed logic in `ContentsFragment` and
  `XServerDisplayActivity`.
- one central `installed present -> usable -> broken` runtime/package truth in
  `ContentsManager`, with `ContentProfile.locallyInstalled` reduced to
  owner-private state behind accessors, so UI, manifest install, and launch
  dependency lanes stop treating raw install flags as product truth.
- one shared install-state UI contract on top of that truth, so local+remote
  entries stay manageable, broken installs stay visible/repairable, and
  graphics-driver package matching/counting no longer forks per fragment.
- one manifest-backed dependency recovery lane for component/runtime payloads
  that donor manifest already owns, instead of failing launch immediately on
  recoverable `dxvk` / `wowbox64` / `fexcore` / `proton` gaps.

### Batch 3: Rootfs/imagefs metadata and payload truth

Owner files:

- `ImageFs`
- `ImageFsInstaller`
- install-time rootfs provider/layout detection
- package/rootfs compatibility checks

Target:

- preserve provider/layout identity,
- preserve `arm64-v8a` ABI subtree truth,
- preserve `prefixPack` and payload completeness truth,
- stop silent path flattening or provider rewriting.

### Batch 4: `FreeWine11` source donor transfer

Owner repos:

- `FreeWine11`
- `AndreRH/wine`
- `ValveSoftware/Proton`

Target:

- compare Proton bleeding-edge and AndreRH by class,
- port the best logic into `FreeWine11`,
- keep `Aesync`, `bionic` truth and `arm64ec` ownership intact.

Priority source classes:

- Android-facing DLL/runtime classes
- loader/bootstrap safety
- build glue and packaging-adjacent source classes
- arm64ec/arm64x support classes

### Batch 5: Provider and wrapper closure

Owner layers:

- Vulkan/OpenGL provider resolution
- companion package materialization
- wrapper manifest and ICD/layer truth
- bionic wrapper/provider proof

Target:

- no unexplained degraded provider state.

### Batch 6: Validation and comparative proof

Required proof:

- donor package diff
- donor rootfs diff
- source diff by owner class
- `compile + unit test`
- `make/install/pack` proof on `WCP`
- device/runtime proof on live containers

## Concrete Work Queue By Repo

### `aesolator`

1. finish runtime root/path/address unification everywhere,
2. finish store/install/working-dir truth,
3. finish donor-style dependency discovery and storage/download recovery,
4. finish feed scoring and package completeness checks,
5. finish rootfs provider/layout truth preservation,
6. finish comparative runtime diagnostics against donor containers.

### `wcp-runtime-lanes`

1. compare current packaging and stage contracts against `Waim`, `moze`, `Xnick`,
2. keep `arm64-v8a` ABI subtree and compat links strict,
3. keep `prefixPack` and runtime manifest/source metadata honest,
4. keep package-side component provenance and donor freshness visible.

### `FreeWine11`

1. compare current source line against `Proton bleeding-edge`,
2. compare ARM64EC/ARM64X intent against `AndreRH arm64ec`,
3. transfer the missing source classes centrally, not by single error,
4. rerun build/package/device proof only after the source batch is coherent.

## Verification Contract

The batch is not closed until these all survive together:

1. app compile/tests,
2. runtime package presence/layout checks,
3. `WCP` stage/package checks,
4. source donor reflection pass,
5. donor comparison against live donor containers/packages,
6. live device/container runtime proof.

## Sources

- `AndreRH/wine`:
  https://github.com/AndreRH/wine
- `ValveSoftware/Proton`:
  https://github.com/ValveSoftware/Proton
- `ValveSoftware/Proton` bleeding-edge commits:
  https://github.com/ValveSoftware/Proton/commits/bleeding-edge
- `ValveSoftware/Proton` releases:
  https://github.com/ValveSoftware/Proton/releases
- `GameNative`:
  https://github.com/utkarshdalal/GameNative
- `GameNative` `v0.9.0` release:
  https://github.com/utkarshdalal/GameNative/releases/tag/v0.9.0
- `The412Banner/Nightlies`:
  https://github.com/The412Banner/Nightlies
- `The412Banner/Nightlies` latest nightly used here:
  https://github.com/The412Banner/Nightlies/releases/tag/nightly-20260413-190849
- `Arihany/WinlatorWCPHub`:
  https://github.com/Arihany/WinlatorWCPHub
- `Waim908/wine-winlator`:
  https://github.com/Waim908/wine-winlator
- `Waim908/rootfs-winlator`:
  https://github.com/Waim908/rootfs-winlator
- `moze30/winlator-glibc`:
  https://github.com/moze30/winlator-glibc
- `moze30/winlator-wcp`:
  https://github.com/moze30/winlator-wcp
- `Alexoqool/winlator-bionic-build`:
  https://github.com/Alexoqool/winlator-bionic-build
- `Alexoqool/winlator-bionic-build` latest release used here:
  https://github.com/Alexoqool/winlator-bionic-build/releases/tag/2026-04-12
- `Xnick417x/Winlator-Bionic-Nightly-wcp`:
  https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp
- `atgehrhardt/wine-wcp-builder`:
  https://github.com/atgehrhardt/wine-wcp-builder
- `xodiosx/wine-stuff`:
  https://github.com/xodiosx/wine-stuff
- `leegao/bionic-vulkan-wrapper`:
  https://github.com/leegao/bionic-vulkan-wrapper
- `Ludashi` archive donor:
  https://github.com/safaaking554-maker/Winlator-ciore-cmod-ludashi-
- `lastmile-ai/mcp-agent`:
  https://github.com/lastmile-ai/mcp-agent
- `lastmile-ai/mcp-agent` README:
  https://raw.githubusercontent.com/lastmile-ai/mcp-agent/main/README.md
- official Codex use-cases:
  https://developers.openai.com/codex/use-cases
