# GameNative Runtime Gap Inventory

Updated: `2026-03-15`

## Scope

Focused inventory of the remaining donor-runtime gaps after the latest
pre-compile transfer passes.

This document intentionally excludes most of `app/gamenative/*` store, auth,
library, Compose UI, and platform-service code. It tracks only the parts that
still matter for:

- runtime payload delivery
- rootfs / overlay placement
- container launch
- input / X11 / renderer plumbing
- Wine / Proton / driver / wrapper infrastructure

## Runtime Asset Parity

The following donor asset lanes are now staged locally in
`app/src/main/assets`:

- `graphics_driver/`
  - additional Turnip / Adrenotools / wrapper / Vortek / VirGL / Zink payloads
- `dxwrapper/`
  - donor `cnc-ddraw`, older `DXVK` branches, additional `VKD3D`, async
    variants
- `fexcore/`
  - donor `2507`, `2511`, `2512`, `2601`, `2603`
- `wowbox64/`
  - donor `0.3.4`, `0.3.6`, `0.4.0`
- `steampipe/`
  - `steam_api.dll`, `steam_api64.dll`
- `wincomponents/`
  - donor `opengl.tzst`, `wmdecoder.tzst` plus the rest of the lane
- `box86_64/`
  - donor `box64` archives, rc presets, and rc profile payloads
- `steaminput/`
  - donor controller VDF payloads
- root-level donor runtime metadata
  - `steam_regions.json`
  - `box86_env_vars.json`

Result:

- the previously obvious asset-side runtime gaps in the donor tree are no
  longer missing locally
- remaining donor-runtime gaps are now mostly code-side or native-side, not
  payload-side

## Remaining Donor Code Gaps

Latest file-level inventory result:

- donor `com/winlator/*` Java/Kotlin relative-path delta versus local
  `com/winlator/cmod/*`: `MISSING_COUNT 0`
- donor filename parity is now closed for the source-backed Java/Kotlin lane
- the remaining meaningful gap is no longer missing donor wrapper files; it is
  binary-only donor native behavior and the first honest compile/verification
  pass

### High-Risk / Not Blind-Copy Safe

- donor `libwinlator_11.so`

Why still open:

- it is binary-only in the donor repo
- exported JNI symbols map to the donor's older native
  `XInputStream` / `XOutputStream` path
- local `cmod` already uses source-backed Java-side stream classes and richer
  local native `GPUInformation`, so a blind binary swap would create false
  parity

Now closed from the same lane:

- `xenvironment/components/VortekRendererComponent.java`
- `contentdialog/VortekConfigDialog.java`
- donor `libvortekrenderer.so`
- donor `libvirglrenderer.so`
- donor bionic helper-libs `libevshim.so`, `libdummyvk.so`

### Input / Gesture Lane

Now staged locally:

- `inputcontrols/ControllerManager.java`
- `inputcontrols/TouchMouse.java`
- `xserver/XKeycode.kt`
- `data/TouchGestureConfig.kt`
- `xserver/PhysicalControllerHandler.kt`
- `externaldisplay/ExternalDisplayInputController.kt`
- `externaldisplay/ExternalDisplaySwapController.kt`
- `externaldisplay/ExternalOnScreenKeyboardView.kt`
- `externaldisplay/IMEInputReceiver.kt`
- `externaldisplay/SwapInputOverlayView.kt`

What remains open:

- behavioral validation and compile/runtime verification, not filename parity
- donor input/external-display import is now a quality/integration lane, not a
  missing-file lane
- the donor app-routing helper layer outside `com/winlator/*` is now staged
  locally too via:
  `container/ContainerUtils.kt`, `container/IntentLaunchManager.kt`,
  `container/ContainerMigrator.kt`, `container/ContainerConfigTransfer.kt`,
  and `data/GameSource.kt`
- remaining risk there is compile/runtime proof and storefront-service
  integration scope, not missing helper files

### Superseded Or Locally Re-Implemented

- `box86_64/Box86_64PresetManager.java`
- `box86_64/rc/*`
- `core/envvars/EnvVars.java`

Why these no longer block parity:

