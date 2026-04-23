# Chapter 2 Graphics Stack Roadmap 2026-04-16

## Problem Statement

`Ae.solator` already carries real `Wrapper`, `Vortek`, `VirGL`, `Gladio`, `Turnip` companion routing, local `XServer`, and native renderer bridges. But the graphics stack is still not product-closed because ownership is split across static arrays, hardcoded defaults, asset names, runtime env routing, and partial donor transfer.

Today the stack can present a stronger surface in code than it actually guarantees at runtime. That is the defect class to close.

## Current Status

Phase 1 has already started in-tree:

- bundled wrapper version authority moved away from the old static resource list
- preferred bundled wrapper selection now resolves centrally from `AdrenotoolsManager`
- install/UI/save fallback paths now consume the same central wrapper registry

This roadmap therefore tracks the remaining closure work from that new baseline, not from the previous static-array state.

## External Truth Surface

### Mesa

- Local corpus:
  - `/data/data/com.termux/files/home/.cache/research/mesa3d-org-20260416`
  - `/data/data/com.termux/files/home/.cache/research/mesa3d-org-20260416/MESA3D_CHAPTER2_ACTION_MAP_2026-04-16.md`
- Canonical use in Chapter 2:
  - Android build/control truth
  - Turnip/Freedreno architecture and debug semantics
  - VirGL host/guest contract
  - Zink capability gating and debug/perf policy

### OpenGL

- Local corpus:
  - `/data/data/com.termux/files/home/.cache/research/opengl-org-20260416`
  - `/data/data/com.termux/files/home/.cache/research/opengl-org-20260416/OPENGL_CORPUS_STATUS_2026-04-16.md`
- Current official baseline:
  - OpenGL `4.6`
  - GLSL `4.60`
- Canonical ownership:
  - registry
  - refpages
  - Khronos OpenGL wiki

### Vulkan

- Local corpus:
  - `/data/data/com.termux/files/home/.cache/research/vulkan-org-20260416`
  - `/data/data/com.termux/files/home/.cache/research/vulkan-org-20260416/VULKAN_CORPUS_STATUS_2026-04-16.md`
- Current official baseline:
  - Vulkan `1.4`
  - Vulkan Roadmap `2026`
- Canonical ownership:
  - spec
  - guide
  - validation/layers/profiles/portability

### X11 / X11Libre

- Current upstream donor candidate:
  - repo: `https://github.com/X11Libre/xserver`
  - default branch: `master`
  - last push observed: `2026-04-15T12:04:36Z`
  - latest release observed: `xlibre-xserver-25.1.3`, published `2026-04-07T18:22:07Z`
- Local X11 book corpus action map:
  - `/data/data/com.termux/files/home/aesolator/docs/X11_BOOK_CORPUS_ACTION_MAP_2026-04-16.md`
- What matters for Chapter 2 is not blind replacement of our Java `XServer`, but selective transfer of:
  - extension coverage ideas
  - testing discipline
  - tearing/modesetting/X11 compatibility ideas
  - documentation and build/test discipline

## Current Product Truth

### Already Present

- UI exposes `Wrapper`, `Vortek`, `VirGL`, `Gladio`.
- Runtime already routes:
  - `Wrapper` through Turnip + OpenGL companion logic
  - `Vortek` through local ICD rewrite plus `Gladio`
  - `VirGL` through local virgl socket and renderer component
- `XComposite` owner seam is already present in the local `XServer`.
- `_NET_WM_HWND` / `_WINE_HWND` compatibility surface is already present in window lookup logic.

### Still Structurally Broken

- bundled graphics-driver registry is split across:
  - `DefaultVersion`
  - `arrays.xml`
  - `GeneralComponents`
  - `ImageFsInstaller`
  - `GraphicsDriverConfigDialog`
  - `ContainerDetailFragment`
  - `ShortcutSettingsDialog`
  - `XServerDisplayActivity`
- `Wrapper` defaults still point at stale hardcoded entries instead of the strongest supported bundled package.
- asset extraction and runtime route selection do not share one central model.
- `VirGL`, `Vortek`, `Gladio`, and `Wrapper` versions are still partly hardcoded instead of manifest-driven.
- current `XServer` extension surface is stronger than before, but still not treated as a first-class product frontier with a clear parity target.

## Root Cause

The graphics stack has multiple parallel sources of truth.

That creates five repeating defect families:

1. Registry drift  
   Bundled providers and versions are represented differently in code, resources, and assets.

2. Default-selection drift  
   The default package chosen for a lane is not derived from the same capability model used for installation and runtime.

