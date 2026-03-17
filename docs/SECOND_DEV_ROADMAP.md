# Second Developer Roadmap

Updated: `2026-03-16`

## Mission

Second autonomous developer lane for `aesolator`:

- tighten app/UI behavior against the active split model,
- keep runtime/forensic contracts visible,
- close remaining `Contents` UX and device-validation gaps,
- report deltas clearly to the first developer.

## Execution Policy

- Convert multi-goal user messages into one ordered backlog and keep that order
  visible instead of bouncing between fresh requests ad hoc.
- When the user explicitly says "do not compile until the full transfer is
  done", keep the active donor-transfer lane build-free. Land code, docs, and
  staged transfer work first; reopen `assembleDebug` / APK verification only after that
  lane reaches a written closure point or the user changes the rule.
- When the user says the full transfer is one batch, keep donor work under one
  cumulative commit target instead of cutting per-lane commits while the
  transfer is still in flight.
- Default lane order for this repo:
  requested product/UI/content work first, documentation sync second,
  debugging/forensics third.
- Break the order only when a blocker crash/build/runtime defect prevents the
  next planned task from moving.
- Every forced reorder must be logged in the reflective journal with:
  blocker, reason, scope impact, and explicit return point.

## Current Priorities

0. Desktop bootstrap and host-launch closure
   - keep `Ae.solator` consumer-only for host `LLVM 22.1.1`; the owner-side CI
     lane now lives in `wcp-runtime-lanes/main`
   - keep all future host-compiler CI/release work on `wcp-runtime-lanes/main`
     directly; do not re-open branch-first LLVM orchestration in `aesolator`
   - close the fresh `winex11.drv` init regression under direct
     `android_bionic_wowbox64_guest`
   - verify that redirect/sysvshm preload and host X11 closure agree with the
     donor `GameNative` launch contract before widening into cursor UX again

1. `New Container` device-led UX closure
   - keep container creation free of XR/native startup regressions
   - polish the runtime-missing state, footer actions, and tab readability from
     real screenshots instead of XML-only inference
   - normalize boolean controls and env-var editing so the flow stops looking
     like a mix of legacy checkbox/toggle widgets
   - run top-to-bottom behavioral QA on the full create flow
   - confirm the new cold-start routing after deferred prompts on a stable
     foreground session
   - finish no-shortcut desktop input closure:
     preserve the cursor-touchpad model, but make single taps hit the intended
     target without requiring the user to fight stale cursor position
2. `Contents` integrated source closure
  - keep `WCP Archive` and `WCPHub` visible together without provenance drift
  - preserve top-card sizing/alignment and selector readability
  - verify install/update/remove behavior on the current build
  - keep overlapping `WCPHub` families (`Wine`, `DXVK`, `VKD3D`) visible when
    the source selector is explicitly switched away from archive
  - keep content badges semantically aligned with the selected family instead
    of leaking `Proton` naming into non-Wine package rows like `VKD3D`
  - expand GameHub release ingestion beyond a single page and sort the visible
    releases by source lane, channel, version, architecture, and package format
  - keep `Proton` and `Wine` routed through verified runtime sources only until
    a donor feed exposes complete packaged runtimes instead of raw skeleton
    payloads
  - audit `BannersComponentInjector` as a donor for full package-link harvest,
    release-tag browsing, search/sort UX, and release-notes surfaces without
    collapsing `Ae.solator` provenance rules
3. Dashboard adaptive polish
   - verify the new landscape density pass on a stable device session
   - keep the main menu readable as a control surface, not a stretched portrait
     grid
4. Shared control-system polish
   - keep selectors, switches, seekbars, preference rows, and compact toggles
     on one geometry system instead of drifting by screen
   - prefer global style/resource fixes where possible, then validate on live
     `Contents`, `Settings`, and `Graphics Center` screens
   - keep long text readable by truncation or controlled wrapping, not marquee
5. `Contents` source-of-truth enforcement
   - `REMOTE_WINE_PROTON_OVERLAY` must track `aesolator/contents/contents.json`
   - `Wine` lane must expose archive-managed `freewine11`
