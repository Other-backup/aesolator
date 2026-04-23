# Community Donor Frontier 2026-04-13

Updated: `2026-04-13`

## Scope

Reflective donor map for the current `Ae.solator -> FreeWine11 -> WCP` line,
focused on:

- fresh `aesolator`-relevant upstream motion during the last month,
- active `glibc` community donor lanes,
- what belongs in app logic,
- what belongs in rootfs/runtime packaging,
- and which Codex workflows from the official `use-cases` surface should be
  treated as hard engineering habits for this repo.

## Primary Source Wall

- AndreRH Wine `arm64ec` branch:
  `https://github.com/AndreRH/wine`
- Valve Proton `bleeding-edge`:
  `https://github.com/ValveSoftware/Proton`
- GameNative:
  `https://github.com/utkarshdalal/GameNative`
- The412Banner Nightlies:
  `https://github.com/The412Banner/Nightlies`
- Arihany WinlatorWCPHub:
  `https://github.com/Arihany/WinlatorWCPHub`
- Waim908 wine-winlator:
  `https://github.com/Waim908/wine-winlator`
- Waim908 rootfs-winlator:
  `https://github.com/Waim908/rootfs-winlator`
- moze30 winlator-glibc:
  `https://github.com/moze30/winlator-glibc`
- moze30 winlator-wcp:
  `https://github.com/moze30/winlator-wcp`
- Alexoqool winlator-bionic-build:
  `https://github.com/Alexoqool/winlator-bionic-build`
- Xnick417x Winlator-Bionic-Nightly-wcp:
  `https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp`
- atgehrhardt wine-wcp-builder:
  `https://github.com/atgehrhardt/wine-wcp-builder`
- leegao bionic-vulkan-wrapper:
  `https://github.com/leegao/bionic-vulkan-wrapper`
- Ludashi archive lane:
  `https://github.com/safaaking554-maker/Winlator-ciore-cmod-ludashi-`
- xodiosx wine-stuff:
  `https://github.com/xodiosx/wine-stuff`
- lastmile-ai mcp-agent:
  `https://github.com/lastmile-ai/mcp-agent`
- Official Codex use cases:
  `https://developers.openai.com/codex/use-cases`

## Donor Taxonomy

### Tier 1: source-logic donors

1. `AndreRH/wine`
   current `arm64ec` base donor for `FreeWine11` ARM64EC source logic.
2. `ValveSoftware/Proton`
   current high-signal donor for wrapper/proton-side integration,
   launch/runtime policy, compatibility work, and upstream pace.

### Tier 2: app/runtime behavior donors

1. `utkarshdalal/GameNative`
   strongest app-side donor for Android runtime/product behavior.
2. `brunodev85/winlator`
   historical upstream reference, but not the freshest active wall today.

### Tier 3: binary/feed donors

1. `The412Banner/Nightlies`
   fast binary/nightly hub for wrappers/translators.
2. `Arihany/WinlatorWCPHub`
   package-index/feed donor.
3. `Waim908/wine-winlator`
   active community `glibc` Wine/WCP release donor.

### Tier 4: rootfs donors

1. `Waim908/rootfs-winlator`
   explicit rootfs/imagefs donor.
2. `moze30/winlator-glibc`
   active glibc app/runtime line.
3. `moze30/winlator-wcp`
   glibc packaging/patch lane.
4. `Alexoqool/winlator-bionic-build`
   bionic APK/runtime aggregation lane for Pipetto-style Winlator Bionic.
5. `Xnick417x/Winlator-Bionic-Nightly-wcp`
   bionic nightly package lane.

### Tier 5: build-harness / experimental donors

1. `xodiosx/wine-stuff`
   workflow/build-harness donor around `arm64ec`, scripts, and GStreamer fixes.
2. `lastmile-ai/mcp-agent`
   tooling/orchestration donor for MCP-native agent workflows, durable task
   composition, and simple workflow patterns.
3. `atgehrhardt/wine-wcp-builder`
   automated builder lane spanning GameNative / Winlator Bionic packaging.
