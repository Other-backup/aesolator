# Chapter 2 Live Forensics 2026-03-29

## Scope

Integrated live pass on:

- `Ae.solator`
- `WCP`
- `FreeWine11`
- device route `adb -> XServerDisplayActivity -> container_id=2`

Target line:

- container `2`
- runtime `Wine-bionic-11.4-arm64ec-1`
- package `freewine11-arm64ec.wcp`

## Live Symptom

Direct `desktop shell` launch on the Chapter 2 `FreeWine11` line opens the
startup screen and proves `wine` / `wineserver` / `wineboot.exe --init`, but
no mapped application window is observed before the activity later reaches the
background-pause path.

## Proof

### Route and runtime selection

- `ROUTE_INTENT_RECEIVED` emitted with trace ids:
  - `fw11-forensic-20260329-100152-c2`
  - `fw11-forensic-20260329-2nd-c2`
- runtime resolved as:
  - `Wine-11.4-arm64ec-1`
  - release tag `freewine11-arm64ec-latest`
  - artifact `freewine11-arm64ec.wcp`

### Runtime plane

- direct desktop-shell route selected:
  - `shell_executable=explorer.exe`
  - `desktop_shell_launch_mode=direct_explorer`
- translator lane proved:
  - `requested_emulator=fexcore`
  - `effective_emulator=fexcore`
  - `hodll=libwow64fex.dll`
- graphics lane proved:
  - `dxwrapper_active=dxvk+vkd3d`
  - wrapper provider `turnip-vulkan`
  - wrapper runtime source `wrapper-embedded`

### Device/process proof

- live processes observed after launch:
  - `wine`
  - `wineserver`
  - `C:\\windows\\system32\\wineboot.exe --init`
- active prefix root:
  - `/data/user/0/com.winlator.cmod/files/imagefs/home/xuser`
- active container metadata in `home/xuser/.container` matches container `2`

### Prefix activity

- prefix registry files were modified during the launch:
  - `system.reg`
  - `user.reg`
  - `userdef.reg`
- `wineserver` lock file updated at launch time

### Shell bootstrap proof gap

- first clean wait sample on the second pass:
  - `XSERVER_BOOTSTRAP_PRELOADER_FALLBACK_WAIT`
  - `attempt=1`
  - `shell_process_present=true`
  - `wineboot_process_present=true`
  - `wineserver_present=true`
  - `tracked_window_count=0`
  - `bootstrap_elapsed_ms=5714`

This proves the class is not:

- missing runtime package
- wrong runtime entry
- wrong translator route
- missing DXVK/VKD3D wrapper lane
- dead launch submission

## Fixed During This Pass

### 1. Legacy sync env drift

Before fix:

- container config still carried old `WINEESYNC=1`
- runtime accepted `Aesync`
- policy downgraded to:
  - `wine_sync_effective=esync`
  - `wine_sync_reason=manual_wine_sync_env_present`

After fix:

- old legacy esync-only default is promoted to Chapter 2 `auto`
- second live proof:
  - `wine_sync_effective=auto`
  - `wine_sync_userspace_policy=aesync`
  - `wine_sync_reason=legacy_esync_default_promoted_to_aesync`
  - `wineaesync=1`
  - `winefsync=1`
  - `wineesync=1`

Owner:

- `app/src/main/java/com/winlator/cmod/runtimeprofile/WineSyncPolicy.java`

### 2. Probe-container exception spam

Before fix:

- every launch emitted noisy stack traces:
  - `Skipping broken container: xuser-probe`
  - `Skipping broken container: xuser-protonprobe`
  - `NumberFormatException`

After fix:

- auxiliary probe homes are ignored structurally without exception spam:
  - `Ignoring auxiliary container home: xuser-probe`
  - `Ignoring auxiliary container home: xuser-protonprobe`

Owner:

- `app/src/main/java/com/winlator/cmod/container/ContainerManager.java`

## Remaining Real Residual

The remaining live class is narrower:

- direct `explorer` route is submitted correctly
- runtime/process/bootstrap evidence exists
- but mapped-window proof still does not materialize before the app later
  reaches `stop_background_pause`

Observed background interaction on the second pass:

- `XSERVER_RUNTIME_PAUSED`
- `reason=stop_background_pause`
- `desktop_shell_bootstrap=true`
- `tracked_window_count=0`

This means the remaining frontier is now one of these:

1. long-running or stuck `wineboot.exe --init` during desktop-shell bootstrap
2. app lifecycle policy pausing the runtime too early while bootstrap is still
   in progress
3. a deeper Wine/bootstrap/window-materialization class below the already-fixed
   route/runtime/package layers

## Non-owners

This pass did **not** support blaming:

- package selection
- wrong runtime package version
- stale duplicate runtime roots
- missing `explorer.exe` payload
- missing `FEX` route
- missing `DXVK/VKD3D` route
- `ntsync` absence

## Artefacts

- screenshot before fixes:
  - `/data/data/com.termux/files/usr/tmp/aesolator-forensic-20260329-100152-c2.png`
- screenshot after fixes:
  - `/data/data/com.termux/files/usr/tmp/aesolator-forensic-20260329-2nd-c2.png`

## Next Correct Batch

1. run a focus-stable launch where `Ae.solator` stays foreground long enough to
   separate `wineboot` latency from `stop_background_pause`
2. if the same class persists, inspect `wineboot` / window-materialization
   ownership below the current app/runtime layer
3. if background pause is the blocker, change lifecycle policy so bootstrap is
   not frozen while `wineboot.exe --init` is still the active shell-prep path

## Deep Sync Re-Audit

A later broad donor re-check tightened the interpretation of this report:

- [CHAPTER2_SYNC_DEEP_REAUDIT_2026-03-29.md](/data/data/com.termux/files/home/aesolator/docs/CHAPTER2_SYNC_DEEP_REAUDIT_2026-03-29.md)

The important correction is that `Aesync` is now good enough to treat as the
current Chapter 2 userspace broker, but it still must not be described as
semantic parity with kernel `ntsync`. The remaining no-window stall is still
real, but broad re-audit moved the strongest suspicion away from sync-policy
selection and closer to `wineboot` / loader / msgwait / lifecycle ownership.
