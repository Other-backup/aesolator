# Graphics / XServer Binary Census 2026-04-16

Updated: `2026-04-16`

## Problem Statement

The graphics/XServer donor lane cannot be treated as transferred from
Java/UI parity alone.

The real ownership surface includes:
- donor Java/XServer source;
- donor shipped native libraries inside the official Winlator 11 APK;
- hard-coded app-private and Vulkan-loader paths inside those libraries;
- local `Ae.solator` package rename and runtime contract;
- local XServer renderer/compositor seams.

Without a binary census first, any claim of "full transfer" is false.

## Donor Artifacts Inspected

Official donor APK unpack:
- `/data/data/com.termux/files/home/.cache/research/winlator11-apk`

Key binaries:
- `lib/arm64-v8a/libwinlator.so`
- `lib/arm64-v8a/libvortekrenderer.so`
- `lib/arm64-v8a/libvirglrenderer.so`
- `lib/arm64-v8a/libgladiorenderer.so`

Donor source mirror used for semantic mapping:
- `/data/data/com.termux/files/home/.cache/research/winlator-app-tar`

## Build IDs

- `libwinlator.so`: `360094eb3a05d63d12c6262192bbd3fb8516562b`
- `libvortekrenderer.so`: `bba2ada28ab066327da02709c80e684342498d96`
- `libvirglrenderer.so`: `9281afc55abe4072db162c23e473f496fd13965a`
- `libgladiorenderer.so`: `d3e06c31dac122fb82e9d731f7e8c7172822563b`

All four are Android API 24 (`.note.android.ident` shows `r24`), so this lane
is Android-native, not a fake glibc surface.

## Dynamic-Link Surface

`libwinlator.so` depends on:
- `liblog.so`
- `libandroid.so`
- `libjnigraphics.so`
- `libEGL.so`
- `libGLESv2.so`
- `libGLESv3.so`
- `libm.so`
- `libdl.so`
- `libc.so`

`libvortekrenderer.so` depends on:
- `libwinlator.so`
- `liblog.so`
- `libandroid.so`
- `libdl.so`
- `libjnigraphics.so`
- `libEGL.so`
- `libGLESv2.so`
- `libGLESv3.so`
- `libm.so`
- `libc.so`

`libvirglrenderer.so` depends on:
- `libwinlator.so`
- `liblog.so`
- `libandroid.so`
- `libEGL.so`
- `libGLESv2.so`
- `libGLESv3.so`
- `libjnigraphics.so`
- `libm.so`
- `libdl.so`
- `libc.so`

`libgladiorenderer.so` depends on:
- `libwinlator.so`
- `liblog.so`
- `libandroid.so`
- `libEGL.so`
- `libGLESv2.so`
- `libGLESv3.so`
- `libjnigraphics.so`
- `libm.so`
- `libdl.so`
- `libc.so`

Implication:
- the renderer lane is anchored on `libwinlator.so` as a shared native owner;
- Vortek, VirGL, and Gladio are not just resource files or UI aliases.

## Exported JNI Surface

`libvortekrenderer.so` exports donor JNI entrypoints:
- `Java_com_winlator_xenvironment_components_VortekRendererComponent_createVkContext`
- `Java_com_winlator_xenvironment_components_VortekRendererComponent_destroyVkContext`
- `Java_com_winlator_xenvironment_components_VortekRendererComponent_initVulkanWrapper`
- `Java_com_winlator_xenvironment_components_VortekRendererComponent_handleExtraDataRequest`

`libvirglrenderer.so` exports donor JNI entrypoints:
- `Java_com_winlator_xenvironment_components_VirGLRendererComponent_handleNewConnection`
- `Java_com_winlator_xenvironment_components_VirGLRendererComponent_handleRequest`
- `Java_com_winlator_xenvironment_components_VirGLRendererComponent_destroyClient`
- `Java_com_winlator_xenvironment_components_VirGLRendererComponent_destroyRenderer`
- `Java_com_winlator_xenvironment_components_VirGLRendererComponent_getCurrentEGLContextPtr`

`libgladiorenderer.so` exports donor JNI entrypoints:
- `Java_com_winlator_xserver_extensions_GLXExtension_createGLContext`
- `Java_com_winlator_xserver_extensions_GLXExtension_destroyGLContext`
- `Java_com_winlator_xserver_extensions_GLXExtension_createGLXContext`
- `Java_com_winlator_xserver_extensions_GLXExtension_destroyGLXContext`

Implication:
- direct binary reuse is invalid under `com.winlator.cmod` without JNI/package
  adaptation;
- local closure must come from source/native transfer, not from blindly
  shipping donor `.so` files.

## Hex-Anchored Runtime Paths