6. Documentation sync
  - keep `AGENTS.md`, roadmap, and reflective journal current
  - keep implementation notes aligned with repo contracts
  - keep the Termux local-build path self-contained instead of relying on
    ad-hoc CLI flags for `aapt2`
7. Handoff/reporting discipline
   - summarize each completed step for the first developer
   - call out verification gaps explicitly
8. Donor and runtime reverse-engineering
  - inspect `The412Banner/BannersComponentInjector` for safe source/feed
    improvements and document every borrowed behavior before integration
  - map `imagefs` structure, libraries, overlays, and runtime patch points in
    detail before changing rootfs-related install logic
  - keep release artifacts traceable after install:
    source label, release tag, artifact name, published date, and notes should
    survive from remote feed to local profile metadata where available
9. `GameNative` X11 / renderer / driver import lane
  - audit donor code by subsystem, not by repository folklore
  - import only foundation pieces first:
    Vulkan probe helpers, fd/socket hygiene, hardware-buffer exposure,
    driver classification, and renderer/component plumbing
  - donor X11/input foundation now also includes arch-specific input DLL
    payload selection instead of one flat legacy archive
  - defer broad gesture/UI borrowing until desktop input closure is stable
  - keep the donor audit written in
    `docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md`
10. `GameNative` full runtime transfer lane
  - map all donor logic touching:
    payload intake, package placement, manifest install, pre-install steps,
    container routing, launch requests, Wine/Proton management, launcher
    components, and runtime execution
  - keep the global transfer matrix in
    `docs/GAMENATIVE_FULL_TRANSFER_MATRIX.md`
  - keep the donor manifest layer local-first:
    adapt `ManifestRepository`, `ManifestInstaller`, and
    `ManifestComponentHelper` on top of existing `ContentsManager` and
    `AdrenotoolsManager`, not as a second package manager
  - transfer foundation before surface UX:
    launcher/runtime stack first, payload/manifest stack second,
    routing/config stack third
  - current donor-launcher foundation is already in tree:
    `ImageFs` runtime markers, launcher factory, and local Bionic/glibc
    launcher split are imported; the next closure step is `ImageFsInstaller`
    / runtime placement and request-path parity
  - donor network/runtime environment parity has started too:
    `NetworkHelper` now carries donor IF-address probing and
    `NetworkInfoUpdateComponent` is staged into the local environment stack
  - donor request routing is now being absorbed into `XEnvironment` too:
    `WineRequestComponent` replaces the old standalone handler model, with the
    remaining open tail narrowed to donor-specific Steam/auth branches
  - donor `SteamPipeServer` / `SteamClientComponent` foundation is no longer
    just staged locally; `SteamClientComponent` is now attached to the local
    `XEnvironment`, so the remaining tail is runtime verification rather than
    basic wiring
  - latest donor-wrapper closure:
    `PrefManager.kt`, `container/ContainerData.kt`,
    `contentdialog/NavigationDialog.java`, and Kotlin `xserver/XKeycode.kt`
    are now staged locally, and the file-level donor Java/Kotlin delta under
    `com/winlator/*` is currently `MISSING_COUNT 0`
  - latest build-perimeter closure:
    the app module is now prepared for the staged donor Kotlin lane via the
    Kotlin Android plugin, but this remains a pre-compile closure point until
    the user reopens the first honest build
  - second-sweep closure:
    a broad donor pass outside `com/winlator/*` is now recorded in
    `docs/GAMENATIVE_SECOND_SWEEP_INVENTORY.md`; remaining donor surfaces are
    storefront/external-display/Compose/dynamic-feature lanes, not forgotten
    core runtime files
  - latest container-contract closure:
    local `Container` now carries donor-style runtime state and JSON keys for
    Steam type, graphics-driver version, exec args, executable path, install
    path, session metadata, box86 state, gesture config, external-display
    state, suspend policy, DRM flags, and portrait mode
  - latest second-sweep foundation closure:
    donor low-dependency `app/gamenative/*` runtime pieces are now staged
    locally too: `TouchGestureConfig`, `PhysicalControllerHandler`, and the
    external-display foundation (`ExternalDisplayInputController`,
    `ExternalDisplaySwapController`, `ExternalOnScreenKeyboardView`,
    `IMEInputReceiver`, `SwapInputOverlayView`)
  - latest donor app-routing closure:
    `ContainerUtils`, `IntentLaunchManager`, `ContainerMigrator`,
    `ContainerConfigTransfer`, and a local `GameSource` model are now staged
    too; donor `appId` semantics are bridged through `Container.sessionMetadata`
    instead of replacing the numeric local `ContainerManager` identity model
  - remaining donor second-sweep tail is now honest rather than missing-file
    based:
    compile/runtime proof for the staged lane, storefront-service decisions,
    and rootfs archive ownership mapping