3. Materialization drift  
   UI-visible options, extracted assets, and runtime env/ICD state can diverge.

4. Route-contract drift  
   `Wrapper`, `Vortek`, `VirGL`, and `Gladio` are partly treated as UX labels and partly as real runtime contracts.

5. X11 surface drift  
   `Ae.solator` uses an embedded X11 stack, but extension coverage, testing, and donor intake are not yet managed as a dedicated subsystem frontier.

## Roadmap

### Phase 0: Corpus and Contract Freeze

- Keep `Mesa`, `OpenGL`, and `Vulkan` corpora on disk with durable status notes.
- Treat `OpenGL`, `Vulkan`, `Mesa`, and `X11Libre` as official donor truth lanes, not ad hoc web reading.
- For every new donor transfer, capture:
  - source URL
  - observed date
  - target owner files
  - whether the transfer is design, code, testing, or docs only

### Phase 1: Central Graphics Registry

Goal: one owner for bundled graphics providers, versions, manifests, and preferred defaults.

Required implementation:

- move bundled-wrapper version ownership out of static arrays and hardcoded defaults
- introduce one central registry fed by actual bundled asset metadata
- derive from that one registry:
  - UI version entries
  - default wrapper package
  - install/extract surface
  - runtime display labels
  - fallback selection

Primary owner files:

- `app/src/main/java/com/winlator/cmod/contents/AdrenotoolsManager.java`
- `app/src/main/java/com/winlator/cmod/contentdialog/GraphicsDriverConfigDialog.java`
- `app/src/main/java/com/winlator/cmod/xenvironment/ImageFsInstaller.java`
- `app/src/main/java/com/winlator/cmod/core/DefaultVersion.java`
- `app/src/main/java/com/winlator/cmod/core/GeneralComponents.java`
- `app/src/main/java/com/winlator/cmod/ContainerDetailFragment.java`
- `app/src/main/java/com/winlator/cmod/contentdialog/ShortcutSettingsDialog.java`
- `app/src/main/res/values/arrays.xml`

Done criterion:

- no stale static wrapper version list remains authoritative
- empty config fallback, asset install, and runtime route all resolve through the same registry

### Phase 2: Wrapper Lane Closure

Goal: `Wrapper` becomes a real product lane, not a compatibility bucket.

Required implementation:

- make default Turnip selection device-aware and manifest-driven
- bind companion OpenGL lane explicitly and centrally
- stop routing on ad hoc `DefaultVersion.WRAPPER_ADRENO`
- make route degradation explicit and forensic, never silent
- formalize `providerLane`, `driverRoute`, `companionProviderLane`, `apiFocus`, and `sourceRepo` as runtime-owned fields

Primary owner files:

- `AdrenotoolsManager.java`
- `GraphicsDrivers.java`
- `Container.java`
- `XServerDisplayActivity.java`

Done criterion:

- `Wrapper` resolves to a specific package and companion lane with explicit forensic state
- stale `System` fallback is only used when the registry/capability model says so

### Phase 3: Native Provider Closure

Goal: `Vortek`, `VirGL`, and `Gladio` become manifest-driven lanes with explicit runtime contracts.

Required implementation:

- replace hardcoded built-in version constants where manifest-owned assets exist
- add provider capability descriptors:
  - Vulkan API max/min
  - OpenGL level
  - required companion lane
  - socket or ICD expectations
  - validation support
- move lane-specific env policy out of scattered conditionals toward normalized route descriptors

Primary owner files:

- `XServerDisplayActivity.java`
- `VortekRendererComponent.java`
- `VirGLRendererComponent.java`
- relevant asset manifests under `app/src/main/assets/graphics_driver`

Done criterion:

- the product no longer assumes one hardcoded built-in version per native lane unless the asset corpus itself has only one valid candidate
- baseline status 2026-04-16:
  `GraphicsDrivers` now owns built-in asset/version/package/extract truth for `Vortek`, `VirGL`, and `Gladio`,
  and `ContainerDetailFragment`, `ShortcutSettingsDialog`, and `XServerDisplayActivity`
  resolve those lanes from live asset filenames instead of split `DefaultVersion` ownership

### Phase 4: X11 Surface Closure

Goal: local `XServer` becomes a managed subsystem with an explicit parity target.

Required implementation:

- define target extension matrix for Chapter 2:
  - current: `MIT-SHM`, `DRI3`, `Present`, `Sync`, `XComposite`, `GLX`
  - next candidates must be prioritized from actual product need, not vanity parity
- review `X11Libre` for:
  - extension semantics
  - testing/build discipline
  - tearing/modesetting policy
  - compatibility/documentation flows
