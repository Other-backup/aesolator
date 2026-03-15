# GameNative Full Transfer Matrix

Updated: `2026-03-15`

## Goal

Prepare a full-strength donor transfer from `GameNative` into `aesolator`
across all runtime-critical layers:

- payload intake
- package placement
- runtime selection
- Wine / Proton handling
- container creation and migration
- launcher / execution path
- X11 / renderer / driver stack
- pre-install and post-install steps
- environment shaping

This is a transfer matrix, not a promise to bulk-paste the donor unchanged.
`aesolator` keeps its own provenance, forensic, and `Contents` contracts.

## Transfer Rule

For every donor subsystem:

1. inventory exact files
2. decide `import now` / `adapt next` / `hold`
3. transfer foundation first
4. document contract impact before widening the patch

## Already Imported

These donor-derived foundation pieces are already in the local tree:

- `core/GPUHelper.java`
- `cpp/winlator/gpu_helper.c`
- `core/GPUInformation.java` hardware-classification helpers
- `core/DXVKHelper.java`
- `core/GeneralComponents.java`
- `core/PatchElf.java`
- `contents/ManifestEntry.java`
- `contents/ManifestData.java`
- `contents/ManifestContentTypes.java`
- `contents/ManifestRepository.java`
- `contents/ManifestComponentHelper.java`
- `contents/ManifestInstaller.java`
- `renderer/GPUImage.java` hardware-buffer accessor
- `xconnector/XConnectorEpoll.java` rlimit bootstrap hook
- `cpp/winlator/xconnector_epoll.c` tracked fd close / ancillary-fd handling

See also:

- [GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md](/data/data/com.termux/files/home/aesolator/docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md)

## Transfer Lanes

### Lane 1: Runtime Foundation

Primary donor files:

- `com/winlator/xenvironment/ImageFs.java`
- `com/winlator/xenvironment/ImageFsInstaller.java`
- `com/winlator/container/ContainerManager.java`
- `com/winlator/container/Container.java`
- `com/winlator/core/WineInfo.java`
- `com/winlator/core/WineUtils.java`
- `com/winlator/core/WineRegistryEditor.java`
- `com/winlator/core/WineThemeManager.java`

Status:

- partially shared already
- requires focused diff pass, not blind overwrite

Next transfer intent:

- strengthen runtime-path resolution
- compare donor container migration and activation logic
- compare donor prefix/regedit/start-menu helpers against current container flow

### Lane 8: RootFS / ImageFS Hybridization

Primary donor files:

- `com/winlator/xenvironment/ImageFs.java`
- `com/winlator/xenvironment/ImageFsInstaller.java`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `ubuntufs/build.gradle.kts`
- `ubuntufs/src/main/AndroidManifest.xml`

Status:

- donor path identified
- hybridization plan documented
- archive diff/extraction pass still open

Why it matters:

- donor rootfs versioning is newer (`26` vs local `21`)
- donor uses variant-specific base archives and post-extract overlays
- donor preserves imported `Wine` / `Proton` payloads more explicitly
- rootfs quality affects every later lane:
  launcher, Wine/Proton, drivers, multimedia, wrapper hooks, and container boot

Canonical doc:

- [IMAGEFS_HYBRID_PLAN.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_HYBRID_PLAN.md)

### Lane 2: Guest Program Launchers

Primary donor files:

- `com/winlator/xenvironment/components/GuestProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/BionicProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/GlibcProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/WineRequestComponent.java`
- `com/winlator/xenvironment/components/SteamClientComponent.java`
- `com/winlator/xenvironment/components/NetworkInfoUpdateComponent.java`

Status:

- not yet transferred
- high-value lane for runtime execution and Wine/Proton launch behavior

Why it matters:

- binds installed runtime to actual launch path
- controls env shaping, preload, launcher sockets, wineserver lifecycle
- contains donor execution logic for Box64/FEX/WoW64 and runtime extras

Planned order:

1. compare `GuestProgramLauncherComponent`
2. compare Bionic / glibc launchers
3. extract reusable runtime/env logic
4. only then evaluate donor-specific Steam extras

### Lane 3: Payload / Manifest / Install Logic

Primary donor files:

- `app/gamenative/utils/ManifestInstaller.kt`
- `app/gamenative/utils/ManifestComponentHelper.kt`
- `app/gamenative/utils/ManifestRepository.kt`
- `app/gamenative/utils/ManifestModels.kt`
- `app/gamenative/utils/PreInstallSteps.kt`
- `app/gamenative/utils/LaunchDependencies.kt`
- `app/gamenative/utils/launchdependencies/*`
- `app/gamenative/utils/preInstallSteps/*`
- `app/gamenative/service/epic/manifest/*`
- `app/gamenative/service/gog/*Manifest*`

Status:

- partially transferred
- highest-value donor lane outside X11/renderer

Why it matters:

- richer payload install routing
- pre-install dependency chain
- stronger manifest-driven file placement
- better post-download to post-install transition model

Planned order:

1. manifest models / repository mapping
2. manifest installer and component helper
3. launch dependencies and pre-install steps
4. platform-specific manifest helpers only where reusable