11. Hybrid `ImageFS` refresh lane
  - treat `GameNative` `ubuntufs` as a donor source map, not as the final
    product rootfs
  - reverse donor `ImageFsInstaller`, variant-specific archives, and overlay
    payloads (`imagefs_gamenative.txz`, `imagefs_bionic.txz`,
    `redirect.tzst`, `extras.tzst`)
  - diff donor rootfs against current `Ae.solator imagefs.txz`
  - build a hybrid plan that keeps our `Contents` / runtime / forensic
    contracts while replacing stale userland libraries with better donor ones
  - document every adopted rootfs delta by subsystem before any archive rebuild
  - current foundation already imported:
    variant-aware `ImageFsInstaller`, donor overlay deployment, preserved
    imported runtimes in `opt/`, and `Container.containerVariant` /
    `imagefs .variant/.arch` markers
  - newly closed tail inside this phase:
    donor-style rootfs delivery fallback is now represented in code too
    (`downloads.gamenative.app` + R2 fallback), glibc patch payload
    prefetch is staged, donor `container_pattern_gamenative.tzst` /
    `pulseaudio-gamenative.tzst` are present locally, and container fallback
    now accepts both `prefixPack.tzst` and `prefixPack.txz`
  - latest rootfs compatibility closure:
    local main-runtime lookup now bridges donor `/opt/wine` and local
    `/opt/<main-runtime-id>`, and streamed donor archive inventory has already
    confirmed that `glibc` carries a heavy `opt/` tooling layer while `bionic`
    is closer to a clean userland/Vulkan surface
  - latest runtime-package placement closure:
    installed `Wine` / `Proton` packages now derive their effective runtime
    root from the shared parent of `wineBinPath` / `wineLibPath` /
    `winePrefixPack`, while post-install hooks normalize `lib/wine` layout,
    guarantee `prefixPack.*` at the effective runtime root, and restore
    executable bits on runtime binaries before the first honest compile
  - latest archive-diff closure:
    streamed inspection now shows local `imagefs.txz` and donor
    `imagefs_bionic.txz` are close on broad userland, while donor
    `imagefs_patches_gamenative.tzst` owns the extra `opt/system32` /
    `opt/apps` / `Steamless` / `7-Zip` utility overlay and donor `glibc`
    archives carry macOS packaging noise that must stay out of any rebuilt lane
12. Local host LLVM lane
  - build a dedicated `LLVM 22.1.1` toolchain for `Termux/Android`
  - keep it outside rootfs and outside APK payloads
  - use it as the pinned host compiler baseline for future `wine` and runtime
    compilation instead of mixing Termux `21.1.8` with NDK-host binaries
  - keep a documented high-performance non-root device profile for long local
    toolchain builds; current target profile is `JOBS=6` with fixed
    performance mode and one linker job
  - latest ownership-table closure:
    `docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md`, including the fact that
    `imagefs.txz.02` is orphan/invalid baggage rather than a live archive shard
  - latest donor-rootfs-first closure:
    canonical donor base is now staged in code too:
    `imagefs_bionic.txz` / `imagefs_gamenative.txz`, `:ubuntufs` scaffold,
    shared `imagefs/opt` for `Wine` / `Proton`, canonical `/tmp`, and
    `usr/local/bin/box64` with a compatibility bridge for `usr/bin/box64`
  - latest per-library closure:
    `docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md` now records which Vulkan,
    OpenGL, Pulse, sysvshm, redirect, XAudio/XACT, utility, and helper-library
    paths belong to donor base, donor overlays, APK-to-guest helper lane, or
    legacy hold
  - latest runtime-asset closure:
    the donor-only runtime payload gaps are now staged locally too:
    extra `graphics_driver`, `dxwrapper`, `fexcore`, `wowbox64`,
    `steampipe`, `wincomponents`, `box86_64`, `steaminput`,
    `steam_regions.json`, and `box86_env_vars.json`
