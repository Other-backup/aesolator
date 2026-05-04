# Second Developer Roadmap

Updated: `2026-05-04`

## Mission

Second autonomous developer lane for `aesolator`:

- tighten app/UI behavior against the active split model,
- keep runtime/forensic contracts visible,
- close remaining `Contents` UX and device-validation gaps,
- report deltas clearly to the first developer.

## Master Engineering Directive

`docs/MASTER_ENGINEERING_DIRECTIVE.md` is mandatory for this roadmap.
Tasks are not closed by diagnosis alone when remediation is possible:
investigate, identify root cause, cover the defect class, apply the systemic
fix, update tests/config/docs/tooling, and verify.
Do not narrow active tails to one file or one visible symptom when the class
spans app/runtime/package/device surfaces.

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

### Active Black Diamond App-Donor Pass: Ae.solator Only

Scope:
- Work is currently limited to `aesolator`.
- `freewine11`, Wine source donors, Proton source donors, and WCP packaging lanes are not part of this app-side pass.
- Donor freshness is measured from Android/app donors that affect Ae.solator behavior directly.

Freshness ledger:
- `GameNative-app` is at `cbea7f75f7bd5b1dd5f665148c91251cf4a89b39`.
- `WinNative-main-git` is at `b4297a39ade7ca46e3505b2c26ceafa5f0f69146`.
- `palazos-winlatorCmod` is at `9a27d00aeaaed884624355539c0d352769d285d9`.
- `EtchDroid` and `libaums` are refreshed as utility evidence only, not runtime-source donors.

Closed in the current app-side batch:
- XInput2 raw input extension from GameNative is integrated into the local X server path.
- Renderer effects no longer recurse through `drawFrame()`; scene rendering, FBO targets, filter ownership, and render-scale effect hooks are split.
- FSR1 EASU/RCAS, scaling mode, source texture filter, and vivid effect classes are present on the Ae.solator renderer surface.
- Prelaunch stale Wine process cleanup now runs as a logged launch stage and uses the local native lifecycle reaper where available.
- WinNative's 2026-04-26 range-button fix is applied as a generalized input-control binding guard.
- X11 pointer-grab confinement is now represented in `GrabManager` and respected by pointer delta injection instead of being left as donor-only ClipCursor behavior.
- Controller hotplug now has soft slot release, physical-device dedupe, and fingerprint/uinput impostor rejection in one input-device class.
- `PatchElf` now routes through the native donor rewrite surface in local `libpatchelf.so`, preserving the local loader while adding real section rewrite ownership.
- Foreign runtime profile parsing now accepts donor schema aliases and preserves bionic-native truth when donor labels also contain generic glibc/rootfs wording.
- Rolling bleeding-edge runtime identity now accepts capability suffixes such as `10.0.99-arm64ec-ntsync`, matching the user forensic miss class.
- WCP/WCP.xz package installation now uses a payload-first model:
  suffix-aware archive probing handles zstd-tar, xz-tar, raw tar, and zip WCP
  variants; profile-less Wine/Proton packages synthesize a safe profile; and
  bionic/glibc runtime ownership is resolved from root layout plus ELF markers
  before install-root naming.
- Content-install logs now include archive format, root shape, runtime
  classifier signals, and installed-profile diagnostics for foreign-device
  package failures.

- Total donor transfer request escalated to black-diamond whole-repo sweep: matrix generator now scans every repo Java/Kotlin class (`434` classes), records local SHA12, and supports donor checkout comparison (`status/path/hash`) for literal class-by-class transfer decisions.
- Donor universe widened to 24 GitHub donors across app/runtime, package/feed, rootfs, graphics, translator, runtime-core, and archaeology lanes via `tools/donor_sources_2026_05_04.json` with tier metadata for anti-bias transfer decisions.
- Donor matrix audit executed: `434 x 24 = 10416` unresolved donor-cells (`pending/review`), so full transfer closure remains open until donor checkouts + per-class patch sweeps are completed.
- Donor source retrieval currently blocked in this environment (`CONNECT tunnel failed, response 403`) during GitHub clone attempts; physical code transfer cannot proceed until donor trees are locally available.

Verification:
- Final unified Gradle gate passed on `2026-04-26`.
- Local debug APK install to `192.168.43.4:39057` passed with `adb install -r`.
- CI/release dispatch is owned by `wcp-runtime-lanes` after the aesolator push.

### Active Closure Slice: Runtime Drawer / Prefix Pack / Route-Aware Profiles

Carry these tails as one closure batch until a fresh live build disproves them:

1. `Debug` / runtime `Logs`
   - stop rendering the runtime log as striped table rows
   - keep `View Log` from the runtime drawer on the exact same visual contract
     as the main forensic viewer:
     same width profile, same compact action row, same live/paused ticker
     rhythm, same console body emphasis
   - latest marked screenshot batch (`2026-03-20 22:16-22:17`) sharpened the
     remaining geometry tail:
     the runtime `Logs` window still read as a different product surface
     because metadata stacked above a detached action row and left a dead band
     in the header, so both viewers now need the same inline
     `metadata-left / actions-right` top contract plus matching outer padding
   - live proof now exists on the rebuilt surface:
     `out/live_verify_20260320_2238_runtime_logs_contract/`
     shows `DESKTOP_DEBUG_LOGS_AUTOOPEN_ARMED`,
     `DESKTOP_DEBUG_LOGS_AUTOOPEN_EXEC`, and a fresh screenshot where the
     runtime drawer `Logs` view matches the main forensic viewer geometry more
     closely while the `LogView` body renders semantic color accents instead of
     the older flat striped table feel
   - preserve smooth streaming under live append so the log window no longer
     contributes to runtime `ANR` risk while the user drags it
   - fresh live proof also exposed a new blocker around that surface:
     `Runtime Profiles` can still occupy the top-most popup path on a fresh
     launch and steal the drawer/log proof flow, so the debug launch contract
     now needs a direct `Logs` auto-open lane next to `Task Manager` and
     `Prefix Pack`
2. `Runtime Profiles`
   - stop exposing a fake generic `64-bit Emulator` selector on the live
     desktop route
   - for `arm64ec` containers, show an explicit `FEX` launch lane with real
     `version + preset` controls
   - for `x86_64` containers, show an explicit `Box64` launch lane with real
     `version + preset` controls
   - keep selector contrast readable and avoid blue-on-blue controls
3. compact runtime geometry
   - keep `Runtime Profiles`, `Screen Effects`, and `Input Controls` in the
     compact runtime-dialog lane instead of forcing tall wide-scroll dialog
     geometry just because they use a `ScrollView`
   - latest marked screenshot batch (`2026-03-20 22:43`) added a drawer-open
     logic tail:
     reopening the runtime drawer could preserve a stale internal scroll offset
     and expose the next session with a clipped half-card at the top edge, so
     the drawer must snap back to scroll-top on every open instead of resuming
     from the previous partial-row position
   - live proof now exists for that drawer-open fix as well:
     `out/live_verify_20260320_2310_drawer_autoopen/drawer.png`
     shows the drawer reopening from the top with the header and first action
     card fully visible instead of resuming from the previous clipped row
   - center the preloader / install-start loading icon and keep the loading
     card visually symmetric
   - latest marked screenshot (`2026-03-20 22:33`) tightened that same tail:
     the detached installer hand-off toast also showed the leading info icon
     drifting inside the card, so the loading/hand-off family now shares a
     single requirement: fixed-width card, symmetric paddings, and a dedicated
     centered icon lane instead of free-floating icon placement
   - live proof now exists for both surfaces:
     `out/full_forensics_20260320_2248_startup_centering/screen_03s.png`
     shows the refreshed `Starting up` card with the icon centered in the
     loading ring, and
     `out/live_verify_20260320_2252_prefix_toast_centering/screen_12s.png`
     captures the detached installer hand-off toast after the new centered
     leading icon lane landed
