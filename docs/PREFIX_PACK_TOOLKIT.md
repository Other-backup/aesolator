# Prefix Pack Toolkit

Updated: `2026-03-21`

## Goal

Keep the extra Windows runtime lane reproducible and source-backed instead of
depending on opaque mirrors or one-off manual cache drops.

The current toolkit stages a rootfs-visible pack under:

- `/opt/ae/prefix-pack`
- `Z:\opt\ae\prefix-pack`

It covers:

- `Full VC / VCRun Stack` from `abbodi1406/vcredist` release `v0.103.0`
  plus the archived `VC6RedistSetup_enu.EXE` bootstrap for `vcrun6`
- `Wine Mono 11.0.0`
- `Wine Gecko 2.47.4` (`x86` + `x86_64`)
- `Microsoft .NET Framework 3.5 SP1` + `4.0 Full` + `4.8`
- `DirectX June 2010` from the official Microsoft Download Center
- `XNA Framework 3.1` + `4.0 Refresh`
- `OpenAL 1.1`
- `PhysX System Software` + `PhysX Legacy`
- `LAVFilters`
- `DirectX SDK June 2010` tools lane
- `GLview`

The user-facing contract is explicit:

- `Prepare` verifies or downloads payloads into the staged rootfs cache and
  mirrors them into the visible Windows-side cache.
- `Install` runs the Windows-side installer or setup logic into the current
  prefix.
- `Install` auto-prepares missing payloads first, writes a scheduled state
  marker, stages a visible launcher under
  `C:\AePrefixPack\staging\<lane>\install-<lane>.cmd`, and only then dismisses
  the Android overlay before handing off to the Windows-side installer so
  GUI-driven setups can stay visible and be retried manually.
- `Launch` reruns that staged launcher directly from Android. It is not just an
  Explorer shortcut.
- Windows-side runnable installers stay visible under:
  `C:\AePrefixPack\cache`
- The stage root is also intentionally visible:
  `C:\AePrefixPack\staging`
- `State` and logs live under the dedicated prefix save root so the Android UI
  can show the last known result without guesswork.
- Zero-byte or partial files do not count as prepared payloads. Repo cache,
  rootfs cache, and the visible Windows-side cache all need size-valid proof
  before closure.
- `Prefix Pack` is not where `DXVK`, `VKD3D`, `dgVoodoo`, `Vulkan SDK`, or
  graphics-driver payloads belong. Those remain dedicated payload lanes.
- `Prefix Pack` should surface existing graphics diagnostics already present in
  the rootfs/prefix such as `DXDiag`, `TestD3D.exe`, `GPUInfo.exe`, and
  installed `GLview` instead of hiding them behind ad-hoc desktop links.

## Source Policy

- `abbodi1406/vcredist` is the audited upstream for the VC AIO lane.
- `TechPowerUp` is treated only as a mirror/distribution page, not as the
  source-of-truth for engineering or manifest pinning.
- `vcrun6` is pinned to the archived `ftp.microsoft.com` redist mirror because
  the older direct Microsoft CDN path is no longer live.
- `Wine Mono` and `Wine Gecko` come from official `dl.winehq.org` release
  directories.
- `.NET Framework 3.5 SP1`, `4.0 Full`, and `4.8` come from official
  Microsoft `dotnet.microsoft.com` source pages plus official Microsoft
  download endpoints.
- `DirectX June 2010` is pinned to the official Microsoft binary URL and keeps
  the public Microsoft details page alongside it as provenance.
- Every catalog row must carry both:
  - a concrete download URL when unattended fetch is supported
  - a human-readable source page URL for provenance and manual fallback
- When a stable digest is available, the catalog should also carry a pinned
  `sha256` so the repo helper, rootfs helper, and Android UI all verify the
  same cached payload.
- Mirror pages are not enough on their own. The catalog contract is:
  `download_url + source_page_url + source_label + install_cmd (+ sha256 when available)`.

## Loader Contract

The toolkit is intentionally split into three aligned backend surfaces:

- repo-side cache helper:
  `tools/prefix-pack/fetch-cache.sh`