13. Device migration readiness
  - keep `main` as the only real landing lane before device moves
  - keep a repo-tracked bootstrap path for a new `Termux/Android` host:
    packages, SDK/NDK, host LLVM fetch, and `local.properties`
  - keep the external handoff file and the repo bootstrap doc aligned so the
    next device can resume from the same blocker without archaeological work
14. `libwinlator_11.so` source-backed reconstruction lane
  - treat donor `libwinlator_11.so` as an audit surface, not a binary drop-in
  - record every reconstructible donor-native behavior in
    `docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md`
  - current closure:
    reconstructible source-backed behavior has largely been staged already
    (`GPUHelper`, `xconnector_epoll`, `GPUImage`, helper libs, Vortek
    foundation, stream-path parity)
  - remaining tail is compile/runtime proof, not more blind binary copying

## Phase Plan

### Phase 1: Contents Surface

- close layout friction in top cards and selectors
- ensure visible titles remain readable without forced wrapping in selectors
- preserve intentional wrapping only where action labels need it

### Phase 2: Contents Behavior

- verify source filter logic against `docs/CONTENTS_QA_CHECKLIST.md`
- verify archive/hub provenance rendering in list rows
- audit install-state transitions and duplicate/update behavior

### Phase 3: Device Validation Prep

- prepare ADB-oriented verification checklist from existing docs
- identify which items remain code-only vs device-only

### Phase 4: Donor Source Harvest

- audit `BannersComponentInjector` source/release UX and package-feed model
- port only contract-safe improvements:
  full release pagination, better search/sort, richer release metadata, and
  stronger package-link normalization
- verify any donor runtime package assumptions against actual extractable
  payload structure before exposing them in `Contents`

### Phase 5: ImageFS Reverse Map

- document base `imagefs` layout, shipped libraries, runtime overlays, and
  patch application points
- separate base rootfs facts from container-time prefix/runtime mutation
- identify which parts of `imagefs` are safe to optimize in-app versus those
  owned by runtime-lane artifacts

### Phase 6: GameNative Foundation Imports

- land donor-derived `GPUHelper` and native Vulkan probe support
- strengthen `GPUInformation` hardware classification and card lookup
- harden `XConnectorEpoll` native fd/rlimit hygiene
- prepare `GPUImage` / renderer plumbing for a future alternate Vulkan lane
- evaluate `VortekRendererComponent` only after the foundation layer settles

### Phase 7: GameNative Runtime And Payload Transfer

- compare and adapt donor launcher components:
  `GuestProgramLauncherComponent`, `BionicProgramLauncherComponent`,
  `GlibcProgramLauncherComponent`, `WineRequestComponent`
- compare and adapt donor payload/manifest install logic:
  `ManifestInstaller`, `ManifestComponentHelper`, `PreInstallSteps`,
  `LaunchDependencies`
- current closure inside this phase:
  donor manifest models/repository/installer/helper are now present locally as
  Java foundation on top of `ContentsManager`
- compare and adapt donor container routing:
  `ContainerUtils`, `IntentLaunchManager`, `ContainerMigrator`,
  `ContainerConfigTransfer`
- current closure inside this phase:
  donor routing/config-transfer foundation is now staged locally through a
  `sessionMetadata` appId bridge and local `SharedPreferences`-backed
  `PrefManager`
- compare and adapt donor Wine/Proton management surfaces only after the
  runtime stack beneath them is stable

### Phase 8: ImageFS Hybridization

- inventory donor `GameNative` rootfs delivery path:
  dynamic feature shell, real archive names, overlay payloads, and versioning
- keep this phase build-free until the rootfs transfer matrix says the base
  installer/overlay/runtime-placement foundation is in place
- diff donor `imagefs_gamenative.txz` / `imagefs_bionic.txz` against local
  `imagefs.txz`
- classify rootfs deltas by subsystem and ownership:
  base, overlay, runtime payload, mutable container state