4. `Task Manager`
   - treat the final Linux process row as a hard functional bug until a fresh
     device screenshot shows it is no longer clipped
   - keep the Linux-first surface stable and avoid reintroducing oversized dead
     tail space while adding bottom slack
5. `Prefix Pack`
   - remove open-jank from lane-card rendering and delayed status hydration
   - keep `Install` honest for GUI-heavy lanes:
     a detached guest installer start must advance lane state beyond `queued`
     instead of triggering self-retry loops
   - keep the user-visible contract explicit:
     `Prepare -> Install -> Clean`, with state/log proof still recorded under
     `AePrefixPack\save_data`
   - latest marked screenshot batch (`2026-03-20 21:54-21:56`) adds three
     fresh geometry tails:
     `Overview` badge overlaps the first summary line,
     the `Lanes` hint burns too much vertical space before the first section,
     and the live lane list still feels heavier than necessary while opening
   - latest manual screenshots (`2026-03-21 11:22-11:24`) keep one logic tail
     explicitly alive:
     `DXSDK_Jun10.exe` launches from the visible `C:\AePrefixPack\cache`,
     shows `Setup is loading installation components`, and then fails on the
     legacy `.NET Framework 2.0 redist` prerequisite with `error 51023`; do
     not mark legacy DX install flow closed until a newer live bundle disproves
     that exact chain
   - the auxiliary `Prefix Pack` surfaces still belong to the same contract:
     `Graphics Diagnostics` and lane `Info` must use the same split-pane,
     inline-header runtime-management geometry as the main toolkit rather than
     falling back to older stacked badge or button-wall layouts
6. runtime log surfaces
   - the console visual language is now correct, but the fresh screenshots
     still show a control row that is too tall and steals useful height from
     the log body in both the main forensic viewer and the runtime drawer log
7. execution discipline
   - keep the roadmap itself updated during the same session as new screenshot
     batches, forensic bundles, marked defects, and crashes arrive
   - do not call the batch closed until the freshest user screenshots no longer
     show the same geometry / contrast / flow defects

### Active Donor Base: Ajay / Prefix Install Flow

- Fresh donor base is now staged locally from the official release:
  `docs/PREFIX_DONOR_SHORTLIST.md`
- Official winner:
  `Ajay Prefix Pro v1.6 Offline`
  with verified archive hash
  `e4a23f89c8cc5944b87d7228d04a820e659b494a7e230498910f2c93a2305aa6`
- Local donor root:
  `/data/data/com.termux/files/home/donors/ajay-prefix/v1.6_offline`
- Reflective donor conclusion:
  Ajay is the strongest donor for offline prefix-local installers, Wine-side
  helper scripts, and GPU/API test coverage, but it is not sufficient as the
  source-of-truth for our stricter `Prefix Pack` state/proof contract
- Active import targets from this donor lane:
  richer graphics diagnostics coverage,
  compact lane grouping,
  explicit `Only Start Menu / Prefix / Both` install mental model,
  and broadened helper-script inventory
- Active non-import rule from this donor lane:
  do not copy Ajay's looser `Start <exe>` installer dispatch or treat its
  `.NET` handling as proof that our `DXSDK Jun10 -> legacy .NET 2.0` blocker
  is solved
- What is already imported from the donor lane into the Android-side
  management contract:
  visible `Prepare -> Install -> State/Logs` staging roots,
  broader offline redistributable coverage,
  `Wine Mono -> XNA` prerequisite logic,
  donor diagnostics surfacing (`DXDiag`, `DXCaps`, `DXCpl`, `GLview`,
  `TestD3D.exe`, `GPUInfo.exe`),
  and stricter state/proof logging around staged launchers
- What is still intentionally not imported:
  Ajay start-menu cloning,
  loose `Start <exe>` fire-and-forget launchers,
  payload ownership that belongs to runtime lanes (`DXVK`, `VKD3D`,
  `dgVoodoo`, `Vulkan SDK`, graphics-driver payloads),
  and the out-of-scope Mono Project Windows installer lane the user rejected

### Active Clean Pass: Prefix Pack / Legacy DX / Freshest-Log Closure

Closed or materially narrowed in this pass:

1. freshest-crash-first discipline is now explicit:
   fatal crash files and the newest runtime bundle are captured before new UI
   poking when the user reports a live crash
2. runtime-drawer polish stays gesture-first and targeted:
   no permanent floating open button, stronger contrast is applied to the
   `Task Manager` and `Prefix Pack` rows instead of covering the renderer
3. `Prefix Pack` now follows a precise
   `Prepare -> Install -> State/Logs` contract instead of an opaque
   `fetcher` mental model
4. Windows-visible installer copies remain first-class:
   `Z:\opt\ae\prefix-pack\cache -> C:\AePrefixPack\cache ->
   C:\AePrefixPack\staging`
5. donor/rootfs diagnostics remain surfaced:
   `DXDiag`, `TestD3D.exe`, `GPUInfo.exe`, `DXCaps`, `DXCpl`, and `GLview`
6. donor utility overlays such as `opt/apps` and `7-Zip` are preserved during
   runtime patching instead of being treated as disposable residue
7. the supply chain is tighter:
   `XNA 3.1`, `XNA 4.0`, `OpenAL 1.1`, `.NET Framework 3.5/4.0/4.8`,
   `PhysX`, `LAVFilters`, and the full `DXSDK_Jun10.exe` lane are staged from
   source-backed upstreams, while
   `dgVoodoo` and `Vulkan SDK` stay on dedicated payload lanes
8. false-ready payload states are no longer acceptable:
   repo cache, device cache, and Windows-visible cache now reject zero-byte
   entries, and device staging uses a large-file-safe temp bridge instead of
   direct `run-as cat` streaming
9. the old `Prefix Pack` launch crash is narrowed to a concrete fixed root
   cause:
   long `WinHandler.exec()` commands are no longer limited to the previous
   64-byte packet assumption
10. `Task Manager` root cause is explicit and closed:
    the process tabs no longer depend on a `wrap_content` parent with a
    weighted zero-height list region, so a live process list is not silently
    collapsed even when forensics already report `windows_visible > 0`
11. `Prefix Pack` now behaves like a compact sectioned loader:
    `Core`, `Managed`, `Legacy APIs`, `Middleware`, and `Diagnostics` lanes are
    grouped with per-lane state instead of one oversized button wall
12. managed-runtime ownership is explicit:
    `Wine Mono` stays the single WineHQ MSI, while a new dual-arch Mono Project
    lane was later removed from the active `Prefix Pack` surface after user
    feedback rejected Mono Project as out-of-scope for this stack
13. `AGENTS.md`, roadmap, and reflective notes are kept in the same closure
    lane so the next pass inherits the install/forensic/runtime contract
14. `Prefix Pack` install-flow is now honest:
    `Install` auto-prepares missing payloads, then dismisses the Android
    overlay before launching the Windows-side installer, while per-lane
    `State` and `Logs` stay directly reachable from the same surface

Remaining live-verify tails after the clean rebuild/install:

