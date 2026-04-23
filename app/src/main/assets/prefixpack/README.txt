Ae.solator Prefix Pack Toolkit
==============================

Rootfs install dir:
  Z:\opt\ae\prefix-pack

What this toolkit does
----------------------
- keeps one audited catalog of the extra runtime installers we actually care about
- binds each payload to both a direct download URL and a source page URL
- stages cache-aware Windows helpers inside the prefix-visible Z: drive
- mirrors visible Windows installer copies into `C:\AePrefixPack\cache`
- separates cache, install, and install-state/log semantics instead of hiding
  them behind one vague fetch button
- auto-prepares missing payloads before install and dismisses the Android
  overlay so real Windows-side installer GUI can stay visible when required
- keeps the fetch/build lane reproducible instead of relying on random mirrors
- verifies pinned SHA-256 values where the catalog carries them
- uses the Android toolkit as the primary user-facing control surface instead of old desktop loader shortcuts

Default stack
-------------
- Full Visual C++ / VCRun stack from abbodi1406/vcredist plus legacy VC6 bootstrap
- Wine Mono 11.0.0
- Wine Gecko 2.47.4 x86 + x86_64
- Microsoft .NET Framework 3.5 SP1 + 4.0 Full + 4.8
- Microsoft DirectX June 2010 from the official Microsoft Download Center
- Microsoft DirectX SDK June 2010 tools lane for older diagnostics and control panels
- Microsoft XNA Framework 4.0 Refresh
- Microsoft XNA Framework 3.1
- OpenAL 1.1
- NVIDIA PhysX runtime + legacy PhysX runtime
- LAVFilters
- GLview graphics diagnostics

Cache path
----------
  Z:\opt\ae\prefix-pack\cache

Visible installer cache
-----------------------
  C:\AePrefixPack\cache

Save-data path
--------------
  C:\users\xuser\Documents\AePrefixPack\save_data

State path
----------
  C:\users\xuser\Documents\AePrefixPack\save_data\state

This mirrors the Ajay-style discipline of keeping prefix-tool state and logs
under a dedicated save-data root instead of scattering them across ad-hoc temp
locations.

Status helper
-------------
Run:
  Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd status

Links helper
------------
Run:
  Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd links

Windows loader
--------------
Run:
  Z:\opt\ae\prefix-pack\windows\prefix-pack-loader.cmd help

Primary UI entrypoint
---------------------
Use the Android-side Prefix Pack Toolkit from the runtime drawer. The Windows
loader stays as backend plumbing for installs, not as the main user-facing
entrypoint. The Android UI should make it obvious what is cached, what was
installed, which payloads are mirrored into the visible `C:` cache, where
logs/state live, and which diagnostics are already present.

Windows install helpers
-----------------------
- prefix-pack-loader.cmd
- install-default-runtime-pack.cmd
- install-vcrun-full.cmd
- install-vcpp-aio.cmd
- install-vcrun6.cmd
- install-wine-web-stack.cmd
- install-dotnet-framework.cmd
- install-directx-jun2010.cmd
- install-xna-framework.cmd
- install-openal.cmd
- install-directx-sdk-tools.cmd
- install-physx-runtime.cmd
- install-lavfilters.cmd
- install-glview.cmd

Rootfs-side shell fetch helper
------------------------------
  /opt/ae/prefix-pack/bin/prefixpack-prefetch.sh

Rootfs-side shell loader
------------------------
  /opt/ae/prefix-pack/bin/prefixpack-loader.sh

Repo-side cache helper
----------------------
  sh tools/prefix-pack/fetch-cache.sh

Examples from adb / run-as
--------------------------
List catalog:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh list

Show links and install bindings:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh show

Show cache status:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh status

Fetch all downloadable entries:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-prefetch.sh

Fetch one entry:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-prefetch.sh vcpp_aio

Fetch the legacy VC6 runtime entry:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-prefetch.sh vcrun6sp6

Show one entry with both URLs:
  run-as com.winlator.cmod sh files/imagefs/opt/ae/prefix-pack/bin/prefixpack-loader.sh show directx_jun2010

Repo-side catalog status:
  sh tools/prefix-pack/fetch-cache.sh status

Repo-side entry details:
  sh tools/prefix-pack/fetch-cache.sh show vcpp_aio
  sh tools/prefix-pack/fetch-cache.sh show vcrun6sp6
  sh tools/prefix-pack/fetch-cache.sh show directx_jun2010

DirectX note
------------
The pinned DirectX EXE URL is the official Microsoft binary, and the catalog
also keeps the Microsoft details page as provenance. The shell fetch lane uses
HTTP/1.1 because the public CDN was unstable with plain HEAD requests in
Termux.