- decide whether `Ae.solator` should move to:
  one hybrid archive, or two variant archives with shared overlays

## Current Open Risks

- The old `New Container -> black canvas / no taskbar` blocker is now reduced
  from an unknown launch failure to a specific shell-registry contract:
  `HKCU\Software\Wine\Explorer\Desktops\shell` must be seeded before launch.
  With that registry prep in place, a direct on-device launch now reaches and
  holds a real desktop surface with `Start` visible after the bootstrap window.
  Remaining risk is now narrowed to follow-up desktop input UX and
  package/runtime integration, not the prior shell-collapse loop.
- The next desktop-closure gate after shell/taskbar visibility is direct input
  reachability on live hardware. The old hidden-touchpad blocker is closed,
  and `Start` now opens through the live `TouchpadView` transport as well as
  the shell itself, but the current narrow tail has shifted again:
  long-session cursor behavior, pointer-state drift, and multi-gesture/manual
  interaction still need live validation after the tap transport fix.
- Cursor ownership is now explicitly split: desktop shell surfaces may keep the
  GL/X11 root fallback cursor, while fullscreen-like non-shell app windows
  suppress that fallback to avoid the old “guest cursor + X11 cursor” double
  image. Remaining cursor risk is therefore quality of the ownership heuristic,
  not the old unconditional fallback.
- The desktop input baseline was then refined again on March 14, 2026 from a
  touch-surrogate cursor to an explicit `cursor_touchpad` model with a visible
  centered pointer. Remaining desktop work should preserve that desktop-style
  cursor semantics unless the user explicitly asks for tablet-style direct
  touch. `GameNative` is now the donor reference for that cursor path, and the
  old async move/button queue has been retired in favor of a direct
  trackpad-style contract. The current baseline is `cursor_trackpad`:
  movement by delta, click at the current cursor position, no jump-to-tap.
  The next closure step is a live manual pass for cursor drag, repeated clicks,
  and post-click state stability, not another shell/bootstrap rewrite.
- `New Container` now has a verified end-to-end baseline on-device: a local
  donor runtime resolves correctly, `Create` reaches `Creating Container…`,
  and a real `/files/imagefs/home/xuser-*` container root is materialized.
  Remaining risk is now secondary runtime choice/defaulting and deeper
  post-create UX, not the old false missing-runtime gate.
- Container launch is no longer allowed to fail silently on orphaned
  `/files/imagefs/home/xuser-*` roots. If the prefix exists but `.container`
  metadata is missing, `ContainerManager` now recovers the config from the
  newest local `Proton`/`Wine` install and records
  `CONTAINER_CONFIG_RECOVERED`. The old `container_id -> null -> finish()`
  collapse on `XServerDisplayActivity` is closed for this failure mode.
- Direct cold-start validation for `selected_menu_item_id` flows is now coded
  more defensively in `MainActivity`, but shared-device foreground contention
  still limits clean screenshot proof for those routes.
- The landscape dashboard density pass is build-complete and installed, but it
  still needs one stable foreground capture to confirm the new 4-column control
  surface on the target phone.
- `Contents` no longer loses archive provenance on feed failure, but live
  `WCPHub` plus `WCP Archive` behavior still needs explicit on-device review
  across source switching and install actions. The `Install Runtime` path now
  lands on a populated `WCP Archive` / `Wine` view again.
- GameHub feed ingestion now includes paginated release polling and stronger
  visible ordering, but it still needs a live `Contents` device pass to confirm
  that nightly/stable and architecture variants render as intended in the UI.
- `Nightlies by The412Banner` is now integrated as a first-class `Contents`
  source lane for `Proton`, `DXVK`, `VKD3D`, `Box64`, `WOWBox64`, and
  `FEXCore`, but device-led validation of source switching and installation
  across those donor packages is still pending. Ordering now prefers
  `publishedAt`/`verCode`, and donor `Wine/Proton` intake now prefers
  compressed `.wcp.xz/.wcp.zst` artifacts because that is where the usable
  prefix-bearing payloads live.
- `Nightlies` source discovery is still vulnerable to unauthenticated GitHub
  API rate limiting on first load. Device logs now confirm `HTTP 403` from
  `api.github.com`, so the next closure step is a non-API or cached fallback
  path rather than more UI-only tuning.