- rootfs-side shell loader:
  `/opt/ae/prefix-pack/bin/prefixpack-loader.sh`
- Windows-side loader:
  `Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd`

These three surfaces must stay in sync. If a new payload is added, update all
three, plus the Android toolkit UI and this runbook in the same pass.

The primary user-facing entrypoint is the Android `Prefix Pack Toolkit`
dialog inside the runtime drawer. The Windows-side loader remains backend
plumbing rather than the default way the user should discover the feature.

The Windows-side helper follows an Ajay-style state discipline:

- installer cache:
  `C:\AePrefixPack\cache`
- staging root:
  `C:\AePrefixPack\staging`
- per-lane staged launcher:
  `C:\AePrefixPack\staging\<lane>\install-<lane>.cmd`

- save root:
  `C:\users\xuser\Documents\AePrefixPack\save_data`
- log root:
  `C:\users\xuser\Documents\AePrefixPack\save_data\logs`
- state root:
  `C:\users\xuser\Documents\AePrefixPack\save_data\state`

Toolkit asset changes must also bump:

- `app/src/main/assets/prefixpack/VERSION`

Otherwise a live device can keep a stale rootfs copy even after APK reinstall.

## Scope Boundary

This toolkit is intentionally not a second payload/package manager.

Keep these families out of `prefix-pack` and in their dedicated app-side
package lanes:

- `DXVK`
- `VKD3D`
- `DgVoodoo`
- `VulkanSDK`
- graphics-driver payloads
- runtime-wrapper payloads and emulator DLL bundles

Ajay-style suites may expose convenience launchers around these packages, but
in `Ae.solator` they stay as `Contents` payloads so versioning, provenance,
install paths, and runtime activation remain under one owner path.

The current Ajay-derived audit is recorded in:

- `docs/AJAY_PREFIX_COMPONENT_AUDIT.md`

## Repo Tools

Fetch the downloadable cache set into the repo-local output dir:

```sh
sh tools/prefix-pack/fetch-cache.sh
```

Show repo-side cache status:

```sh
sh tools/prefix-pack/fetch-cache.sh status
```

Show exact links/install bindings for one or more entries:

```sh
sh tools/prefix-pack/fetch-cache.sh show vcpp_aio
sh tools/prefix-pack/fetch-cache.sh show vcrun6sp6
sh tools/prefix-pack/fetch-cache.sh show directx_jun2010
```

Run deterministic source-page recon before pinning or replacing a web-backed
catalog row:

```sh
sh tools/prefix-pack/fetch-cache.sh recon directx_jun2010
```

The recon output lives under:

- `out/prefix-pack-recon/<catalog_id>/report.md`
- `out/prefix-pack-recon/<catalog_id>/recommendations.md`
- `out/prefix-pack-recon/<catalog_id>/surface.json`
- `out/prefix-pack-recon/<catalog_id>/records.tsv`
- `out/prefix-pack-recon/<catalog_id>/errors.json`

This is the bionic-safe internal replacement for using `Mapr` directly in this
lane:

- keep the deterministic source-intake ideas
- do not vendor or redistribute `Mapr` code into this repo
- keep provenance, auth/captcha/challenge markers, and same-origin fetched
  artifacts visible before a catalog pin lands

Fetch only one entry:

```sh
sh tools/prefix-pack/fetch-cache.sh vcpp_aio
```

Build an offline overlay archive from the fetched cache:

```sh
sh tools/prefix-pack/build-offline-overlay.sh
```

Stage the cached payloads directly into a live device rootfs cache:

```sh
sh tools/prefix-pack/device-stage-cache.sh stage 10.0.0.1:40741
```

The device staging helper now uses a large-file-safe temp bridge under
`/data/local/tmp/<package>-prefix-pack-stage` before the final `run-as cp`
into app-private `imagefs`, instead of streaming file contents directly into
`run-as cat`.

The default outputs are:

- cache dir:
  `out/prefix-pack-cache`
- overlay archive:
  `out/prefix-pack-offline-overlay.tzst`

The device staging helper writes into:

- `files/imagefs/opt/ae/prefix-pack/cache`
- `Z:\opt\ae\prefix-pack\cache`

## On-Device Rootfs State

The app now ensures the toolkit exists inside the live `imagefs` without a full
rootfs reinstall. The runtime marker is:

- `files/imagefs/opt/ae/prefix-pack/VERSION`

The rootfs helper script is:

- `files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh`
- `files/imagefs/opt/ae/prefix-pack/bin/prefixpack-prefetch.sh`

Examples through `run-as`:

```sh
adb shell run-as com.winlator.cmod \
  sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh list

adb shell run-as com.winlator.cmod \
  sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh show directx_jun2010

adb shell run-as com.winlator.cmod \
  sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh status

adb shell run-as com.winlator.cmod \
  sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-prefetch.sh vcpp_aio

adb shell run-as com.winlator.cmod \
  sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-prefetch.sh vcrun6sp6
```

Inspect the staged device-side cache:

```sh
sh tools/prefix-pack/device-stage-cache.sh status 10.0.0.1:40741
```

## Windows-Side Prefix Helpers

The rootfs still carries backend helper scripts:

- `prefix-pack-loader.cmd`
- `prefix-pack-status.cmd`
- `install-default-runtime-pack.cmd`
- `install-vcrun-full.cmd`
- `install-vcpp-aio.cmd`
- `install-vcrun6.cmd`
- `install-wine-web-stack.cmd`
- `install-dotnet-framework.cmd`
- `install-directx-jun2010.cmd`
- `install-directx-sdk-tools.cmd`
- `install-xna-framework.cmd`
- `install-openal.cmd`
- `install-physx-runtime.cmd`
- `install-lavfilters.cmd`
- `install-glview.cmd`

These scripts read the staged cache under:

- `Z:\opt\ae\prefix-pack\cache`
- `C:\AePrefixPack\cache`

They also keep dedicated state under:

- `C:\users\xuser\Documents\AePrefixPack\save_data`

## Managed Runtime Coverage

- `Wine Mono` is currently the single upstream WineHQ `x86` MSI.
- `Wine Gecko` is published as separate `x86` and `x86_64` MSIs.
- `XNA` follows donor-backed `Wine Mono first` logic.
  Do not treat it as a generic `.NET 4` redirect when the real missing layer is
  the Wine-managed runtime already expected by classic XNA setups.
- `.NET Framework` remains its own explicit lane for classic managed apps and
  installers that are not satisfied by Wine Mono.
- `C:\users\xuser\Documents\AePrefixPack\save_data\logs`
- `C:\users\xuser\Documents\AePrefixPack\save_data\state`

Interactive installers are allowed, but the Android UI must say so and must
still point to the resulting log/state roots. Silent install assumptions are
not enough for this lane.

## Verification Targets

After build/install/launch, verify:

1. `files/imagefs/opt/ae/prefix-pack/catalog.tsv` exists.
2. `files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh` and
   `prefixpack-prefetch.sh` exist and are executable.
3. forensic logs contain `PREFIX_PACK_TOOLKIT_READY`.
4. `PREFIX_PACK_TOOLKIT_READY` reports the expected toolkit version.
5. `sh tools/prefix-pack/device-stage-cache.sh status <serial>` shows the same
   toolkit version and staged cache state under app-private `imagefs`.
6. the launched container no longer depends on a desktop or start-menu
   shortcut for Prefix Pack discovery; the Android-side toolkit remains the
   primary entrypoint.
7. the Android UI distinguishes cache readiness from install result and shows
   the save/log/state roots.
8. the Android UI keeps the visible `C:\AePrefixPack\cache` / staging paths
   explicit instead of hiding installers in temp-only paths.
9. donor/rootfs diagnostics already present in the live imagefs are reachable
   from the Android `Prefix Pack` surface.

## DirectX Note

The repo/rootfs fetch helpers now pull the official Microsoft binary URL
directly and force `curl --http1.1` in Termux because plain HEAD negotiation
against the CDN was unreliable. The Microsoft details page remains pinned in
the catalog as the human-readable provenance page.