- run one fresh managed-runtime prepare/install pass so device cache can move
  from the previous `14/16` state to the new post-`.NET` target and leave the
  managed-runtime plus `.NET Framework` state markers
- capture one fresh post-fix `Prefix Pack` install launched from the current
  dialog surface and confirm the new `r7` state/log markers under
  `AePrefixPack\save_data`
- keep `dgVoodoo` proof explicit for legacy DirectX paths without moving
  `dgVoodoo` itself into `Prefix Pack`

### Fresh Live Proof: Managed Runtime Closure (`2026-03-21 15:44-15:47`)

Fresh bundle:
`out/managed_runtime_closure_20260321_154439/`

Reflective result from the latest clean-session batch:

1. `.NET Framework` lane moved past the old dead launcher path
   - detached guest dispatch is now proven by fresh
     `DETACHED_GUEST_PROGRAM_STARTED/TERMINATED` events and a confirmed retry
     path in `logcat`
   - however the lane still leaves `dotnet_framework.properties` in `queued`
     with no fresh lane log under `AePrefixPack\save_data\logs`
   - active tail:
     the `.NET` lane still needs an honest post-dispatch proof token and state
     transition instead of stopping at "launcher accepted"
2. `legacy_dx_sdk` materially improved
   - the stale `failed exit_code=1023` state is replaced by a fresh
     `interactive` state with dispatch proof:
     `Guest runtime produced a fresh lane log...`
   - the old blocker is therefore no longer Android hand-off failure
   - active tail:
     DXSDK still is not closed until the actual SDK tool proof
     (`DXCapsViewer.exe` / `dxcpl.exe`) is visible in a newer clean pass
3. `xna` closed its previous false-negative state
   - the latest `xna.properties` is now `success`
   - `xna-framework-3.1.log` and `xna-framework-4.0-refresh.log` both exist
     in the fresh run
   - the lane now continues when proof is already visible instead of
     remaining stuck on a noisy MSI return code
4. remaining managed-runtime contradiction
   - despite the stronger lane results, fresh `user.reg` still shows disabled
     managed overrides (`mscoree`, `mscoreei`, `mscorlib`, `mscorwks`)
   - active tail:
     prefix-wide managed runtime contract still needs a persistent repair,
     not just lane-local workarounds
5. screenshot hygiene note
   - the latest automated screens in this proof run were polluted by live user
     foreground activity (`Telegram`, `Termux`), so current closure claims
     must rely on log/state/registry proof first and wait for the next clean
     screenshot batch before calling the visual side closed

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
  - keep the prefix-pack supply chain pinned to audited upstreams, not mirrors,
    and document every source/payload choice explicitly
  - keep prefix-pack manifest fields, repo/rootfs/Windows loaders, start-menu
    entries, and toolkit `VERSION` in lockstep so device-side `imagefs` never
    drifts behind the current APK assets
  - keep the ownership boundary explicit:
    anything that already belongs to dedicated `Contents` / payload lanes
    stays out of `prefix-pack`, even if donor bundles expose it via helper
    scripts or app-store shortcuts
  - keep the latest runtime UX tails explicit:
    `Task Manager` must stay a stable Linux-first table with non-clipping rows,
    while `Prefix Pack` must consume deferred install targets only once and
    prove runtime launch dispatch beyond `scheduled`
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
    just staged locally; `SteamClientComponent` is now attached through
    explicit Steam launch evidence instead of ambient startup, so the
    remaining tail is runtime verification rather than basic wiring
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

13. Prefix-pack runtime toolkit lane
  - keep the extra runtime cache lane reproducible and source-backed
  - stage a rootfs-visible toolkit under `/opt/ae/prefix-pack` instead of
    scattering ad-hoc payloads in random directories
  - treat `abbodi1406/vcredist` as the VC AIO upstream and `TechPowerUp` only
    as a mirror, not a source-of-truth
  - pin `Wine Mono` / `Wine Gecko` to official `dl.winehq.org` release
    directories and keep the exact versions in the catalog
  - keep `DirectX June 2010` cache-aware and explicitly documented as a manual
    Microsoft-page lane until the official direct binary URL is stable enough
    for unattended fetches
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
- Fresh same-device `FreeWine11` desktop-shell RCA found a later black-screen
  bootstrap fault: the app could still emit visual-ready from process proof
  alone while `tracked_window_count=0`, and the direct shell command had
  drifted from canonical `wine explorer /desktop=shell,<geometry> "explorer.exe"`
  to `wine explorer.exe /desktop=...`. The runtime rule is now the canonical
  desktop command plus explicit rejection of process-only readiness until a
  mapped shell window or fallback bridge appears.
- The next verified blocker on that same line is the fallback bridge itself:
  `desktopShellLaunchMode` could flip to `winhandler_shell` while launching
  only bare `wfm.exe`, then only bare `wine winhandler.exe "wfm.exe"`.
  Both reopen `winHandlerReady=false` stalls. The fallback contract is now
  explicit: keep the same desktop host and launch
  `wine explorer /desktop=shell,<geometry> winhandler.exe "wfm.exe"`, while
  collecting runtime logs from the current `Ae.solator/logs` root instead of
  stale `Winlator/logs`-only tooling.
- Fresh device RCA then proved a deeper structural tail: even with the correct
  hosted fallback command, the Android-side `WinHandler` UDP bridge could still
  stay invisible because `start()` swallowed bind/receive failures and could be
  started after guest launch had already begun. The runtime contract now
  requires `WinHandler` to bind before guest submission and to log
  `socket_starting/socket_bound/init_received/socket_failed` for ports
  `7947/7946`.
- The next live pass then proved a second structural tail after the socket fix:
  `WinHandler` bound correctly, but the fallback still launched while
  `wineboot.exe --init` was alive. That stacks a second shell route on top of
  unfinished first-boot init and prevents an honest direct-route settle. The
  contract is now tightened so fallback is blocked while `wineboot` remains
  active, and the preloader watch window is widened for first-boot containers.
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
- Fresh bionic device proof closes the `winex11.drv PROCESS_ATTACH -> RETURN 0`
  blocker too: `BionicProgramLauncherComponent` now keeps the guest/runtime
  `LD_LIBRARY_PATH` head first and appends `imagefs/usr/lib/android-host` as a
  fallback tail, so android-host X11 overlay copies no longer shadow the
  runtime's own `usr/lib` closure.
- The new proof burden is no longer container bootstrap. Fresh build/install on
  `10.0.0.1:40741` emits forensic marker
  `BIONIC_HOST_LIBPATH_ORDER_APPLIED`, shows
  `MODULE_InitDLL(... winex11.drv ...) - RETURN 1`, and keeps
  `wineserver`, `wfm.exe`, `war3.exe`, and `winecfg.exe` alive in the same
  session. Remaining risk has moved to payload-specific warnings and tuning,
  not to X11/container bring-up.
- Fresh `2026-03-18` direct-container proof closes the next control-plane tail
  too: `XServerDisplayActivity` now parses upscaler/framegen settings even
  without a shortcut, and clean-session `UPSCALER_ROUTE_APPLIED` resolves
  `backend=mobfgsr`, `backend_source=container`, `framegen_enabled=1`, and
  `soc_class=adreno-7xx` on `SM8475 / taro` instead of falling back to
  `backend=off` and `adreno-6xx-and-older`.