4. `leegao/bionic-vulkan-wrapper`
   bionic graphics/provider donor, relevant to wrapper/runtime seams rather
   than app behavior.
5. `safaaking554-maker/Winlator-ciore-cmod-ludashi-`
   archive/fork signal only; useful for archaeology, not a primary donor.

## Fresh Findings

### AndreRH / FreeWine source donor

- `AndreRH/wine` default branch is still `arm64ec`.
- Repo `pushed_at` was `2026-03-15`, but the visible top branch commits are not
  a fresh April source wave.
- Conclusion:
  still the right baseline source donor for `FreeWine11`, but not the main
  active signal for the last month. The current fast frontier has shifted to
  Proton/app/runtime/feed/rootfs donors.

### Proton donor wall

- `ValveSoftware/Proton` default branch is `proton_10.0`.
- `bleeding-edge` was active on `2026-04-12`.
- Recent visible commits include:
  `lsteamclient` SDK 1.64 work,
  `vrclient` Vulkan/OpenVR device-extension handling,
  `fixup! proton: Prefer native ddraw for a few games`,
  and fresh experimental build tags on `2026-04-12`.
- Conclusion:
  Proton is currently a live donor for compatibility glue, wrapper/runtime
  integration, and regression triage, even when `FreeWine11` remains the
  source-of-truth for the shipped Wine core.

### GameNative donor wall

- `GameNative` was active on `2026-04-12`.
- Fresh app/runtime commits include:
  `wine-mono` bump to `11.0.0`,
  an extra XNA MSI directory,
  `fexcore 2604`,
  `Added wine env var fix type + fix for stardew`,
  storage/download handling fixes,
  and container cleanup behavior.
- Conclusion:
  `GameNative` is currently the strongest fresh donor for Android app/runtime
  product behavior around content bootstrapping, prefix dependencies, and
  translator/runtime management.

### Bionic donor wall

- `Alexoqool/winlator-bionic-build` is an active aggregation lane for
  Pipetto-style Winlator Bionic APK drops.
- `Xnick417x/Winlator-Bionic-Nightly-wcp` is a nightly WCP lane for bionic
  payloads.
- `atgehrhardt/wine-wcp-builder` explicitly targets GameNative / Winlator
  Bionic builder automation.
- `leegao/bionic-vulkan-wrapper` is a bionic graphics/provider donor, not an
  app-source donor.
- Conclusion:
  bionic donors matter for runtime/provider/build lanes and must be compared
  separately from `glibc` donors. They are not interchangeable with the
  `glibc` donor wall.

### Ludashi donor wall

- The visible GitHub signal is currently an archive/fork lane:
  `safaaking554-maker/Winlator-ciore-cmod-ludashi-`.
- Conclusion:
  `Ludashi` is presently a weak archaeology donor, useful for recovery or
  comparison, but not a primary active source donor like `GameNative` or
  `Proton`.

### Nightlies / feed donor wall

- `The412Banner/Nightlies` is highly active with hourly/daily automation.
- Fresh release `nightly-20260412-193836` carries:
  `FEX-2604`,
  `DXVK arm64ec`,
  `VKD3D-Proton 3.0b`,
  `Box64 Hybrid`,
  and fresh Turnip artifacts.
- Conclusion:
  Nightlies stays the fastest wrapper/translator/component freshness source, but
  it is a binary/feed donor, not a source-logic owner.

### WCPHub donor wall

- `Arihany/WinlatorWCPHub` remains an actively refreshed package index.
- Root carries `pack.json`, plus release lanes such as `WINE` and
  `WOWBOX64-NIGHTLY`.
- Conclusion:
  this is a packaging/feed donor and a useful fallback for missing runtime
  packages, not a root source donor.

### Waim glibc donor wall

- `Waim908/wine-winlator` was active on `2026-04-12`.
- Fresh commits include:
  font-display fixes,
  `wcp-package.sh` changes to avoid deleting WM registry state,
  package-size reduction,
  and packaging-wait logic.