- `GameNative` donor manifest foundation is now present locally, but it is not
  wired into any visible `Ae.solator` surface yet. Remaining risk is not
  parser/install capability; it is the missing UI/routing layer that exposes
  those donor entries without violating existing provenance rules.
- Rootfs strategy is now explicitly split from package/runtime logic. The
  donor path is clearer: `GameNative` uses a newer variant-aware rootfs
  installer backed by `imagefs_gamenative.txz` / `imagefs_bionic.txz` and
  extra overlays, but we have not yet unpacked and classified those archives
  against our current `imagefs.txz`. The risk is not lack of direction; it is
  lack of per-library adoption evidence.
- `Vulkan SDK` had a false installed-state in `Contents` because the base
  `imagefs` already ships `usr/share/vulkan`; package visibility now needs to
  stay tied to real `Contents` installs plus explicit `vulkanApiMin/max` /
  `vulkanSdkVersion` metadata so runtime pickup and UI state do not drift.
- `dgVoodoo` still had a split-brain contract: `Contents` packages lived in
  `contents/DgVoodoo/*`, while runtime stage/dependency checks looked only at
  `contents/dgvoodoo/current`. That bridge now needs to stay aligned so
  package installs, wrapper presence checks, and runtime staging all see the
  same installed payload set.
- `BannersComponentInjector` still has deeper donor logic not yet harvested
  locally, especially around richer source discovery, release-tag browsing, and
  download-management UX.
- Donor audit on March 14, 2026 confirmed that
  `BannersComponentInjector` is most useful here as an external source-manager
  reference, not as a desktop/runtime-launch source-of-truth. The immediately
  relevant donor value is feed/release parsing and package intake logic, while
  the container desktop fix remained an in-prefix Wine shell contract issue.
- A cross-repo handoff is now open for the `freewine11` build lane:
  review Valve Wine commit
  `6ccff11d0e7d620cd958b56b0904fcbd9a9bfb26` in the dedicated handoff note
  before the next runtime build churn.
- That handoff has widened into a full upstream compare task:
  `ValveSoftware/wine:proton_10.0...GameNative/proton-wine:proton_10.0`
  is a 3-commit Android/Winlator downstream layer, not just one isolated fix.
- `WCPHub` source parsing no longer drops overlapping families at ingest time,
  but the integrated device pass still needs one more live confirmation for
  list rendering and install actions after the parser fix.
- The shared selector/switch/preference geometry pass is now built and
  installed, but many secondary dialogs still have only code-level validation
  rather than screenshot proof on the target device.
- The hard container-launch freeze is no longer blocked in the old
  splash/orientation handoff: `XServerDisplayActivity` now boots past the stale
  portrait gate, `am start -W` completes again, and live forensics confirm
  `explorer.exe` window mapping. The remaining desktop risk has moved
  downstream into post-bootstrap interaction quality and payload consumers.
- Desktop shell bootstrap now has a termination grace window before the app
  honors a guest-launcher exit with zero mapped windows. This closes the
  early-race path where `explorer /desktop=shell` could exit milliseconds
  before the first shell window map and drag the activity into `exit()` too
  early. Remaining desktop risk is later interaction quality and true native
  process death, not that early bootstrap race.
- The desktop cursor stack now has an explicit ownership split in
  `GLRenderer`: fullscreen-like non-shell guest windows can suppress the
  compositor cursor entirely, not just the root fallback, so duplicate cursor
  reports are now narrowed to classification/owner detection rather than the
  old unconditional draw path.
- Manual trackpad taps now share the same queued left-click transport as the
  already proven `DESKTOP_DEBUG_START_PROBE_DISPATCHED` path. The remaining
  click risk is no longer "debug probe works but finger tap uses another code
  path"; it is now limited to tap recognition thresholds and any later
  pointer-state drift that might still appear on device.
- The `Computer`/file-manager tail is now classified as a desktop-icon
  interaction issue, not a missing runtime binary: the shell probe sweep found
  a working icon region, but the first anchored-tap experiment regressed normal
  desktop clicks and was rolled back. The remaining closure item is a safer
  `Computer` convenience path that does not destabilize the default cursor
  contract.