- `Vulkan SDK` proof is now explicit on the same device pass: `rootfs`
  contains `usr/share/vulkan-sdk/1.4.341.1/arm64/*` plus
  `usr/share/vulkan/icd.d/wrapper_icd.aarch64.json`, and the same launch emits
  `vulkan_sdk_profiles=VulkanSDK-1.4.341.1-arm64-1` in
  `GRAPHICS_ROUTE_APPLIED`.
- The wrapper provenance contract is now stricter than that older `Vulkan SDK`
  proof: `Ae.solator` should treat `wrapper_icd.aarch64.json` as the source of
  runtime wrapper API truth, not `vulkan-sdk` payload metadata. The current
  canonical source-build baseline is official non-main `Mesa staging/26.1`
  plus local `aeso-wrapper-forwardport-mesa26-v1`; older `staging/26.0`
  wrapper archives remain historical evidence, not current source truth.
- The remaining framegen tail is now narrowed, not speculative: the staged
  rootfs currently contains no `mobfgsr` / `dlssg` / `fsr3` payload files, so
  the open gate is provider-side runtime consumption, not app-side env export
  or launch-policy selection.
- Active `2026-03-20` runtime clean-pass tails are now narrower and explicit:
  `Runtime Profiles` still needs live-proof as a route-aware surface where
  `arm64ec` shows only `FEXCore` controls and `x86_64` shows only `Box64`
  controls; runtime `Debug/Logs` must match the main forensic console style
  instead of the older striped table look; `Screen Effects` and sibling drawer
  dialogs still need tighter geometry with smaller profile rows and less dead
  height; `Task Manager` only has one remaining geometry tail, namely the final
  Linux row padding without dead-space overshoot; and `Prefix Pack` must prove
  a one-shot installer hand-off without slow re-open, stale `queued` state, or
  proof retries after a direct GUI installer already appeared.
- Fresh `2026-03-20 20:42-20:45` user screenshots plus today-forensics refine
  that same `Prefix Pack` tail from "installer might not start" to a stricter
  causal chain: GUI installers do appear (`visualcppredist_aio_x86_x64.exe`,
  `vc6redistsetup_enu.exe`, `dxsetup.exe`), but the toolkit still keeps running
  verify/retry candidates because launch proof only trusted files/state and not
  the newly mapped installer window surface. The corrective path is now
  explicit: treat fresh non-shell mapped windows as live proof, stop retrying
  GUI lanes once a valid hand-off exists, and reduce card-build burst work so
  opening the toolkit does not feel like a blocking batch.
- The same fresh screenshot batch also adds a dependency tail, not just a paint
  nit: `dxsetup.exe` currently throws a `.NET Framework Initialization Error`
  (`v4.0.30319`) from the live prefix. `DirectX June 2010`, `XNA`, and related
  managed legacy lanes therefore need a prerequisite-aware flow that redirects
  into the `.NET Framework` lane first instead of launching a doomed installer
  and leaving the user with a false failure.
- The next clean-pass narrowed that dependency work even further: the new
  redirect path itself is correct, but stale `.NET Framework` lane states from
  much older sessions can still look "in flight" and suppress a needed rerun.
  Active remediation therefore includes stale-state expiry for queued /
  interactive prerequisite lanes so a dead old `.NET` attempt cannot block
  `DirectX` forever.
- Fresh live proof on `10.0.0.1:38781` now closes both halves of that same
  installer contract:
  `vcrun_full` no longer enters the old verify/retry ladder after the first
  accepted hand-off, and `directx_jun2010` now reroutes into
  `dotnet_framework` instead of launching a doomed `DXSETUP` path. The latest
  clean bundle shows `PREFIX_PACK_INSTALL_LAUNCH install_target=dotnet_framework`,
  `DETACHED_GUEST_PROGRAM_STARTED`, and then a real
  `XSERVER_APP_WINDOW_MAPPED class_name=ndp48-x86-x64-allos-enu.exe`, with no
  follow-on `PREFIX_PACK_RUNTIME_DISPATCH_RETRY`.
- `Task Manager` is now effectively feature-closed and only retains a tiny live
  geometry guardrail: preserve the final Linux row with enough bottom inset to
  avoid clipping, but do not reintroduce the old dead tail after the last row.
- The roadmap itself is now a live operating surface during active runtime/UI
  work: every fresh marked screenshot batch, forensic bundle, crash, hang, or
  wording correction from the user must be folded into this file before the
  pass is called closed.
- Fresh `2026-03-20 22:33-22:43` marked screenshots add one narrow active tail
  to `Starting up` and one structural tail to `Prefix Pack`: the loading ring
  is still optically right-shifted relative to the `Starting up...` label, and
  grouped lane hand-off still reads like a generic lane replay instead of a
  concrete installer launch. Active remediation is now explicit: shift the
  loader icon lane by the shared title axis, keep the log viewers on one
  console renderer family, and route `Prefix Pack` through staged lane
  ownership so lane scripts, not raw payload bypass, own extraction, GUI spawn,
  logging and proof.
- Fresh `2026-03-21 09:42-09:45` live proof plus the newest marked screenshots
  close the old `Debug/Logs` console-family tail and keep `Task Manager`
  within its final geometry guardrail, but they also expose the last real
  installer blocker more sharply: `.NET Framework` now stages the correct lane
  and logs `PREFIX_PACK_RUNTIME_WINHANDLER_DISPATCHED`, yet the first accepted
  command still reaches no lane bootstrap proof. The causal chain is now
  explicit: GUI lanes were being marked `interactive` too early, synthetic
  dispatch metadata was masquerading as fresh launcher proof, and desktop-shell
  dispatch still preferred the nested `cmd /c call install-<lane>.cmd` form
  before the simpler staged `launch-<lane>.vbs` route. Active remediation is
  therefore to keep GUI lanes in `queued` until a real window / lane-log /
  state proof appears, stop writing synthetic proof into launcher logs, and
  prefer the staged dispatch script ahead of the raw batch launcher when
  building runtime and detached-guest candidate lists.
- Fresh `2026-03-21 09:55-09:57` live proof confirms that this cleanup is now
  honest: `.NET Framework` stays `queued` instead of self-promoting to
  `interactive`, candidate `0` is the shorter staged
  `wscript launch-<lane>.vbs` route, and the toolkit no longer treats
  synthetic dispatch metadata as proof that the installer actually appeared.
  The next remaining runtime gate is responsiveness, not false state: GUI
  lanes should go detached-first with faster verification cadence so a visible
  installer window can appear quickly, and the fallback ladder should no
  longer spend most of its time parked on a dead desktop-shell route.
- The same pass also resolves the fresh Mono confusion from the user report:
  `Mono Project 6.12.0.206` is a separate Windows Mono product and is no longer
  part of the active `Prefix Pack` surface. Keep `Wine Mono` as the Wine-managed
  web/runtime payload and do not surface the separate Mono Project MSI lane
  unless the user explicitly asks for it.