- Fresh releases include:
  `10.15-r3-tkg`,
  `10.14-r0-arm64ec`,
  and recent `10.15-r1/r2`.
- Conclusion:
  Waim is the current high-signal community `glibc` donor for packaged Wine
  runtimes and packaging discipline.

### Waim rootfs donor wall

- `Waim908/rootfs-winlator` was active on `2026-04-10`.
- Fresh releases include:
  `rootfs-g7.1.6-1.1.tzst`
  and `imagefs.txz`.
- README emphasizes:
  locale/encoding completeness,
  timezone data,
  MangoHud variables,
  and rootfs replacement/overlay workflows.
- Conclusion:
  this donor matters at the rootfs/imagefs layer, not as a drop-in app or
  runtime-source replacement.

### Moze glibc donor wall

- `moze30/winlator-glibc` released `v7.1.6-b1` on `2026-03-22`.
- `moze30/winlator-wcp` shows fresh April work around `Wine 10.15 R2`,
  glibc patches, and imported glibc headers/build files.
- Conclusion:
  moze is a real glibc donor family, but it is more useful today as an app /
  packaging / glibc patch reference than as a first source donor for the
  `FreeWine11` bionic line.

### xodiosx harness donor wall

- `xodiosx/wine-stuff` was active on `2026-04-12`.
- Fresh work touches:
  default `arm64ec` branch selection,
  workflow generation,
  and GStreamer compatibility/build handling.
- Conclusion:
  useful as a build-harness donor, not as a product-source donor.

### mcp-agent tooling donor wall

- `lastmile-ai/mcp-agent` presents itself as a composable MCP-native agent
  framework with:
  full MCP support,
  simple workflow patterns,
  and optional durable execution via Temporal.
- Repository surface includes:
  `docs`, `examples`, `scripts`, `tests`, and `src/mcp_agent`.
- Conclusion:
  it is relevant to our automation/tooling lane, especially for
  donor-refresh orchestration, research pipelines, and verification loops.
  It is not a runtime/product donor for `Ae.solator` itself.

## Applied Product Conclusions

1. `Ae.solator` must treat new `glibc` donors as first-class metadata, not as
   invalid rootfs noise.
2. Runtime bootstrap must merge multiple donor feeds for `glibc` / Proton
   requests instead of relying on one hardcoded feed.
3. Feed selection and merge policy must be centralized, or every new donor
   reopens the same parsing and priority drift.
4. `FreeWine11` remains the product-owned bionic source line; donor value is
   transferred by subsystem, not by blind replacement.
5. The current `GameNative` donor transfer with the cleanest app-side ROI is
   the prelaunch `gamefixes` layer:
   per-game launch args, registry seeding, and env-var fixes as one explicit
   prelaunch owner class.

## Codex Skill Adoption From Official Use Cases

The official Codex use-cases page currently surfaces several workflows that are
directly relevant to this repo.

### Adopt immediately

1. `Understand large codebases`
   keep whole-repo maps, donor matrices, and file-owner classification current
   before editing runtime/product seams.
2. `Create a CLI Codex can use`
   every repeated donor-refresh, forensic-capture, or build-classification lane
   should become a deterministic script before it becomes folklore.
3. `Save workflows as skills`
   once a donor-closure or device-forensics workflow stabilizes, distill it
   into a reusable skill / rule / script trio instead of repeating the same
   manual reasoning.
4. `Iterate on difficult problems`
   hard runtime tails should run as scored closure loops with evidence,
   candidate ranking, and explicit stop conditions.
5. `Learn a new concept`
   dense donor docs / changelogs should become engineering reports tied to the
   current product boundary, not loose notes.
6. `Review pull requests faster`
   donor transfer should be reviewed as a risk-ranked diff against
   runtime/app/product contracts, not only by “does it compile”.
7. `Automate bug triage`
   fresh runtime bundles, device logs, and donor deltas should feed one
   actionable triage surface instead of independent note piles.