- local tree already has `box64/Box64Preset.java`,
  `box64/Box64PresetManager.java`, and related preset UI
- donor `Box86_64Preset.java` compatibility shell is already staged locally
- donor `core.envvars.EnvVars.java` compatibility wrapper is already staged
  locally
- local glibc launcher already consumes staged donor rc payloads through
  `config.box64rc`, so the remaining gap is donor app-management code, not the
  underlying preset payload lane
- donor assets for `box86_64` / rcfiles are now staged locally, which closes
  the payload gap first
- code parity here should be treated as an adaptation decision, not a raw copy

### Donor App-Wrapper Lane

Now staged locally:

- `PrefManager.kt`
- `container/ContainerData.kt`
- `container/ContainerUtils.kt`
- `container/IntentLaunchManager.kt`
- `container/ContainerMigrator.kt`
- `container/ContainerConfigTransfer.kt`
- `data/GameSource.kt`
- `contentdialog/NavigationDialog.java`

Notes:

- `PrefManager.kt` is adapted to local `SharedPreferences` ownership rather
  than importing donor datastore/storefront state
- `ContainerData.kt` is staged as a local compatibility snapshot without
  Compose saveable dependencies before the first honest compile
- `ContainerUtils.kt` is adapted through local `sessionMetadata` appId mapping
  instead of mutating the numeric local `ContainerManager` identity model into
  donor string IDs
- `IntentLaunchManager.kt` now stages donor-style launch-request parsing and
  temporary container-config override flow on top of the local container layer
- `ContainerMigrator.kt` and `ContainerConfigTransfer.kt` are now staged as
  source-backed local bridges rather than left as paper-only donor references
- `NavigationDialog.java` is staged on top of local resources/contracts rather
  than donor app-shell resources

### Low-Value Runtime Tail

- none currently blocking the first honest compile boundary

Current donor reality:

- donor `core/Win32AppWorkarounds.java` is now staged locally as a lightweight
  compatibility shell, and it remains low-value until a real app-specific
  workaround map is proven necessary

### Remaining Donor APK Native Libraries

- `libc++_shared.so`
- `libextras.so`
- `libhook_impl.so`
- `libmain_hook.so`
- `libopenxr_loader.so`
- `libpatchelf.so`
- `libwinlator.so`
- `libwinlator_11.so`
- `libzstd-jni-1.5.2-3.so`

Why not imported blindly:

- some are already produced locally by the build (`libwinlator.so`)
- some belong to generic toolchain/runtime packaging rather than donor-specific
  app logic (`libc++_shared.so`, `libzstd-jni-1.5.2-3.so`)
- some are donor opaque helper binaries with no current local call-site or no
  source-backed ownership decision yet (`libextras.so`, `libhook_impl.so`,
  `libmain_hook.so`, `libopenxr_loader.so`, `libpatchelf.so`,
  `libwinlator_11.so`)

## Honest State Before First Compile

Closed now:

- rootfs delivery / fallback foundation
- explicit rootfs layer ownership table
- explicit rootfs per-library adoption table
- donor overlay staging
- main runtime `/opt/wine` bridge
- donor-compatible `Container` runtime-state contract
- runtime-root resolution from package metadata
- `lib/wine` post-install normalization
- runtime-root `prefixPack.*` availability
- executable-bit repair for imported runtimes
- Steam-side runtime infrastructure wiring
- runtime asset parity for the major donor payload lanes
- explicit orphan classification for local `imagefs.txz.02`

Still open before claiming deeper code parity:

- donor binary-only `libwinlator_11.so` assessment / non-adoption
- honest compile-time/runtime validation of the newly staged donor Kotlin/app
  wrapper lane
- honest compile/runtime validation of the donor-rootfs-first lane

## Next Candidate Moves

If the transfer batch continues before the first compile, the best next code
targets are:

1. Compile the transfer batch honestly after the remaining non-code tails are
   closed.
2. Keep `libwinlator_11.so` documented as an opaque reference unless a source
   reconstruction lane is opened explicitly.
3. Validate the staged donor input/app-wrapper lane by behavior, not by file
   presence.
4. Validate the donor-rootfs-first lane in a real build/runtime pass before any
   archive rebuild claim.