- Fresh `2026-03-21 10:04-10:05` screenshots and the matching `r15` forensic
  bundle turn the `.NET Framework` contradiction into an explicit runtime
  diagnosis instead of a vague installer complaint: the live prefix already
  contains `Microsoft.NET\Framework*\v4.0.30319` CLR files, but the expected
  `.NET v4` registry proof is still missing, so dependent SDK bootstrap apps
  throw `v4.0.30319` initialization errors while the `4.8` GUI can still claim
  the newest runtime is present. Active remediation is therefore not "add more
  Mono", but to treat this as a broken partial CLR state, prefer `.NET 4.0
  Full` as the repair-first lane for `v4.0.30319`-era tools, and keep legacy
  DirectX / XNA lanes redirected until that repair path leaves fresh proof.
- The same forensic pass also exposed a concrete lane-contract bug: when a
  prerequisite redirect jumped from `legacy_dx_sdk` or `xna` into
  `dotnet_framework`, the child lane inherited the parent launcher metadata and
  wrote stale `launcher_file` breadcrumbs. The corrective path is now explicit:
  reset lane context per target before every state transition, route
  prerequisite redirects through `prefix-pack-loader.cmd install <target>`, and
  reject oversized or corrupted state markers instead of trusting them as live
  proof.
- Because those launcher/script fixes landed after the earlier `r15` toolkit
  sync, the active runtime tail also includes a required toolkit version bump.
  Device proof is not complete until the refreshed rootfs toolkit reaches
  `2026.03.21-r16` and the new staged scripts are visible under
  `files/imagefs/opt/ae/prefix-pack/windows`.
- Fresh `r16` live proof then narrowed the remaining blocker one level deeper.
  The redirect and lane-state contract is now honest: `legacy_dx_sdk` reroutes
  into `.NET`, `.NET` keeps its own launcher path, and stale oversized state
  files are rejected. But the actual `.NET 4.0 Full` GUI still leaves no fresh
  installer window proof in this hybrid prefix. The current correction is
  therefore to treat the prefix as a partial donor CLR install: if
  `Framework*\v4.0.30319` files already exist while registry proof does not,
  repair the official `.NET 4` detection keys in place first, then rerun the
  dependent lane before falling back to another GUI installer pass.
- Fresh `2026-03-21 10:54` live registry audit refines that diagnosis further:
  the registry is not globally empty for all prefix-pack components. The live
  prefix already carries concrete `system.reg` footprints for `Visual C++`,
  `XNA 3.1/4.0`, `PhysX`, and `Wine Mono`, while `.NET 4` proof remains the
  outlier. The active registry-repair work therefore targets the real missing
  layer instead of bluntly treating the whole registry as broken:
  `dotnet_framework` now emits a dedicated registry-audit log, checks both
  64-bit and `Wow6432Node` proof keys, repairs `.NET 4` keys in place when CLR
  files already exist, and only then allows dependent lanes such as
  `legacy_dx_sdk` to continue.
- Fresh `2026-03-21 11:18-11:24` screenshots plus the matching forensic/log
  bundle invalidate the earlier assumption that `legacy_dx_sdk` is mainly a
  `.NET 4` problem. The live `explorer` trace shows `DXSDK_Jun10.exe` loading
  `v2.0.50727` CLR pieces, then spawning `dotnetfx2.0.x64.exe`, while the
  screenshot batch surfaces both the old `v4.0.30319` initialization dialog
  and the later `dotnet 2.0` / `51023` prerequisite failures. Active
  remediation is therefore requester-aware and two-layered: fix the broken
  registry-audit quoting so the lane stops logging false `reg query` syntax
  errors, then pivot `legacy_dx_sdk` to the legacy `.NET 2.0/3.5` family
  instead of the generic `.NET 4` redirect. The `dotnet_framework` umbrella
  lane now has to preserve `requested_by` across staged state transitions,
  prefer the legacy repair/install path when launched on behalf of
  `legacy_dx_sdk`, and treat a fresh mapped installer window as real dispatch
  proof instead of leaving the lane stranded in a false `queued` state.
- Fresh donor reflection from the fully unpacked `Ajay Prefix Pro v1.6 Offline`
  base sharpens that remediation again:
  `legacy_dx_sdk` should keep the donor-backed Mono/DLL-override guard, but
  `xna` should no longer be modeled as a generic `.NET 4` lane. Ajay's own XNA
  launchers point at `Wine Mono` first, so the active no-build batch now moves
  `xna` onto a lane-owned `Wine Mono prerequisite -> XNA MSI` path and treats
  the old `legacy_dx_sdk -> dotnet_framework` forensic bundle as historical
  proof of the pre-fix behavior that the next closure cycle must beat.
- Fresh donor-driven UI work on `2026-03-21` narrows the active scope
  explicitly to Android-side management surfaces and freezes start-menu work
  out of the pass. The current no-build batch ports donor geometry into
  `Prefix Pack`, `Runtime Profiles`, `Screen Effects`, and the last-row guard
  of `Task Manager` only.
- `Prefix Pack` is moving away from stacked overview chrome toward a split
  management surface:
  left operational rail for session/flow/path state, right lane workspace with
  slimmer section headers and denser lane cards. Active acceptance criteria are
  fewer overlay badges, less dead top space, and only the primary
  `Prepare / Install / Clean` actions on lane rows.
- `Runtime Profiles` is now explicitly a route-owned surface instead of a
  generic emulator chooser: one overview rail, one active family card, and no
  visual suggestion that `FEX` and `Box64` are simultaneously editable on the
  same live route.
- `Screen Effects` now follows the same split management geometry:
  color adjustments on one side, profile/toggle control on the other. This is
  a usability pass only; no renderer semantics should drift during the same
  slice.
- Empty helper `cmd` windows are now part of the same tail inventory:
  the staged dispatch layer must hide helper consoles and let only the real GUI
  installer own the foreground window. `PhysX` and `GLview` are included in
  that contract rather than being left in a vague `queued/accepted` state.
- The widened management batch now also absorbs adjacent runtime dialogs that
  sit directly behind those surfaces:
  `Input Controls`, preset editors behind `Runtime Profiles`, and
  `Container Storage Info` now move toward the same inline-header /
  split-surface language instead of keeping older overlay-badge cards.
- `Task Manager` remains feature-closed for this pass. Only the safe bottom
  reserve is allowed to move, and only to ensure the final Linux row is never
  clipped while avoiding a new dead tail after the list ends.
- Because the user explicitly requested no intermediate compiles for this donor
  UI pass, proof is deferred until the whole management-surface slice is ready
  for one closure cycle. Do not mix halfway APK/device claims into this batch.
- The user then widened the donor UI pass again, but with a strict guardrail:
  let adjacent management surfaces join the batch if that improves coherence,
  but do not degrade install/state/proof logic while scope expands. That keeps
  start-menu and payload sprawl subordinate to the stronger runtime-management
  contract rather than letting them dilute it.
## 2026-03-21 Managed Runtime Closure Slice

- Fresh screenshot batch `2026-03-21 11:19-11:24` stays in the active tail list as historical control proof for the old DXSDK path:
  - `.NET Framework 4 Setup` claims a newer version is already present.
  - `DXSDK_Jun10.exe` later shows `.NET Framework Initialization Error` for `v4.0.30319`.
- Fresh post-restore live bundles narrowed the real active tails:
  - `live_verify_20260321_152227_legacy_dxsdk_postrestore`
    - launcher/hand-off is now real
    - blank helper `cmd` window is gone
    - DXSDK wizard opens for real
    - remaining blocker is guest-side managed runtime execution, not Android dispatch
  - `live_verify_20260321_152439_xna_postrestore`
    - staged launcher and detached guest path are now real
    - active tail moved to guest-side XNA proof/state handling
- Fresh app-private proof from the live prefix:
  - `dotnet_framework.properties` says `success`, but the live prefix still carried `HKCU\\Software\\Wine\\DllOverrides` entries with `mscoree/mscoreei/mscorlib/mscorwks=disabled`
  - `legacy_dx_sdk.properties` remained `failed` with `exit_code=1023`
  - `xna.properties` remained `failed` even though `xna-framework-3.1.log` and `system.reg` already showed `XNA 3.1` / `XNA 4.0` proof
- Root-cause conclusions for the current closure pass:
  - `Prefix Pack` had a false-positive managed-runtime contract:
    registry/file proof existed, but disabled managed overrides still broke real CLR-backed helpers
  - `DXSDK` no longer needs launcher surgery first; it needs managed-runtime repair in the live prefix
  - `XNA` needs proof-based lane success instead of trusting raw MSI exit codes alone
- Required patch batch for this slice:
  - normalize `.NET InstallRoot`
  - repair disabled managed overrides back to builtin when Wine Mono is already present
  - stop calling `.NET` satisfied when the live prefix still has disabled managed overrides
  - make `legacy_dx_sdk` use an adaptive managed-runtime guard instead of blindly disabling Mono-related overrides
  - make `xna` continue when proof is already visible despite a noisy MSI return code

## 2026-03-21 Frozen Resume Point

- This roadmap is intentionally frozen at the current omega checkpoint.
- Do not reopen the pass from a broad UI or donor scope.
- The next continuation must start from:
  `Prefix Pack -> legacy_dx_sdk -> DXSDK_Jun10.exe`
- Fresh user screenshots already narrowed the blocker:
  - the lane can reach the real DirectX SDK installer window
  - it can reach `Copying Files`
  - it then hangs or re-enters the legacy managed prerequisite path
  - the visible failure family remains `.NET Framework 2.0 redist` / `51023`
- One and only one final omega compile already happened successfully.
  Do not launch another Gradle build as the first resume step.
- The repo already carries a newer unrebuilt lane script in
  `app/src/main/assets/prefixpack/windows/install-directx-sdk-tools.cmd`.
  Because that patch landed after the single final build, the first resume
  action should be a live rootfs sync of that script, not a second compile.
- Exact resume order:
  1. restore working wireless `adb`
  2. sync the patched `install-directx-sdk-tools.cmd` into
     `files/imagefs/opt/ae/prefix-pack/windows/`
  3. rerun only `legacy_dx_sdk`
  4. capture a fresh lane-only forensic bundle
  5. decide from that proof whether the remaining defect is inside the lane
     script, the guest dispatch bridge, or another legacy CLR edge
- Reference hold note:
  [HOLD_2026_03_21_DXSDK_CHECKPOINT.md](/data/data/com.termux/files/home/aesolator/docs/HOLD_2026_03_21_DXSDK_CHECKPOINT.md)

## 2026-03-25 Chapter 2 integrated runtime shift

- The active product frame has changed:
  `Ae.solator` now advances one dedicated runtime line, `FreeWine11`, instead
  of treating generic `Wine/Proton` intake as the main story.
- This shifts the main debug loop from app-only tails toward one integrated
  runtime closure loop:
  `Ae.solator symptom -> WCP layout/provenance -> FreeWine11 native runtime ->
  translator payload proof -> app confirmation`.
- `FreeWine11` compile-side omega closure is already available; the next
  product work is packaging and integrating that runtime as the app's own
  `bionic-only` line with a `FreeWine11` `prefixPack.txz`.
- `FEX` and `wowbox64` remain required launch/runtime dependencies for the
  arm64ec story, but they are no longer allowed to blur root-cause order:
  native bootstrap and prefix faults must be proven at the FreeWine layer
  before blaming translators.
- The shared contract for this phase is now explicit in:
  `docs/CHAPTER2_FREEWINE_AESOLATOR_CONTRACT.md`.
- Active execution lane:
  integrate the packaged `FreeWine11` WCP into `Ae.solator`, then continue the
  next dense runtime debug passes from the app surface rather than from
  disconnected repo-local assumptions.

## 2026-03-26 Container 2 FreeWine11 forensic closure

- Fresh direct-route `adb` forensics for `Container 2` on
  `FreeWine11 11.4-arm64ec-1` are now clean:
  `issue_count=0`, `wait_status intent=1 submit=1 terminal=0`,
  `wine_process_present=1`.
- The critical runtime/package fix behind that proof is now explicit:
  preserve the ABI subtree `arm64-v8a/*` inside the WCP and expose
  compatibility symlinks instead of flattening the runtime root.
- `Ae.solator` now emits trace-aware `ROUTE_INTENT_RECEIVED` and
  `LAUNCH_EXEC_SUBMIT` markers for direct forensic launches, so host tooling no
  longer guesses the launch edge from side effects.
- The forensic parser no longer escalates a missing terminal marker to a
  bootstrap failure when desktop-shell runtime evidence already proves the
  session is alive.
- Wrapper-embedded Vulkan forensics now keep requested/applied layer state
  visible without treating missing validation/api_dump layers as active
  warnings in this product line.
- Same-device `adb` screenshot/UI capture still tends to foreground
  `com.termux`; treat that as a capture limitation, not as a runtime failure
  in `Ae.solator`.

## 2026-03-27 Whole-frontier hardening

- The integrated `Ae.solator -> WCP -> FreeWine11` debug loop is now locked to
  whole-frontier operation by rule:
  structural tails must open with freshest full-harvest proof and whole-tree
  logic reflection, not repo-local or one-module retries.
- Integration-first development is now explicit, not implied:
  design and bug-fix passes must start from the live integrated product path,
  and a pass is incomplete if it proves only the app symptom or only one repo
  in isolation.
- If the same runtime/forensic visibility gap appears twice, the next pass
  must add durable instrumentation or helper tooling instead of repeating a
  manual `adb` read.
- Any cross-cutting runtime/source rule learned during these passes must be
  written back into the shared contracts in the same batch.

## 2026-03-27 Reverse-engineering skill intake

- App/runtime forensic work now has a default reverse-engineering spine:
  `reverse-engineering`,
  `rev-symbol`,
  `rev-struct`,
  `radare2-hatchery`,
  `cantordust-viz`.
- `P4nda0s/reverse-skills` was accepted only as a narrow donor for
  `IDA-NO-MCP` export analysis.
- `plurigrid/asi` was accepted only as a curated reverse-engineering donor;
  the remaining skill zoo was explicitly rejected as default live workflow.
- Full chosen-vs-rejected reasoning lives in
  `/data/data/com.termux/files/home/.codex/REVERSE_ENGINEERING_SELECTION.md`.
- This widened into a broader rule:
  future skill intake must cover the whole development/debug lifecycle, not
  only reverse engineering or the current tail.
  The live curated stack now lives in
  `/data/data/com.termux/files/home/.codex/CURATED_SKILL_STACK.md`.

## 2026-03-27 Whole-lotta lower-layer closure rule

- The Chapter 2 loop is now explicitly stricter than the minimum app/runtime/
  source triad.
- If a structural `FreeWine11` class reaches lower static libs, helper
  objects, provider archives, direct-linked owners, parser classes, generated
  artifacts, package/runtime validators, or stale integration bridges, the
  entire blast radius must move in one batch before the app-side black-screen
  class is considered honestly closed.

## 2026-03-29 Android build reproducibility closure

- The root `Ae.solator` Gradle lane now has a stricter contract:
  repo-root `./gradlew` is authoritative, nested `app/` Gradle entrypoints
  are not.
- `preBuild` no longer owns donor rootfs download/mutation. It now verifies
  bundled imagefs/runtime assets and generated JNI state instead of mutating
  `src/main/assets`.
- NDK runtime resolution moved away from eager configuration-time file probes
  toward task-time resolution, and the hard-coded `linux-x86_64` prebuilt
  assumption is no longer embedded directly in the main DSL path.
- Remaining build-repro tail is no longer "why is Gradle caching weirdly?".
  It is the narrower follow-up class:
  final deterministic root lane plus cleanup of remaining Gradle 9
  deprecations.

## 2026-04-25 Black Diamond donor + log frontier

- Fresh active evidence set is now explicit and should stay the source of
  truth for this batch:
  `/storage/emulated/0/Download/forensics_2026-04-25_12-51-00.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_13-54-04.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_14-13-50.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_14-26-21.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_16-11-13.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_16-44-11.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_16-58-16.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_17-27-53.zip`,
  `/storage/emulated/0/Download/forensics_2026-04-25_17-46-12.zip`,
  `/storage/emulated/0/Download/logs.zip`,
  and
  `/storage/emulated/0/Winlator/forensics/exports/forensics_2026-04-25_18-38-57.jsonl`.
- Closed in source during this pass:
  `ContentProfile` / `ContentProfileIdentity` / `ContentsManager` now treat
  runtime identity as a payload-compatible contract rather than a brittle
  label match, which directly targets
  `Broken install: Install directory missing` and
  `Profile cannot be recognized` classes from the fresh user reports.
- `ContentsFragment` now carries scoped remote-profile caches and refresh
  generation guards, so stale feed responses cannot overwrite a newer active
  scope and make artifact lists disappear from `Contents`.
- `ImageFsInstaller` now treats the GameNative rootfs as a universal donor
  base when the glibc/bionic archive payload is byte-identical, stages
  `libc++_shared.so` into both guest and Android-host library closures, and
  sanitizes AppleDouble / `.DS_Store` Vulkan manifest residue while emitting
  `VULKAN_ROOTFS_RUNTIME_CLOSURE`.
- `GuestProgramLauncherComponent` is now the central owner of the direct
  ARM64EC Android-host library-path policy. `GlibcProgramLauncherComponent`
  and `BionicProgramLauncherComponent` both route through the same
  runtime-first / host-second / system-tail `LD_LIBRARY_PATH` sanitizer for
  direct ARM64EC launch: runtime `lib*/wine/unix` segments are preserved, but
  rootfs `usr/lib*` is stripped before Android-host and `/system` / `/apex`
  paths are appended, which directly targets the fresh
  `bad ELF magic` loader class.
- `XServerDisplayActivity` now treats wrapper ICD routing as an Android-host
  contract instead of a rootfs guess:
  the app build embeds `libvulkan_wrapper.so` into `lib/arm64-v8a/`,
  launch-time ICD resolution rewrites the wrapper manifest toward the
  APK-native library path, emits
  `WRAPPER_ICD_ANDROID_HOST_REWRITE_APPLIED`, and reports
  `AERO_VULKAN_RUNTIME_SOURCE=wrapper-host-native` instead of pretending a
  rootfs wrapper path is still authoritative.
- `XServerDisplayActivity.applyGraphicsDriverPackages()` now keeps
  `active_provider_*` telemetry coherent when the selected route falls back to
  `System` or another selected package without a separate `activeInfo`
  object, so `Contents` / route forensics stop collapsing that owner-class
  into empty provider-lane strings.
- `GuestProgramLauncherComponent.resolveInstalledContentProfile()` now routes
  translator payload lookup through `findInstalledProfileByVersion()` before
  the older exact-name path, so installed alias-compatible `FEXCore` /
  `WOWBox64` / `Box64` packages stop surfacing as
  `payload present but profile_found=false`.
- That launcher-owner refactor was verified, not just written:
  `./gradlew :app:testDebugUnitTest --tests com.winlator.cmod.core.AndroidBionicHostLdPathHelperTest --tests com.winlator.cmod.core.VulkanIcdManifestHelperTest --tests com.winlator.cmod.core.WineInfoTest :app:assembleDebug --stacktrace --no-build-cache`,
  APK listing proof
  `app/build/outputs/apk/debug/app-debug.apk -> lib/arm64-v8a/libvulkan_wrapper.so`,
  and
  the previous
  `./gradlew :app:compileDebugJavaWithJavac --stacktrace --no-build-cache`
  pass all succeeded on `2026-04-25`.

### Active log owner-classes after the current source pass

1. `runtime identity / install-root drift`
   - Source-side resolver closure is now landed across
     `ContentProfile` / `ContentProfileIdentity` / `ContentsManager` /
     `WineInfo`.
   - Older bundles still preserve the pre-fix symptom, so the remaining work
     is not more speculative source churn; it is fresh post-fix device proof.
2. `bionic runtime loader purity`
   - Fresh external logs captured
     `CANNOT LINK EXECUTABLE ... libc.so has bad ELF magic: 2f2a2047`
     on a `runtime-bionic-proton-*` path.
   - Central launcher precedence is now corrected in source; the remaining
     gate is fresh runtime proof from a post-fix build/install.
3. `wrapper payload freshness`
   - Pre-fix bundles showed
     `libvulkan_wrapper.so -> libc++_shared.so not found`,
     invalid `._VkLayer_*` JSON residue, and `vkCreateInstance: Found no drivers`.
   - Source-side closure for that class is now in tree:
     APK-native wrapper payload is packaged and ICD routing is rewritten to the
     host-native library. The remaining burden is fresh-device proof from a
     post-fix install, not more speculative edits against stale bundles.
4. `graphics provider coherence`
   - Several bundles showed `graphics_driver=wrapper` with
     `active_provider_lane=""` and `opengl_overlay_active=0` even when the
     selected driver entry was a Turnip package.
   - Empty active-provider telemetry is now narrowed in source by falling back
     to the selected route object when a separate `activeInfo` was absent.
     The remaining audit is fresh proof across `Contents`, rootfs overlays,
     and route application.
5. `translator dependency surfacing`
   - `WOWBOX64_PAYLOAD_REFRESH` and `FEXCORE_PAYLOAD_REFRESH` frequently report
     `payload_missing=true`; some cases legitimately degrade to embedded
     payloads and some are real missing dependencies.
   - Alias-compatible installed payload lookup is now narrowed in source, so
     one false-negative class is removed.
   - The remaining fix is stricter classification between
     `payload genuinely missing` and `payload intentionally degraded to
     embedded fallback`, with equally explicit forensics for both branches.

### Donor execution order for the next Black Diamond passes

1. `AndreVto/proton-wine`
   - target: Proton-11 runtime metadata, prefix-pack contract, Android-facing
     patches, build scripts, launcher/runtime assumptions, and any stronger
     ARM64EC bootstrap logic.
2. `MaxsTechReview/WinNative`
   - target: runtime/xserver integration, wrapper/native routing, broader
     forensics, and any stronger Android-host/X11 closure.
3. `utkarshdalal/GameNative` re-open under the current Chapter 2 contract
   - target: rootfs/runtime/layout/public-release parity rather than the older
     soft-handoff gate snapshot.
4. `palazos/winlatorCmod`
   - target: controller/gamepad ownership, app management surfaces, and any
     donor-side runtime/UI contracts that still beat local behavior.

### Execution rule for this frontier

- Donor work and log work are now one coupled roadmap, not separate queues.
- Each donor pass must consume the freshest reflected log owner-classes first,
  then apply donor logic against those live classes, not against an old paper
  donor matrix.
- No donor prestige bias:
  stronger logic is accepted from any donor only when it closes a live owner
  class or materially strengthens the product contract.

### 2026-04-25 donor batch absorbed into source

1. `palazos/winlatorCmod` -> `controller contamination`
   - centralized controller classification now rejects fingerprint/uinput false
     positives instead of letting them poison player slot assignment.
   - donor-origin `uinput-fpc` exclusion was widened for the live HyperOS/MIUI
     class to include `uinput-xiaomi`.
   - app-side forensics now records rejected controller-like nodes with
     descriptor/vendor/product/source mask and rejection reason.
2. `GameNative + WinNative` -> `controller signature + install/runtime forensic broadening`
   - raw source-mask controller detection was widened with explicit
     gamepad-key / joystick-axis signals so real pads survive while
     fingerprint sensors stop masquerading as player-one devices.
   - runtime/content diagnostics now emit one centralized install-state payload:
     expected install root, resolved install root, runtime root,
     `profile.json` presence, payload presence, alias-resolution state, and
     broken-reason truth.
3. `AndreVto/proton-wine` -> `preserved closure evidence`
   - current source tree already carries the Android/X11/sysvshm contract
     surfaced from the Proton-11 donor line:
     `libandroid-sysvshm`, `_NET_WM_PID`, `_NET_WM_HWND`,
     `WINE_X11FORCEGLX`, and the X11-first desktop route stay source-owned.
   - this batch therefore widened app/runtime forensics around that existing
     closure instead of replaying stale source drift by hand.

### 2026-04-26 native C/C++ donor frontier closure

- `MaxsTechReview/WinNative` app-native transferable code is now absorbed or
  explicitly resolved:
  - process lifecycle is source-owned by `libwinlator.so` through
    `process_lifecycle.c` and `ProcessHelper`;
  - native XZ is source-owned by `libaero_native_xz.so` through
    `NativeXzInputStream`, and file-backed `.txz/.wcp.xz` extraction now uses
    the donor decoder before falling back to the existing Java decoder;
  - Vulkan OEM ICD dependency preload is merged into the local Adrenotools
    route instead of replacing it;
  - fake input / evshim remain owned by local `aero_*` libraries.
- `GameNative` native leftovers are resolved:
  - `xconnectorpatch` is merged into `xconnector_epoll.c`;
  - split GPU/Vulkan extras are weaker than local integrated owners and stay
    `preserve-local`;
  - `arraytools.c` is not imported into the APK because it has no app caller
    and encodes container ownership as implicit frees.
- `palazos/winlatorCmod` native leftovers are resolved:
  - `fakeinput.cpp` is merged through local `aero_fakeinput`;
  - `winhandler.c` is a Windows guest-runtime candidate, not an Android NDK
    library, and remains assigned to the FreeWine/prefix owner lane.
- Verification:
  - native CMake builds `libaero_native_xz.so`;
  - targeted unit tests plus `:app:assembleDebug` are green;
  - APK proof confirms the native runtime libraries are packaged.

### 2026-05-01 browser migration + context restoration baseline

- Source continuity is now restored from local split archives:
  `backup.zip.001..003` and `rollout.zip.001..011` were reassembled and
  unpacked into `.ingest/backup` and `.ingest/rollout` for deterministic
  replay of rules, history, and operating context.
- Build/release source policy is reaffirmed for the new lane:
  fresh runtime builds are consumed from `wcp-runtime-lanes` GitHub releases
  rather than local CLI-era assumptions.
- Device proof policy is updated for the current workflow:
  autonomous install + user-sent forensic bundles without `adb` is now the
  default proof intake until explicit `adb` restoration.
- Immediate next lane:
  wait for the next fresh forensic package, then run one strict closure loop:
  forensic correlation -> code-path audit -> source remediation -> docs sync.

## 2026-05-04 — Winlator-Ludashi LSFG/Vulkan transfer hardening lane

- Added Black Diamond transfer staging artifacts for donor branch `lsfg-vk-color-fix`.
- New matrix: `docs/LUDASHI_TRANSFER_MATRIX_2026-05-04.md` with per-commit decision classes (`import now/adapt later/hold/reject`), revert safety policy, and rollback gates.
- New canonical schema: `docs/LSFG_RUNTIME_CONTRACT.md` to unify global/container/shortcut/launcher LSFG fields and forensic snapshot contract.
- New renderer guard audit: `docs/LSFG_VULKAN_COLOR_ROUTE_AUDIT.md` to force feature-flagged rollout and fallback-first behavior for color-format path changes.
- Next closure step: replace `TBD-*` entries in matrix with exact SHA rows from branch-dump evidence and then execute prioritized `import now` tasks.

- 2026-05-04 LSFG-only framegen lane started: backend identity switched from mobfgsr to lsfg across UI/profile/container/launch env surfaces; legacy compatibility validation pending.
- 2026-05-04 LSFG migration hardening: enabled legacy read-compat (`mobfgsr` -> `lsfg`) and runtime mirror export (`AERO_MOBFGSR_*` mirrors from `AERO_LSFG_*`) with deprecation forensic flag.
- 2026-05-04 UI parity fix: upscaler backend/fg-output entry lists now expose `LSFG/lsfg` instead of legacy MobFGSR labels to match canonical backend identity.

### 2026-05-04 OMEGA single-batch closure pass (LSFG canonicalization)

- Completed end-to-end LSFG contract closure across app-owned lanes in one batch:
  UI (`AdrenotoolsFragment`, `ContainerDetailFragment`, `ShortcutSettingsDialog`, `arrays.xml`),
  storage normalization (`UpscalerProfileStore`), launch env/export and forensic parity
  (`XServerDisplayActivity`, `RuntimeSignalContract`), plus contract docs/matrix sync.
- Decision ledger status:
  - `import-policy`: keep `lsfg` as canonical backend/output identity for write paths.
  - `adapt`: legacy read aliases (`mobfgsr`) normalize to canonical `lsfg`.
  - `adapt`: legacy env mirrors (`AERO_MOBFGSR_*`) exported from canonical `AERO_LSFG_*`.
  - `hold`: donor Vulkan/native swapchain binary replacements remain feature-flag-gated and not blindly imported.
- Verification evidence in this pass:
  - static sweep confirms canonical writes and compatibility reads remain in place;
  - launch env lane confirms canonical + mirror parity with deprecation marker support;
  - forensic lane confirms `LSFG_CONFIG_EFFECTIVE` payload includes `deprecated_alias_used`.
- Residual risk: runtime-package-side consumers outside app lane may still prefer legacy keys;
  mitigated by mirror export plus deprecation observability until retirement window is scheduled.

- 2026-05-04: User requested literal transfer of all 10416 unresolved donor cells in one pass. Added deterministic execution plan doc (`docs/DONOR_TRANSFER_EXECUTION_PLAN_2026-05-04.md`) to formalize batch-wise closure loop and clarify that 10416 is matrix unresolved surface, not directly claimable improvements.

- 2026-05-04: Executed one-batch A/B/C/D transfer start by materializing all 24 donors; all clone attempts failed with `CONNECT tunnel failed, response 403`, so class-level transfer remains blocked by network egress policy. See `docs/DONOR_BATCH_ABCD_EXECUTION_2026-05-04.md`.

- 2026-05-04: Switched to no-clone donor transfer fallback (RAW online file inspection) per user instruction; executed first A-lane code transfer in `FileUtils` null-safe string reads + URI line-break preservation.