- Device-led desktop screenshot proof is still partially blocked by the fact
  that this Termux session shares the same physical phone: `am start -W`
  succeeds, but `Termux` can immediately retake foreground and invalidate the
  capture. That is an environment constraint, not the same thing as an app
  crash or `MotionEvent` ANR.
- `Graphics Driver Configuration` no longer crashes on open and no longer
  renders an empty extensions line: it now uses a safe catalog-backed
  extension source on device instead of the old native probe path that could
  crash inside `libwinlator.so`. Remaining risk is quality of the extension
  catalog itself, not dialog stability or selector presence.
- The `New Container` boolean-control/env-var cleanup is build-complete and
  installed, but stable screenshot proof of the deeper tabs is still limited by
  shared-device foreground hijacking during ADB capture.
- Device-side inspection now confirms local `Wine` and `Proton` package roots
  under `files/contents`, and the old `New Container` failure was a runtime-id
  resolution bug caused by `Contents` entry names carrying `-verCode` suffixes.
  The next pass should therefore focus on runtime selection quality and other
  downstream consumers, not on re-proving basic package presence.
- The repo-side `Contents` workflow contract is now aligned with the static
  checklist gate, so the remaining `Contents` risk is device behavior rather
  than source-of-truth drift inside `.github/workflows/ci-winlator.yml`.
- Termux local build currently depends on local-only SDK/NDK compatibility
  shims; this is an environment workaround, not a committed repository fix.
- `llvm-strip` from the desktop NDK host bundle is still incompatible with
  Termux ARM64, so debug packaging currently proceeds with unstripped native
  libraries.
- `imagefs` now has a documented reverse map, so the remaining rootfs risk is
  no longer “unknown structure” but future ownership mistakes between base
  image, Wine payloads, and boot-time overlays.
- donor `Vortek` Java-side renderer/config foundation and the matching APK
  native libraries are now staged locally. Remaining risk in that lane is no
  longer “missing donor subsystem”, but later runtime wiring and first compile
  verification.
- donor bionic helper-lib parity now has explicit local placement rules:
  `libevshim.so` and `libdummyvk.so` are mirrored into guest `usr/lib`, while
  `libvirglrenderer.so` and `libvortekrenderer.so` stay APK-native runtime
  dependencies.
- donor `libwinlator_11.so` has now been inventoried and deliberately not
  promoted to local source-of-truth. Its JNI surface maps to the donor's older
  native stream model; local `cmod` already supersedes that lane with
  source-backed Java streams and richer native `GPUInformation`.
- donor `box86_64` compatibility stack is now staged locally enough that the
  remaining donor Java/Kotlin delta has collapsed to six files:
  `PrefManager.kt`, `ContainerData.kt`, `NavigationDialog.java`,
  `ControllerManager.java`, `TouchMouse.java`, `XKeycode.kt`. That is now a
  bounded donor-app/input perimeter, not a missing runtime subsystem.
- Static audit after the donor-rootfs-first pass found three real compile
  blockers: launch still does not enforce rootfs variant per container, legacy
  `imagefs.txz` / `imagefs.txz.02` are still packaged as baggage, and
  `Contents` runtime resolution still has no explicit `glibc` / `bionic`
  contract.
- Those static blockers are now closed in code: launch enforces rootfs
  preparation per runtime model, `Contents` carries and resolves explicit
  `runtimeModel`, launcher selection follows the selected runtime contract, and
  shared `/opt` runtime roots are canonicalized as
  `runtime-<model>-<family>-<version>-<verCode>`.
- Remaining pre-compile observation is narrower now: embedded runtime arrays
  stay intentionally empty under the current Contents-first lane, and donor
  rootfs archives are still staged at build time. That is no longer a logic
  gap in the runtime contract; it is a first-compile proof burden.
- Clean static pass after that audit closed the next payload tails too:
  `ImageFsInstaller` no longer keeps legacy rootfs fallback in the live path,
  old `imagefs` archives were moved out of `app/src/main/assets` into
  `.legacy_rootfs/`, `Vulkan SDK` selection now works as one coherent version
  group instead of mixed arch crumbs, and `dgVoodoo` dependency checks now
  validate the stage arch rather than any installed package.
