# GameNative Second Sweep Inventory

Updated: `2026-03-15`

## Purpose

This is the second broad donor sweep across `GameNative`, after the
file-by-file parity pass under `com/winlator/*` reached `MISSING_COUNT 0`.

The goal here is to catch useful runtime-adjacent logic outside the classic
`com/winlator` tree before the first honest compile.

## Current Result

The second sweep is no longer just a warning list.

Low-dependency donor foundation from `app/gamenative/*` is now staged locally:

- widened donor-compatible `Container` contract
- `TouchGestureConfig`
- external-display input/swap/IME/overlay foundation
- standalone `PhysicalControllerHandler`

The remaining donor surfaces are now mostly storefront/service integration,
dynamic-feature delivery, rootfs archive ownership, and Compose-heavy settings
UI.

## Runtime-Relevant Donor Areas Still Outside Local Source Parity

### 1. Container routing / launch request layer

Relevant donor files:

- `app/gamenative/utils/ContainerUtils.kt`
- `app/gamenative/utils/ContainerMigrator.kt`
- `app/gamenative/utils/IntentLaunchManager.kt`
- `app/gamenative/ui/util/ContainerConfigTransfer.kt`

Current local state:

- the compatibility foundation for `PrefManager`, `ContainerData`, and the
  widened donor-style `Container` contract is now in tree
- the donor routing/helper layer is now staged locally too:
  `ContainerUtils`, `IntentLaunchManager`, `ContainerMigrator`,
  `ContainerConfigTransfer`, and local `GameSource`
- local adaptation uses a `sessionMetadata` bridge for donor `appId` semantics
  because `Ae.solator` still has a numeric `ContainerManager` identity model

Why it is still open:

- remaining risk is no longer missing source files
- what remains is honest compile/runtime proof plus the decision boundary for
  future storefront-backed services on top of the staged appId bridge

### 2. External display and split-input lane

Relevant donor files:

- `app/gamenative/externaldisplay/ExternalDisplayInputController.kt`
- `app/gamenative/externaldisplay/ExternalDisplaySwapController.kt`
- `app/gamenative/externaldisplay/IMEInputReceiver.kt`
- `app/gamenative/externaldisplay/SwapInputOverlayView.kt`

Why it matters:

- donor has a deeper external-display / input-swap model than current local
  code
- this could eventually strengthen dual-surface desktop and controller routing

Current local state:

- donor foundation is now staged locally:
  `ExternalDisplayInputController`, `ExternalDisplaySwapController`,
  `ExternalOnScreenKeyboardView`, `IMEInputReceiver`,
  `SwapInputOverlayView`
- donor colors/strings for this lane are also staged locally

Why it is still open:

- it is staged as foundation only, not integrated into the live
  `XServerDisplayActivity` flow yet
- it still needs product/runtime verification before being called finished

### 3. Touch gesture settings / Compose input UX lane

Relevant donor files:

- `app/gamenative/data/TouchGestureConfig.kt`
- `app/gamenative/data/ManifestInfo.kt`
- `app/gamenative/ui/component/dialog/TouchGestureSettingsDialog.kt`
- `app/gamenative/ui/component/dialog/ControllerBindingDialog.kt`
- `app/gamenative/ui/component/dialog/PhysicalControllerConfigSection.kt`
- `app/gamenative/ui/component/dialog/ControlTab.kt`

Why it matters:

- donor keeps a richer touchscreen/touchpad configuration model
- this is the likely next place to mine after the current desktop input lane is
  stable again

Current local state:

- the data foundation is no longer missing:
  `TouchGestureConfig.kt` is staged locally
- the standalone physical-controller runtime layer is staged too via
  `PhysicalControllerHandler`

Why it is still open:

- the remaining donor surface is now mostly Compose dialog/editor UX around
  that data model
- current batch priority remains runtime/container/rootfs parity before full
  donor UI/editor adoption

### 4. Dynamic-feature rootfs delivery

Relevant donor files:

- `ubuntufs/build.gradle.kts`
- `ubuntufs/src/main/AndroidManifest.xml`
- donor `app/build.gradle.kts`
- donor `settings.gradle.kts`

What it tells us:

- donor rootfs delivery is not just archive download logic
- `GameNative` also ships an on-demand `ubuntufs` dynamic feature shell around
  that rootfs lane

Current decision:

- keep this as reference for future delivery architecture
- do not force dynamic-feature adoption before the first honest compile of the
  transferred source batch

### 5. Storefront/service-specific manifest lane

Relevant donor files:

- `app/gamenative/service/epic/manifest/*`
- `app/gamenative/service/gog/*Manifest*`
- `app/gamenative/service/amazon/AmazonManifest.kt`

Current local state:

- manifest foundation has already been adapted locally where it was reusable
- donor service/store coupling remains outside current import scope

## Already Covered By Earlier Imports

The second sweep confirmed that these areas are already represented locally and
are no longer donor-only:

- donor-compatible container runtime state:
  widened `Container.java`
- manifest foundation:
  `ManifestInstaller`, `ManifestRepository`, `ManifestComponentHelper`, models
- launcher/runtime foundation:
  `ImageFs`, launcher split, `WineRequestComponent`, `SteamClientComponent`,
  network/runtime components
- input foundation:
  `ControllerManager`, `TouchMouse`, `XKeycode`,
  `TouchGestureConfig`, `PhysicalControllerHandler`
- external-display foundation:
  `ExternalDisplayInputController`, `ExternalDisplaySwapController`,
  `ExternalOnScreenKeyboardView`, `IMEInputReceiver`,
  `SwapInputOverlayView`
- renderer/driver foundation:
  `GPUHelper`, `GPUInformation` classification, `GPUImage`,
  `xconnector_epoll`, Vortek foundation
- runtime/rootfs assets:
  `graphics_driver`, `dxwrapper`, `fexcore`, `wowbox64`, `steampipe`,
  `wincomponents`, `box86_64`, `steaminput`, `steam_regions.json`,
  `box86_env_vars.json`

## Practical Conclusion

The second sweep did not reveal a missed core `com/winlator` donor lane.

What remains is broader product/runtime architecture:

- storefront-aware container routing and temporary override flow
- config import/export / migration helpers tied to donor `appId` semantics
- Compose gesture/settings UX around the now-staged data foundations
- dynamic-feature rootfs delivery
- first honest compile/runtime proof for the now-staged donor-rootfs-first lane

Those are now explicit future lanes, not hidden omissions.
