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

- `container/Container.java` donor-compatible runtime-state expansion
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
- `data/TouchGestureConfig.kt`
- `xserver/PhysicalControllerHandler.kt`
- `externaldisplay/ExternalDisplayInputController.kt`
- `externaldisplay/ExternalDisplaySwapController.kt`
- `externaldisplay/ExternalOnScreenKeyboardView.kt`
- `externaldisplay/IMEInputReceiver.kt`
- `externaldisplay/SwapInputOverlayView.kt`
- `xconnector/XConnectorEpoll.java` rlimit bootstrap hook
- `cpp/winlator/xconnector_epoll.c` tracked fd close / ancillary-fd handling

See also:

- [GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md](/data/data/com.termux/files/home/aesolator/docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md)
- [GAMENATIVE_SECOND_SWEEP_INVENTORY.md](/data/data/com.termux/files/home/aesolator/docs/GAMENATIVE_SECOND_SWEEP_INVENTORY.md)

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

- donor container/runtime contract is now staged much more fully
- remaining open work is routing/migration behavior, not missing base fields

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
- installer/overlay foundation imported into the local tree
- explicit rootfs layer ownership is now written down
- per-library base/overlay adoption is now written down
- archive diff/extraction pass still open

Why it matters:

- donor rootfs versioning is newer (`26` vs local `21`)
- donor uses variant-specific base archives and post-extract overlays
- donor delivery path is now partially mapped too:
  primary `downloads.gamenative.app`, fallback R2 bucket, plus
  `imagefs_patches_gamenative.tzst`
- donor preserves imported `Wine` / `Proton` payloads more explicitly
- rootfs quality affects every later lane:
  launcher, Wine/Proton, drivers, multimedia, wrapper hooks, and container boot

Canonical doc:

- [IMAGEFS_HYBRID_PLAN.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_HYBRID_PLAN.md)

Current imported status:

- local `ImageFsInstaller` now tracks donor-style `LATEST_VERSION = 26`
- installer is now variant-aware at the foundation level:
  `imagefs_gamenative.txz`, `imagefs_bionic.txz`, with fallback to legacy
  `imagefs.txz`
- installer now mirrors donor rootfs delivery more honestly too:
  when a donor variant archive is not bundled locally, it tries
  `downloads.gamenative.app/<archive>` first and then the donor R2 fallback
- donor overlay assets `redirect.tzst` and `extras.tzst` are staged locally
  and deployed after base extraction when present
- glibc rootfs support now includes staged handling for
  `imagefs_patches_gamenative.tzst` instead of treating the base archive as the
  only donor payload that matters
- imported `Wine` / `Proton` payloads in `opt/` are now preserved more like the
  donor path instead of being wiped on every reinstall
- local `Container` now carries `containerVariant`, and launch-time code writes
  `imagefs` `.variant` / `.arch` markers before runtime bootstrap
- local main-runtime compatibility now bridges donor `/opt/wine` and
  legacy/local `/opt/<main-runtime-id>` so the rootfs lane does not silently
  assume only one historical layout
- donor `container_pattern_gamenative.tzst` and `pulseaudio-gamenative.tzst`
  are now staged locally, and glibc/main-runtime paths now point at those
  donor assets instead of only the older generic `container_pattern_common`
  / `pulseaudio.tzst` route
- local `ContainerManager` now accepts both `prefixPack.tzst` and
  `prefixPack.txz` for runtime fallback extraction, matching donor intake more
  closely
- local `ContentsManager` now also closes the next runtime-placement tail:
  installed `Wine` / `Proton` packages resolve their effective runtime root
  from the shared parent of `wineBinPath` / `wineLibPath` / `winePrefixPack`,
  and post-install hooks normalize `lib/wine` plus restore executable bits on
  installed binaries before launcher/container code consumes them
- donor runtime asset parity is now much closer too:
  the local tree now stages the missing `graphics_driver`, `dxwrapper`,
  `fexcore`, `wowbox64`, `steampipe`, `wincomponents`, `box86_64`,
  `steaminput`, `steam_regions.json`, and `box86_env_vars.json` payloads that
  were previously donor-only
- local `ImageFsInstaller` now also carries a donor-derived
  `generateCompactContainerPattern()` helper adapted to the bridged main-runtime
  path instead of assuming only the older local layout
- donor app-wrapper/source parity is now staged too:
  `PrefManager.kt`, `container/ContainerData.kt`,
  `contentdialog/NavigationDialog.java`, and `xserver/XKeycode.kt`
- local build perimeter is now prepared for the staged donor Kotlin lane via
  `org.jetbrains.kotlin.android`; this is a pre-compile closure step only, not
  compile proof
- explicit layer ownership is now tracked in
  [IMAGEFS_LAYER_OWNERSHIP_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md),
  including the classification of `imagefs.txz.02` as orphan/invalid baggage
  instead of a live shard
- donor-rootfs-first runtime policy is now staged in code too:
  shared `imagefs/opt` runtime installs for `Wine` / `Proton`, canonical `/tmp`
  plus `/usr/tmp` compat bridge, canonical `usr/local/bin/box64` plus
  `usr/bin/box64` compat bridge, rootfs provider/layout markers, and
  `:ubuntufs` dynamic-feature scaffold
- per-library adoption is tracked in
  [IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md](/data/data/com.termux/files/home/aesolator/docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md)

Still open inside this lane:

- first honest compile/runtime proof for the donor-rootfs-first lane
- future cleanup filter enforcement for donor archive noise (`.DS_Store`, `._*`)
- any remaining per-library refinements discovered during real runtime proof,
  not during paper inventory

### Lane 2: Guest Program Launchers

Primary donor files:

- `com/winlator/xenvironment/components/GuestProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/BionicProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/GlibcProgramLauncherComponent.java`
- `com/winlator/xenvironment/components/WineRequestComponent.java`
- `com/winlator/xenvironment/components/SteamClientComponent.java`
- `com/winlator/xenvironment/components/NetworkInfoUpdateComponent.java`

Status:

- foundation imported into the local tree
- still not wired through the whole runtime stack yet
- high-value lane remains active because execution policy and payload/runtime
  placement still need the next donor passes

Why it matters:

- binds installed runtime to actual launch path
- controls env shaping, preload, launcher sockets, wineserver lifecycle
- contains donor execution logic for Box64/FEX/WoW64 and runtime extras

Planned order:

1. compare `GuestProgramLauncherComponent`
2. compare Bionic / glibc launchers
3. extract reusable runtime/env logic
4. only then evaluate donor-specific Steam extras

Current imported status:

- local `Container` now carries donor-style runtime fields and JSON keys for:
  Steam type, graphics-driver version, exec args, executable path, session
  metadata, install path, box86 state, SDL/controller flags, gesture config,
  external-display state, suspend policy, DRM flags, and portrait mode
- `ImageFs` now exposes donor-style runtime markers:
  `.variant`, `.arch`, glibc/bin/lib accessors, storage/files roots, and
  `getRuntimeLibcModel()`
- `GuestProgramLauncherComponent` now has donor-style extension hooks for
  launcher model, env shaping, command building, and runtime-path contracts
- local `BionicProgramLauncherComponent` and `GlibcProgramLauncherComponent`
  are present as donor-derived execution models
- `GuestProgramLauncherFactory` now chooses the launcher from the active
  `ImageFs` runtime libc model
- `XServerDisplayActivity` no longer hardcodes one launcher type and now writes
  runtime libc markers dynamically
- `NetworkHelper` now exposes donor-style `IFAddress`, active-link probing, and
  IPv4 discovery, and `NetworkInfoUpdateComponent` is now part of the local
  runtime environment component set
- local `WineRequestComponent` now replaces the old standalone request handler
  and lives inside `XEnvironment`; request routing keeps donor socket/lifecycle
  structure while preserving the local Android clipboard bridge
- donor `SteamPipeServer` / `SteamClientComponent` foundation is now staged in
  the local tree for later runtime/storefront wiring
- donor `ControllerManager`, `TouchMouse`, and Kotlin `XKeycode` are now also
  staged locally, so the next closure step for input is behavior/compile
  validation instead of filename parity
- donor second-sweep low-dependency input/display foundation is now staged
  too: `TouchGestureConfig`, `PhysicalControllerHandler`, and the
  external-display classes. The remaining open part is integration and donor
  app-routing logic, not missing foundation classes.

Still open inside this lane:

- decide where and when local runtime should actually mount
  `SteamClientComponent`, since `Ae.solator` does not yet have a donor-style
  Steam source lane driving it
- decide whether any donor auth-specific request branches need a local
  `Ae.solator` equivalent beyond plain browser/clipboard routing
- wire launcher/runtime placement deeper into payload install and rootfs
  hybridization work

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

- donor scope audited
- local-adapted routing/config foundation is now staged in the tree
- remaining work is compile/runtime verification and any future storefront
  service integration on top of the staged bridge

Why it matters:

- cleaner pending-launch handling
- better container lookup and migration utility
- more disciplined handoff from UI intent to runtime execution

Current local adaptation:

- `PrefManager` now exposes a donor-compatible property surface on top of local
  `SharedPreferences`
- `ContainerUtils` is now staged locally with a `sessionMetadata` bridge for
  donor `appId` semantics instead of replacing the numeric local
  `ContainerManager` identity model
- `IntentLaunchManager` now stages donor-style `LaunchRequest` parsing and
  temporary in-memory config override flow
- `ContainerMigrator` is staged for legacy directory migration
- `ContainerConfigTransfer` is staged for flat JSON import/export through the
  local container bridge

Remaining tail:

- first honest compile/runtime validation of this staged lane
- decide how much storefront-specific service logic should ever sit on top of
  the appId bridge in `Ae.solator`

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

Current imported status:

- donor arch-specific input DLL payloads are now staged locally:
  `arm64ec_input_dlls.tzst`, `x86_64_input_dlls.tzst`
- local `XServerDisplayActivity.extractInputDLLs()` now prefers those
  arch-specific assets when the active runtime arch matches, and falls back to
  legacy `input_dlls.tzst` otherwise

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

## Pre-Compile Closure Update

Latest closure from the current batch:

- donor `Vortek` foundation is now staged locally:
  `VortekRendererComponent`, `VortekConfigDialog`,
  `graphics_driver/vortek-2.0.tzst`, `graphics_driver/vortek-2.1.tzst`,
  and donor `libvortekrenderer.so`
- donor `virgl` APK native parity is staged locally through
  `libvirglrenderer.so`
- donor bionic helper-libs are now staged and owned explicitly:
  `libevshim.so` and `libdummyvk.so` are mirrored into guest `usr/lib`, while
  `libvirglrenderer.so` and `libvortekrenderer.so` stay APK-native
- donor env-var metadata is staged locally through
  `core/envvars/EnvVarInfo.kt` and `EnvVarSelectionType.kt`
- local `UnixSocketConfig` now carries donor parity constants for
  `VORTEK_SERVER_PATH` and `STEAM_PIPE_PATH`

Important native conclusion:

- donor `libwinlator_11.so` exists only as a binary in the donor repo
- exported JNI symbols show it belongs to the donor's older native
  `XInputStream` / `XOutputStream` model
- local `cmod` already supersedes that lane with source-backed Java streams
  and richer local native `GPUInformation`
- therefore `libwinlator_11.so` is classified as donor reference only, not as
  the local native source-of-truth