Current imported status:

- `ManifestModels` adapted into local Java classes
- `ManifestRepository` adapted with local cache and donor-source fallback
- `ManifestInstaller` adapted on top of existing trusted `ContentsManager` and
  `AdrenotoolsManager` install bridges
- `ManifestComponentHelper` adapted for installed-package availability,
  manifest-version option building, and DXVK context shaping
- UI wiring is still open; manifest layer is now foundation, not yet surfaced

### Lane 4: Container Routing / Launch Requests

Primary donor files:

- `app/gamenative/utils/ContainerUtils.kt`
- `app/gamenative/utils/ContainerMigrator.kt`
- `app/gamenative/utils/IntentLaunchManager.kt`
- `app/gamenative/ui/util/ContainerConfigTransfer.kt`
- `app/gamenative/MainActivity.kt`

Status:

- not yet transferred
- important for external launch, config override, app-to-container routing

Why it matters:

- cleaner pending-launch handling
- better container lookup and migration utility
- more disciplined handoff from UI intent to runtime execution

### Lane 5: Graphics / Driver Policy

Primary donor files:

- `com/winlator/core/GPUInformation.java`
- `com/winlator/core/GPUHelper.java`
- `com/winlator/core/DXVKHelper.java`
- `com/winlator/core/GeneralComponents.java`
- `com/winlator/contents/AdrenotoolsManager.java`
- `com/winlator/xenvironment/components/VortekRendererComponent.java`
- `app/gamenative/ui/screen/xserver/XServerScreen.kt`

Status:

- partially imported
- still open for broader routing and component placement transfer

Why it matters:

- driver and wrapper selection
- Vulkan API / extension policy
- DXVK / VKD3D env shaping
- adrenotools payload pathing
- alternate Vulkan renderer lane

### Lane 6: X11 / Renderer / Input

Primary donor files:

- `com/winlator/xserver/*`
- `com/winlator/widget/TouchpadView.java`
- `com/winlator/widget/XServerView.java`
- `com/winlator/renderer/*`
- `cpp/winlator/*`

Status:

- audited
- partial foundation imports landed
- active closure lane remains desktop cursor/input correctness

Canonical doc:

- [GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md](/data/data/com.termux/files/home/aesolator/docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md)

### Lane 7: Wine / Proton UI and Management Surface

Primary donor files:

- `app/gamenative/ui/screen/settings/WineProtonManagerDialog.kt`
- `app/gamenative/ui/component/dialog/WineTab.kt`
- `app/gamenative/ui/component/dialog/ContainerConfigDialog.kt`

Status:

- not yet transferred
- medium priority after runtime foundation and payload install lanes

Why it matters:

- runtime visibility
- manager UX
- config surfacing for Wine/Proton selection

## Strongest Donor Files By Runtime Relevance

High-signal donor files from the grep pass:

- `app/gamenative/ui/screen/xserver/XServerScreen.kt`
- `app/gamenative/utils/ContainerUtils.kt`
- `app/gamenative/utils/IntentLaunchManager.kt`
- `app/gamenative/utils/ManifestComponentHelper.kt`
- `app/gamenative/utils/ManifestInstaller.kt`
- `app/gamenative/utils/PreInstallSteps.kt`
- `com/winlator/container/ContainerManager.java`
- `com/winlator/core/WineUtils.java`
- `com/winlator/xenvironment/ImageFsInstaller.java`
- `com/winlator/xenvironment/components/BionicProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/GlibcProgramLauncherComponent.java`
- `com/winlator/core/GeneralComponents.java`
- `com/winlator/core/DXVKHelper.java`
- `com/winlator/contents/AdrenotoolsManager.java`
- `com/winlator/xenvironment/components/VortekRendererComponent.java`

## Current Transfer Order

1. foundation already landed:
   `GPUHelper`, `GPUInformation`, `GPUImage`, `XConnectorEpoll`
2. payload/manifest foundation now landed:
   `ManifestData`, `ManifestRepository`, `ManifestInstaller`,
   `ManifestComponentHelper`
3. next: launcher/runtime execution lane
4. next: launch dependency / pre-install lane
4. next: rootfs/imagefs hybrid audit lane
5. next: container routing / external launch lane
6. next: graphics-component placement and wrapper policy lane
7. then: Wine/Proton UI management surface

## Hard Rules For This Transfer

- Do not overwrite `aesolator` `Contents` provenance rules.
- Do not demote existing forensic/reporting contracts.
- Do not mix donor package logic into `WCP Archive` / `WCPHub` labels without
  explicit source separation.
- Do not import app-specific storefront logic unless it materially improves the
  runtime/payload/container stack.
- Prefer reusable runtime logic over donor UI shell code.

## Immediate Next Patch Candidates

- donor launcher/runtime compare:
  `GuestProgramLauncherComponent`, `BionicProgramLauncherComponent`,
  `GlibcProgramLauncherComponent`
- donor payload/install compare:
  `ManifestInstaller`, `ManifestComponentHelper`, `PreInstallSteps`
- donor env / component compare:
  `DXVKHelper`, `GeneralComponents`, `AdrenotoolsManager`