`libwinlator.so`:
- decimal offset `15316`: `/system/lib64/libvulkan.so`
- decimal offset `15640`: `/data/data/com.winlator/cache/.vk-api-version`

Hex sample around `15640`:

```text
00003d20  74 61 2f 64 61 74 61 2f 63 6f 6d 2e 77 69 6e 6c
00003d30  61 74 6f 72 2f 63 61 63 68 65 2f 2e 76 6b 2d 61
00003d40  70 69 2d 76 65 72 73 69 6f 6e
```

`libvortekrenderer.so`:
- decimal offset `114997`: `/system/lib64/libvulkan.so`
- decimal offset `115227`: `/data/data/com.winlator/cache/vortek/.cache-size`
- decimal offset `115974`: `/data/data/com.winlator/cache/vortek/%lx-%dx%d-%d.imd`
- decimal offset `118745`: `/data/data/com.winlator/cache/vortek`
- decimal offset `120938`: `vortek: unable to open libvulkan: %s`

Hex sample around `115227`:

```text
0001c220  2f 64 61 74 61 2f 64 61 74 61 2f 63 6f 6d 2e 77
0001c230  69 6e 6c 61 74 6f 72 2f 63 61 63 68 65 2f 76 6f
0001c240  72 74 65 6b 2f 2e 63 61 63 68 65 2d 73 69 7a 65
```

Implication:
- donor native code is still package-bound to `com.winlator`;
- package-private cache logic must be recreated in local source/native layers
  instead of treating donor binaries as drop-in assets.

## Source-to-Binary Gap Found

The donor binary and source surface proved two missing local owner classes:

1. `Vortek` driver-config drift requires app restart when the effective
   `adrenotoolsDriver` changes.
2. XServer lacked the donor `XComposite` seam and the supporting local
   offscreen-storage / render-subwindows model needed for compositor-owned
   redirected windows.

## Local Remediation Applied

Transferred into `Ae.solator` source:
- `VortekConfigDialog.isRequireRestart(...)`
- restart prompt wiring in:
  - `ContainerDetailFragment`
  - `ShortcutSettingsDialog`
- new `xserver/extensions/XComposite.java`
- `Drawable.offscreenStorage`
- `Window` tag storage
- `WindowAttributes.renderSubwindows`
- `WindowManager` resize preservation for offscreen drawables
- `GLRenderer` recursion gate on `renderSubwindows`
- `XServer` extension registration for `Composite`

Local strengthening beyond donor:
- parent redirect state is reference-counted through tags, not a fragile single
  boolean toggle, so multiple redirected children do not immediately reopen
  subwindow rendering on the first unredirect.

## Remaining Closure Surface

Still not claimed as fully closed:
- one-batch compile/runtime proof after the full graphics/XServer donor lane is
  finished;
- full donor/native diff over every remaining renderer/XServer seam;
- device/runtime validation across all exposed wrapper routes.

That is why this document is a census and closure input, not a fake "done"
claim.

## Rejected Blind Transfers

Items reviewed but not copied blindly:
- donor `graphics_driver/turnip-26.1.0.tzst`
  Local `Ae.solator` already carries a wider provider set with multiple
  `turnip` and `adrenotools` payloads plus wrapper-routed selection logic.
  Replacing that surface with donor single-version payload would be regression,
  not transfer.
- donor `xserver/Decoration.java`
  No live local callsite currently depends on that enum.
  This stays watchlisted until a real window-decoration contract reappears in
  local renderer/XServer flow.
- donor `renderer/FullscreenTransformation.java`
  Donor fullscreen pointer transform model is absent from the active local
  renderer path.
  It stays watchlisted until a current local fullscreen route proves it is the
  missing owner rather than stale donor baggage.

## Research Used

Open sources:
- https://docs.mesa3d.org/drivers/virgl.html
- https://dev.to/possiblyquestionable/vortek-internals-part-1-command-buffers-3n7h
- https://dev.to/possiblyquestionable/vortek-internals-part-2-driver-specific-workarounds-2d8l
- https://github.com/leegao/vortek-deep-dive
- https://github.com/SEGAINDEED/winlator-bionic-vortek

Local donor/source corpus:
- `/data/data/com.termux/files/home/.cache/research/winlator-app-tar`
- `/data/data/com.termux/files/home/.cache/research/winlator11-apk`

## Rule Consequence

For this repository, a graphics/XServer donor pass is invalid unless it
includes:
- donor source diff;
- shipped binary census;
- JNI/package-path proof;
- XServer/renderer seam audit;
- hex-anchored offsets where native route ownership depends on hard-coded
  paths or loader strings;
- one-batch closure discipline with no intermediate build/install/device loop
  when the lane is declared whole.