- selectively transfer ideas, never drop in wholesale C server code
- build one extension gap ledger:
  - missing protocol
  - current symptom
  - owner files
  - donor/source reference

Primary owner files:

- `app/src/main/java/com/winlator/cmod/xserver/**`
- `app/src/main/java/com/winlator/cmod/renderer/**`
- `app/src/main/java/com/winlator/cmod/xenvironment/components/XServerComponent.java`

Done criterion:

- `XServer` has an explicit extension roadmap and parity ledger instead of opportunistic donor intake

### Phase 5: Vulkan Control Plane Closure

Goal: Vulkan is managed as a control plane, not as one ICD path.

Required implementation:

- normalize ownership of:
  - `VK_ICD_FILENAMES`
  - `VK_DRIVER_FILES`
  - validation layers
  - API max/min selection
  - portability/profile policy
- align route policy with official Vulkan guide areas:
  - validation
  - synchronization
  - profiles
  - portability
  - mobile/tile-based best practices
- add explicit capability gates for future `Zink` or alternate Vulkan-backed GL lanes

Primary owner files:

- `XServerDisplayActivity.java`
- lane-specific config dialogs
- provider manifests

Done criterion:

- Vulkan route decisions are explainable from explicit provider metadata and official guide/spec constraints

### Phase 6: OpenGL and Mesa Closure

Goal: OpenGL paths stop being second-class fallback logic.

Required implementation:

- separate the meaning of:
  - host/guest virgl route
  - local GL overlay route
  - Gallium bridge route
  - future Zink-on-Vulkan route
- align OpenGL lane policy with:
  - Khronos registry/refpages/wiki
  - Mesa VirGL/Freedreno/Zink docs
- add capability and debug contracts:
  - `GALLIUM_DRIVER`
  - Mesa debug knobs
  - API/feature gating
  - overlay activation state

Primary owner files:

- `XServerDisplayActivity.java`
- `GraphicsDriverConfigDialog.java`
- `VirGLRendererComponent.java`
- future lane descriptors/manifests

Done criterion:

- OpenGL route state is explicit, inspectable, and aligned with official API/Mesa semantics

### Phase 7: Packaging and Build Closure

Goal: packaged assets and live runtime surface are guaranteed to match route policy.

Required implementation:

- tie graphics package extraction to the central registry
- ensure `ImageFsInstaller` does not operate on stale static entry lists
- add package/asset validation:
  - required library present
  - manifest present
  - companion lane present when required
  - route-specific files staged into imagefs/rootfs correctly

Primary owner files:

- `ImageFsInstaller.java`
- contents/manifest helpers
- packaging docs

Done criterion:

- package materialization cannot silently lag behind UI/runtime registry truth

### Phase 8: Device and Product Proof

Goal: closure is proven on the integrated product path, not only in source.

Required implementation after the one-batch donor lane is ready for verification:

- rebuild app/runtime
- install newest APK
- exercise each graphics lane on device:
  - `Wrapper`
  - `Vortek`
  - `VirGL`
  - `Gladio`
- capture proof for:
  - visible route choice in UI
  - extracted payload presence
  - runtime env/ICD state
  - window creation in local `XServer`
  - degraded-route explanation when a lane is unsupported

Done criterion:

- lane visibility, payload presence, and runtime behavior agree on-device

## Priority Order

1. Central graphics registry
2. Wrapper lane closure
3. Native provider manifest closure
4. X11 surface ledger
5. Vulkan control plane normalization
6. OpenGL/Mesa normalization
7. Packaging validation
8. Device proof

This order is mandatory because later stages depend on earlier ownership cleanup.

## Immediate Next Batch

The next engineering batch should implement Phase 1 and the start of Phase 2 together:

- central bundled-driver registry
- dynamic wrapper version enumeration
- preferred bundled Turnip selection
- removal of stale `wrapper_graphics_driver_version_entries` authority
- unified install/UI/runtime fallback path

Do not jump to deeper `Vortek`/`VirGL` polish before this owner class is closed. Otherwise every later route still sits on stale registry foundations.

## Sources

- Mesa main site: `https://mesa3d.org/`
- Mesa docs: `https://docs.mesa3d.org/`
- OpenGL site: `https://www.opengl.org/`
- Khronos OpenGL Registry: `https://www.khronos.org/registry/OpenGL/index_gl.php`
- Khronos OpenGL Wiki: `https://wikis.khronos.org/opengl/`
- Vulkan site: `https://www.vulkan.org/`
- Vulkan docs/spec/guide: `https://docs.vulkan.org/`
- X11Libre xserver: `https://github.com/X11Libre/xserver`