8. `MCP-native orchestration`
   use MCP/tool-native composition and durable workflow patterns for
   donor-refresh / forensic / verification automation instead of ad-hoc glue.

### Repo-specific translation

- `Understand large codebases` -> AGENTS + roadmap + donor frontier docs +
  emitted/runtime scanners.
- `Create a CLI Codex can use` -> donor refresh / feed merge / forensic bundle /
  build parser scripts.
- `Save workflows as skills` -> donor frontier closure, runtime stack closure,
  Android forensic capture.
- `Iterate on difficult problems` -> whole-tree make/runtime/device closure
  loops with scoring and evidence.
- `MCP-native orchestration` -> staged donor refresh, multi-source research,
  and verification pipelines built as composable tool workflows rather than one
  monolithic script.

## Roadmap

### Phase 0: donor truth normalization

- keep one live donor matrix with:
  repo,
  role,
  freshness,
  branch/tag,
  import scope,
  and reject reasons.
- separate `source donor`, `binary donor`, `feed donor`, and `rootfs donor`
  ownership explicitly.
- keep `tooling donor` separate too, so `mcp-agent`-style orchestration
  patterns do not get mixed into runtime/product ownership claims.

### Phase 1: app/feed closure

- centralize runtime donor feeds and their parse rules.
- merge multiple donor feeds for `glibc`/Proton hydration with one stable
  priority policy.
- stop hardcoding single-feed assumptions into launch/bootstrap paths.
- prepare a deterministic donor-refresh CLI lane so new feed donors can be
  added without re-editing app logic every time.

### Phase 2: rootfs donor closure

- preserve donor rootfs provider/layout metadata.
- stop forcing reinstall just because the active rootfs is not
  `gamenative/ubuntufs`.
- classify rootfs donors as product-visible metadata so device/runtime
  forensics stop lying about the active root.

### Phase 3: runtime/package closure

- compare `FreeWine11` against Proton / AndreRH / active glibc package donors
  by subsystem:
  launcher,
  translators,
  prefix/bootstrap,
  wrappers,
  packaging.
- transfer only source-backed wins that fit the bionic-owned product line.

### Phase 4: translator / graphics closure

- re-audit `FEX`, `wowbox64`, `DXVK`, `VKD3D`, `Turnip`, and `OpenGL driver`
  lanes against:
  Proton,
  Nightlies,
  GameNative,
  and community donor packages.
- preserve app/runtime source-of-truth while widening detection and fallback
  coverage.

### Phase 5: prefix / installer closure

- reflect fresh donor motion around `wine-mono`, `XNA`, and related installer
  surfaces into `Prefix Pack` and runtime/bootstrap logic.
- keep GUI-install proof/state/log ownership explicit.

### Phase 6: device proof closure

- validate donor-aware rootfs/runtime selection on live containers.
- compare our adapted rootfs and runtime against external donor containers with
  direct forensic capture.
- keep closure claims blocked on device/runtime evidence, not only local build
  success.

### Phase 7: automation closure

- turn donor frontier refresh, remote feed merge, device forensic capture, and
  verification passes into composable MCP/tool workflows.
- prefer simple orchestrated stages with explicit artifacts and scoring over one
  opaque automation blob.

## What Landed In This Batch

1. runtime donor feed selection is now centralized in code through
   `RuntimeFeedRegistry`.
2. remote feed merge policy is centralized through
   `RemoteProfileFeedMerger`.
3. launch-time runtime hydration now pulls a wider donor pool for
   `glibc` / Proton requests instead of trusting one feed.
4. rootfs provider/layout metadata is no longer hard-pinned to
   `gamenative/ubuntufs` in the install/launch path.

## Near-Term Next Moves

1. add a donor-frontier refresh script that materializes this matrix from GitHub
   APIs into machine-readable JSON.
2. extend community donor ingestion to more verified feeds once they expose a
   stable metadata endpoint or a parse-safe release surface.
3. run focused device/runtime comparisons on donor-rootfs containers and feed
   the findings back into rootfs/runtime policy.