- Final static cleanup then removed the last live legacy-marker tail too:
  `ImageFs` normalizes old `.provider` / `.layout` values to donor markers,
  and `XServerDisplayActivity` now writes `gamenative` / `ubuntufs`
  unconditionally instead of branching back into legacy labels.
- Remaining risk before the first honest compile is now concentrated in proof,
  not in missing static contracts: donor archive staging, runtime application
  of the selected `Vulkan SDK` group, and end-to-end `dgVoodoo` stage/runtime
  verification.
- Shared-device desktop debugging now has one additional operating constraint:
  when Termux and `Ae.solator` share the same phone, prefer passive ADB
  forensics and `run-as` inspection over foreground launches from the same
  session. The current live desktop tails are narrowed to shell executable
  routing (`wfm.exe` vs `explorer.exe`) and trackpad-state recovery after
  multitouch, not to unknown rootfs structure.
- New device-proof from `2026-03-16` narrowed container-open failure further:
  the next live blocker was not shell/bootstrap/input, but a theme registry
  write crash in `WineThemeManager.apply()` against the container `user.reg`.
  That guardrail is now patched locally and reinstalled; the next pass should
  verify container open before revisiting desktop cursor tails.
- Local no-`adb` forensic practice is now explicit: use external `Ae.solator`
  logs first, not shell `logcat`. The reliable sources on this phone are
  `logs/forensics/*.jsonl`, runtime stream files, and `fatal_crash_*.txt`
  written by the app itself.
- Fresh local external forensic now narrows the active blocker to the
  `XServer` constructor corridor. The next proof target is no longer generic
  "container does not open", but specifically `libwinlator` preload / root
  drawable / early `XServer` construction with synchronous breadcrumbs and
  a cleaned native link perimeter.
- Fresh new-device ADB forensics supersede that older bootstrap hypothesis:
  the active blocker is now the guest Vulkan wrapper lane, not early
  `XServer` construction. The current causal chain is
  `graphics_driver=wrapper` -> `libvulkan_wrapper.so` ->
  missing `libandroid_shmget` -> `vkCreateInstance: Found no drivers` ->
  `nodrv_CreateWindow` -> self-exit.
- Local source-backed remediation now builds `aero_android_sysvshm` with both
  legacy `shm*` and wrapper-expected `libandroid_shm*` exports, and stages it
  as `libandroid-sysvshm.so` through `ImageFsInstaller`. The remaining proof
  burden is one clean-session container launch on the freshly installed APK.
- Fresh clean-session proof then closed that wrapper ABI hypothesis and exposed
  the next real tail: unix-side Wine ELF payloads still carried donor absolute
  `RUNPATH` entries pointing at
  `/data/data/app.gamenative/files/imagefs/usr/lib`, which lines up with the
  surviving `winex11.drv` / `winewayland.drv` / `nodrv_CreateWindow` chain.
- Runtime remediation is now widened accordingly: `ContentsManager`
  post-install/repair passes sanitize absolute donor `RUNPATH` / `RPATH`
  entries in-place to a local `$ORIGIN` + relative `usr/lib` closure, so both
  fresh installs and already-staged runtime roots are repaired from the app
  side instead of by manual device surgery.
- The next live pass closed the deeper rootfs half of that same contract too:
  `ImageFsInstaller` now sanitizes donor absolute `RUNPATH` / `RPATH` across
  `imagefs/usr/lib`, writes a versioned marker
  `.winlator/.elf_runpath_sanitizer_version=2`, and skips short ASCII
  linker-script placeholders like `librt.so` instead of treating them as ELF
  rewrite failures. Remaining runtime risk is no longer path contamination or
  repeated full-tree rescans; it is the surviving `winex11.drv`
  `PROCESS_ATTACH -> RETURN 0` failure itself.
- Repo process/docs now need one explicit Codex operating contract too:
  approval-gated review mode, build/runbook sync, and forensic process
  alignment are now centralized in `docs/CODEX_OPERATING_CONTRACT.md` and
  mirrored from `AGENTS.md`, not left scattered across prompts.
