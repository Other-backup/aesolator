# Chapter 2 FreeWine-Ae.solator Contract

## Scope

`Chapter 2` starts when `Ae.solator` stops treating `Wine/Proton` as
interchangeable inputs and moves onto one dedicated runtime line:
`FreeWine11`.

The deliverable is one product stack:

- `Ae.solator` launcher / container owner
- `FreeWine11` native runtime source tree
- `wcp-runtime-lanes` package/archive owner
- one `bionic-only` `freewine11-arm64ec` WCP line with a `FreeWine11`
  `prefixPack.txz`

## Black Diamond Execution Layer

Chapter 2 donor transfer and runtime closure are governed by the workspace
Black Diamond constitution:
`/data/data/com.termux/files/home/.codex/rules/OMEGA_BLACK_DIAMOND_MONOLITH.md`.

For this product line, that means no donor-copy shortcut and no local
familiarity shortcut. Every high-impact donor/runtime/build/package decision
must be classified as accept donor, preserve local, merge, synthesize a
stronger third solution, or reject donor, with source proof, product-contract
proof, verification strategy, and a durable evidence-ledger checkpoint.

Black Diamond also makes source-first compile stitching mandatory: harden
FreeWine11 ARM64EC, Aesync, bionic-native, bootstrap, X11-first, prefixPack,
artifact, and provenance contracts into a coherent source state before using a
broad build wave as proof.

## Proton-Wine Donor Frontier

`FreeWine11` remains the source-of-truth for this line, but Chapter 2 source
closure must still compare every material source-side class against the active
`proton-wine` donor wall first, especially Android-facing logic in `dlls`,
`libs`, `programs`, `server`, and build glue.

For this line, `proton-wine` is the primary source donor and `Nightlies`-style
repos are build/packaging harness donors. The transfer rule is to carry donor
intent into `FreeWine11` as native product code without literal donor drift:
preserve `Aesync`, `bionic-native` host truth, `arm64ec` ownership, and the
`prefixPack` contract while rejecting donor bugs, stale wrapper assumptions,
or app-specific baggage.

On the local `FreeWine11 -> wcp-runtime-lanes/wine-src` mirror lane, source
sync must stay content-truth only.
Exclude generated/build dirt such as `build-wine*`, `build-wine-hostcheck`,
`autom4te.cache`, `.freewine11`, `out`, and similar local artifacts from the
mirrored source surface, and do not classify touched mirror mtimes as source
drift.
Resume/fetch lanes must compare and copy by real file content/type plus
executable-bit truth, then touch only the changed mirrored source files to
invalidate the build graph.
Do not let `rsync` timestamp/attribute churn reopen whole-tree source drift
and force a fake broad rebuild before `make` even starts.

On pure `aarch64-windows` PE outputs that carry `arm64_import_shims.c`,
duplicate provider import archives already covered by local shims must be
filtered centrally in `tools/makedep.c` instead of being stripped module by
module.
On that same `aarch64-windows` `arm64_import_shims.c` frontier, treat
`winecrt0` import ownership as two central classes inside `tools/makedep.c`:
the broad DLL-support band and the narrow private resolver trio
`LdrGetDllHandle`, `NtQueryVirtualMemory`, and
`RtlFindExportedRoutineByName`.
Measure closure for that class from emitted
`aarch64-windows/arm64_import_shims.o` truth, not from raw include-closure
alone, and keep `libs/winecrt0/arm64_import_shims.c` classified as the
self-owned root rather than a missing-autoforce residual.
Do not reopen that terrace by spraying resolver thunks into modules one by
one or by widening the broad support band where only the narrow resolver trio
is missing.

## Invariants

1. `FreeWine11` is the runtime source-of-truth for this line, not Valve Proton.
2. The target artifact is the `Ae.solator` runtime package:
   `freewine11-arm64ec.wcp` plus compressed release variants and a
   `FreeWine11`-built `prefixPack.txz`.
   Packaging-only or post-build resume lanes must not assume the install-stage
   tree under `stage/` still matches the live `build-wine` outputs.
   If `compose/pack/deploy` needs `STAGE_DIR` and the install surface
   `usr/{bin,lib/wine,share/wine}` is missing, partial, or replaced by a
   previously composed `arm64-v8a` runtime layout, rematerialize `STAGE_DIR`
   from the current `build-wine` through `make install DESTDIR=...` before
   patching or packaging.
   Do not "fix" that class with empty directory creation or by packaging from a
   stale partial stage.
   The same rule applies to local deploy extraction caches derived from a
   `.wcp`: do not reuse an old `out/freewine11-deploy-stage-*` tree merely
   because `profile.json` and `prefixPack.txz` still exist there; local deploy
   stage must be tied to the current `.wcp` hash and rematerialized when the
   package changes.
3. `Ae.solator` is the consumer and runtime-integrator:
   container creation, content routing, runtime model selection, translator
   payload routing, and forensic proof stay app-owned.
4. `wcp-runtime-lanes` is the packaging/publish lane, not the runtime source.
5. `FEX`, `wowbox64`, and related translators are runtime dependencies for
   guest `x86/x64` execution, not the default root-cause for native
   `aarch64` bootstrap failures such as `wineboot --init`.
6. `esync` and `fsync` stay enabled by intent. If `fsync` cannot run because
   the Android app sandbox blocks `futex_waitv`, that must stay a capability
   truth reported by runtime proof, not a policy-level silent disable.
   On locked-bootloader or rootless Android devices, `ntsync` is an optional
   kernel capability, not the required closure path.
   The app-owned top policy must stay modular through
   `AERO_WINE_SYNC_BACKEND=auto|ntsync|aesync|fsync|esync|server`.
   On this line, `auto` enables the composite userspace lane `Aesync`:
   `WINEAESYNC=1`, `WINEFSYNC=1`, `WINEESYNC=1`, with runtime dispatch order
   `fsync -> esync -> server`.
   `Ae.solator` accepts `Aesync` only for the selected Chapter 2
   `FreeWine11` runtime profile:
   `bionic` `Wine` family plus `wcp-runtime-lanes` provenance and a
   `freewine/freewine11` runtime surface such as release tag or artifact name.
   On non-`FreeWine11` runtimes, `auto` and explicit `aesync` requests must
   clear `WINEAESYNC` and degrade to legacy `fsync+esync` fast paths with an
   explicit forensic reason.
   If the shipped Wine runtime reaches `/dev/ntsync`, kernel `ntsync` may still
   win opportunistically; app env controls the legacy userspace fast-path
   policy, not a hard disable for kernel `ntsync`.
7. `bionic-only` means the runtime package must stay valid for Android-native
   deployment and must not drift toward a generic desktop Proton archive.
8. The active Vulkan/OpenGL Mesa wrapper baseline must remain explicit and current:
   `Ae.solator` should consume the official non-main `Mesa staging/26.1` line
   with the local `aeso-wrapper-forwardport-mesa26-v1` overlay instead of
   treating older wrapper donor archives or Mesa `main` as a source-of-truth.
   On Android KGSL, Zink over Turnip is the default Mesa OpenGL path until a
   device-specific Freedreno Gallium route is proven.
9. Direct desktop-shell bootstrap must not honor stale `EXTRA_EXEC_ARGS`
   overrides.
10. For bionic arm64ec `FreeWine11` desktop-shell bootstrap, direct
    `explorer.exe` is the primary route; `WinHandler` is a fallback bridge, not
    the canonical path.
11. Even on the direct `explorer.exe` route, the command form must stay
    canonical:
    `wine explorer /desktop=shell,<geometry> "explorer.exe"`.
    Drifting to `wine explorer.exe /desktop=...` reopens black-screen
    bootstrap faults.
12. Desktop-shell bootstrap must not mark the session visually ready from
    process proof alone when `tracked_window_count=0` and `WinHandler` is not
    required. In that state, the app must keep waiting for mapped-window proof
    or trigger the fallback bridge.
13. If the direct desktop-shell route falls back to the `WinHandler` bridge,
    the launched guest command must stay inside the canonical desktop host:
    `wine explorer /desktop=shell,<geometry> winhandler.exe "wfm.exe"`.
    Bare `wfm.exe` or bare `wine winhandler.exe "wfm.exe"` can leave
    `winHandlerReady` permanently false.
14. For arm64ec desktop-shell bootstrap, requested `fexcore` stays the
    preferred route when both FEX translator DLLs are present; `wowbox64` is
    only a payload-missing fallback.
15. On the `Chapter 2` ARM64EC tool/package line, PE output and
    import-library lookup must keep the real target identity:
    `arm64ec-windows` artifacts resolve through `arm64ec-windows`, not through
    `aarch64-windows`.
    Only explicitly shared lookup surfaces that remain architecture-neutral for
    this line, such as current typelib lookup, may reuse the compatible
    `aarch64-windows` path.
    Unix-host lookup for `ARM64EC` remains `aarch64-linux-gnu`; do not collapse
    those two contracts back into one generic `aarch64` path helper.
16. On the same ARM64X / `aarch64-windows` PE line, malformed import directory
    names are an emitted import-library class owned by `tools/winebuild/import.c`,
    not a runtime loader quirk.
    Keep the import-lib section layout aligned with the LLVM/COFF contract:
    `.idata$2` at 4-byte alignment, `.idata$4` and `.idata$5` at pointer-size
    alignment, and `.idata$6` at 2-byte alignment.
    Measure closure from emitted PE truth with `llvm-readobj --coff-imports`
    instead of per-module runtime workarounds or address-ledger churn.
17. On the same pure `aarch64-windows` consumer-shim line, current identity
    helpers are one central owner class in
    `include/wine/arm64_current_identity_import_shims.inc`, not a per-module
    address-fix terrace.
    Keep `GetCurrentProcess`, `GetCurrentProcessId`, `GetCurrentThread`,
    `GetCurrentThreadId`, and `GetProcessHeap` as local fast paths matching
    Wine's own `__WINESRC__` header semantics, and route
    `GetCurrentProcessorNumber` through a local
    `NtGetCurrentProcessorNumber` import thunk instead of a generic public
    `GetCurrentProcessorNumber` import thunk or a fake constant fallback.
    Measure closure for that class from emitted
    `aarch64-windows/arm64_import_shims.o` plus final PE truth; do not reopen
    it with per-DLL thunk surgery or by treating safe local
    `__imp_* -> local helper` identity slots as the old self-looping public
    import-thunk class.

## Current Verified Runtime/Forensic Contract

- The verified `FreeWine11` WCP layout preserves the ABI subtree
  `arm64-v8a/*` and exposes compatibility symlinks such as `bin` and `lib`.
  Flattening the subtree back into the runtime root is a contract violation
  because it breaks native data-path resolution such as `share/wine/nls`.
- Fresh direct-route `adb` forensics must emit `ROUTE_INTENT_RECEIVED` and
  `LAUNCH_EXEC_SUBMIT` with the supplied `forensic_trace_id`.
- For desktop-shell bootstrap on a live `Container`, `terminal=0` inside the
  short wait window is not a failure by itself once `wine/wineserver` emergence
  proves the session is alive.
- For the direct desktop-shell route, the app-side command must remain
  `wine explorer /desktop=shell,<geometry> "explorer.exe"`, and
  process-only readiness with zero tracked windows is explicitly rejected.
- If fallback is required, the bridge path must launch
  `wine explorer /desktop=shell,<geometry> winhandler.exe "wfm.exe"` so that
  the Java-side `WinHandler` can become ready on the intended shell desktop.
- The Java-side `WinHandler` UDP bridge must be started before guest-launch
  submission and must emit visible forensic events for bind/init/failure on
  ports `7947/7946`; a silent bind failure is a bootstrap defect.
- Direct desktop-shell bootstrap must not degrade into `WinHandler` fallback
  while `wineboot.exe --init` is still present; a live first-boot init keeps
  the route on direct `explorer` until prefix bootstrap settles.
- Android lifecycle policy must not convert a transient `onStop()` into
  immediate `stop_background_pause` while direct desktop-shell bootstrap still
  lacks visual-ready proof and live process proof still shows
  `wineboot`/`wineserver`/`explorer`.
  In that state the app keeps the runtime on a bounded deferred-pause grace
  instead of freezing the bootstrap immediately.
  That lifecycle pause path must also honor the container suspend policy:
  `manual` and `never` do not auto-`SIGSTOP` the Wine tree on `onPause()` or
  `onStop()`, while `auto` may renew the bounded grace during live desktop
  bootstrap instead of freezing it after one fixed short timeout.
- For the selected smartphone Turnip route,
  `GRAPHICS_PROVIDER_CONTRACT_APPLIED` is only honest after the app has either
  materialized the required `freedreno-opengl` companion package from the known
  contents catalog and re-resolved it, or emitted an explicit degraded-provider
  state with companion-missing fields.
- Wrapper-embedded Vulkan runtime forensics keep
  `vulkan_api_dump_requested/applied` and
  `vulkan_validation_requested/applied` visible, but missing validation/api_dump
  layers are no longer active warnings in this product line.
- For Chapter 2 desktop/bootstrap closure, source-side forensics must not stay
  in one central loader seam alone. Keep explicit owner-file bootstrap markers
  across the runtime core entry layer:
  `winecrt0` exe/dll entry stubs,
  `tools/wine`,
  `server/main.c`,
  launch programs `wineboot`, `explorer`, `rpcss`, `services`, `winedevice`,
  and `start`,
  plus the launch-critical DLL core
  `ntdll`, `kernel32`, `kernelbase`, `user32`, `win32u`, `gdi32`, `imm32`,
  `comctl32`, `shcore`, `shlwapi`, `setupapi`, `ole32`, `oleaut32`,
  `combase`, `rpcrt4`, `sechost`, `shell32`, `uxtheme`, `ws2_32`,
  `dnsapi`, `nsi`, and `netapi32`.
  Do not call bootstrap forensics whole-tree closed from one seam or one lucky
  stack alone.
  On this line, the closure claim must also survive
  `.freewine11/scan_bootstrap_forensics_frontier.py` in core mode with zero
  residual owner-entrypoint files, and the same scanner in
  `--runtime-center` mode with zero residual runtime-center owner files across
  `winecrt0`, `ntdll`, `kernel32`, bootstrap DLLs, Android driver glue,
  service/desktop programs, and `wineserver`.
  It must also survive `--module-surface` with zero residual bounded
  runtime-module owner files and `--whole-tree-surface` with zero residual
  compiled-source files across the full make graph once whole-tree autotouch is
  enabled through `tools/makedep.c`.
  Fresh container-launch/runtime failures on this line are parser-owned until
  proven otherwise: every new `wineboot`, `explorer`, DllMain/process-attach,
  loader-init, import-owner, or launch-time `c0000005` symptom must be
  promoted into a reusable `omega` owner class or dedicated scanner and rerun
  across the current whole source/build tree before the next fix batch.
- On the Chapter 2 smartphone-hosted product line, PE-side `ntdll`
  bootstrap/loader telemetry in `dlls/ntdll/loader.c`,
  `dlls/ntdll/heap.c`, and `dlls/ntdll/env.c` must not stay on the default
  product hot path through direct `__wine_dbg_output()` writes.
  Keep that deep telemetry compile-available only behind
  `FW_BOOTSTRAP_OMEGA_FORENSICS` for dedicated forensic builds; default product
  builds keep the safe bootstrap surface and whole-tree compile coverage
  without re-entering early loader/unwind/heap paths just to emit text.
  On the default product PE lane, keep synchronous `__wine_dbg_write()`
  bootstrap emission narrowed to executable/bootstrap-program scopes such as
  `wineboot`, `explorer`, `services`, `rpcss`, `winedevice`, and the
  `exe_*` entry scopes. DLL / loader / process-attach scopes in PE modules
  such as `kernelbase`, `kernel32_process_attach`, `user32`, `shell32`,
  `win32u`, `comctl32`, and similar runtime-center DLLs must stay
  compile-available but runtime-quiet unless a dedicated omega forensic build
  explicitly re-enables them. Do not reopen Chapter 2 product runs by letting
  default PE attach paths synchronously log through `__wine_dbg_write()`.
  On that same PE forensic lane, buffered omega telemetry must emit through
  raw `__wine_dbg_write(len)` rather than line-buffering `__wine_dbg_output()`,
  because `__wine_dbg_output()` itself re-enters `strrchr()` / `strlen()` in
  `ntdll/thread.c` and reopens early loader / SEH recursion while the owner
  class is still being observed.
  On this line, omega whole-tree runtime coverage is now split deliberately:
  whole-tree `autotouch` `tu_load` markers stay runtime-observable across the
  full source graph, using constructor / `.init_array` carriers on the Unix /
  Android-host side and per-TU `.CRT$XCU` carriers on the PE / ARM64EC side.
  That whole-tree carrier is C-family only: assembler `.S` translation units
  must not receive forced C headers or function-entry instrumentation just to
  satisfy autotouch coverage, or install/stage lanes reopen on assembler parse
  failures instead of real runtime drift.
  Per-file first-hit execution markers `tu_exec` remain enabled only on the
  Unix / Android-host side through `-finstrument-functions` in `unix_cflags`.
  Do not pretend PE / ARM64EC objects support that same function-entry
  instrumentation contract unless a shared link-visible carrier for
  `__cyg_profile_func_enter/exit` has been proven for that graph.
  On that same whole-tree autotouch line, forced `-include`
  `include/wine/freewine_bootstrap_autotouch.h` is part of the emitted build
  graph even though it does not appear in source text. Keep
  `tools/makedep.c` tying every non-assembler autotouch TU directly to
  `freewine_bootstrap_autotouch.h`, `freewine_bootstrap.h`, and
  `freewine_bootstrap_filter.h` as file dependencies; otherwise PE/ARM64X
  objects quietly survive header edits and relink stale `fw_bootstrap_log`
  code. Do not model that carrier as an ordinary parsed include if it breaks
  the existing `config.h first` invariant on host tools.
  On the Windows-target clang lane, do not gate whole-tree autotouch on
  `__GNUC__` alone; the carrier must stay live under `__clang__` too or the
  PE graph silently drops back to phantom coverage.
- On the Chapter 2 ARM64X Android bootstrap line, do not scope attach-safe
  runtime logic to `__aarch64__` alone when the emitted module ships as
  `ARM64X` and both the native ARM64 slice and the ARM64EC overlay slice can
  enter the same bootstrap path. Any Android/loader-lock/process-attach
  workaround across runtime-center or bounded module-surface owner files must
  cover `__arm64ec__` too where the same attach path exists. Closure for that
  class must also survive
  `.freewine11/scan_arm64x_android_bootstrap_guard_drift.py` with zero
  residual native-only guard blocks in both module-surface and whole-tree
  modes.
- On that same Chapter 2 ARM64X / `aarch64-windows` Android attach line,
  `DisableThreadLibraryCalls()` is now a central emitted-PE attach owner
  class, not a per-DLL cleanup ritual. The exported implementation remains
  owned by `kernelbase`, but PE ARM64 / ARM64EC consumer callsites should
  collapse to a local no-op from headers so DllMain/process-attach paths stop
  importing and calling the loader-side optimization across the whole DLL
  forest. Measure closure from emitted PE truth and live runtime behavior; do
  not reopen this class by editing `netapi32`, `ws2_32`, `comctl32`,
  `shell32`, and neighbors one by one.
- On locked/rootless Android, omega sync closure now targets a userspace
  broker backend tentatively named `Aesync`, not arbitrary kernel-module
  loading and not another env-only tuning round.
  The backend must be reasoned as one lane across app policy,
  `sync.c` dispatch,
  backend implementation,
  server-object coverage,
  APC/msgwait,
  mixed waits,
  and Android shared-memory / sandbox limits.
- On that same line, `Aesync` is a runtime-owned composite broker, not a thin
  alias:
  it owns mixed `WaitAny`,
  direct mixed `WaitAll` for `fsync+esync` sets through one shared userspace
  coordinator around final zero-timeout acquire / rewind / retry,
  with server fallback only when a `server` lane still participates or a
  backend surface is not locally rewindable,
  `NtSignalAndWaitForSingleObject` fallback,
  queue/msgwait coordination,
  and balanced backend probe lifetime.
  Do not push those classes back into generic outer fallback paths and still
  call the line integrated.
  The chosen kernel-class analog for this line is:
  authoritative typed shared state plus kernel-backed carrier fds and a
  per-thread `epoll` broker.
  Use `eventfd` for generic object, queue, APC, and synthetic thread carriers;
  `timerfd` for timeout / timer carriers; and `pidfd` where the kernel surface
  is genuinely available.
  On the current Android `5.10` line, that `pidfd` clause is process-only:
  process waits may use `pidfd` as a pollable wake carrier, while thread waits
  do not have a usable `PIDFD_THREAD`-class surface here and must stay on the
  existing userspace/server resweep path.
  Where a remaining `server`-lane handle resolves through a server-owned
  internal `event_sync`, `Aesync` must bootstrap and cache that sync's
  dedicated server-owned `eventfd` carrier and feed it into the broker before
  falling back to periodic server slice polling. Periodic server slice polling
  is only for the residual non-pollable server lane, not for
  `event_sync`-backed waits that already have a carrier.
  On the current line, the same carrier contract also applies to hot
  server-only async wait handles: `async` objects may expose their own
  server-owned `eventfd` wake carrier into `Aesync` broker waits, while final
  async result/status and completion side effects remain server truth.
  On the current line, that direct server-carrier contract also applies to
  per-wait completion handles and monotonic server-owned one-bit objects:
  `completion_wait`, `startup_info`, `job`, `thread_apc`, and `context` may
  expose their own server-owned `eventfd` wake carrier into broker waits,
  while queued completion payloads, startup/job/APC/context semantics, and
  any side effects remain server truth.
  A server-owned waiter object does not count as integrated merely because the
  server can export a carrier for it. If the client-side blocking edge still
  goes through `wait_async()`, `wait_internal_server()`, or
  `server_wait_for_object()`, that class is still effectively delegated to the
  server. It only crosses the bar when the blocking path is routed through an
  ordinary `NtWaitForSingleObject()` / generic wait path that `Aesync` can
  actually classify and broker.
  On the current line, async wait handles and completion wait handles have
  already been moved onto that generic wait path; do not reintroduce
  `wait_async()` / `wait_internal_server()` bypass for them.
  Do not claim whole-tree `Aesync` closure from spot checks alone.
  On this line, the closure claim must also survive the source-tree frontier
  parser:
  legacy bypass helpers such as `server_wait_for_object` and
  `wait_internal_server` must be absent,
  direct client `server_wait()` and `server_select()` call sites must collapse
  to the integrated wait engines and the explicit `thread.c`
  context/debug bridge,
  and reply-handle waiter protocols must route back through ordinary
  `NtWaitForSingleObject()` / generic wait ownership instead of hiding a
  residual client-side bypass.
  The carrier fd is a wake nudge, not the final truth. Queue masks, APC
  presence, owner/count state, and other typed semantics must still be
  rechecked from shared memory or server-owned runtime state after broker wake.
  `pidfd` is a pollable process-exit carrier in that sense, not an `eventfd`-
  style counter and not something to model as drainable object truth.
  The same non-drain rule applies to kernel-object carriers that already
  materialize as `inproc_sync` / `ntsync` fds; only explicit server-owned
  `eventfd` carriers are drainable.
  Where `Aesync` has pollable carrier fds from `esync` or runtime-provided
  `fsync` carriers, mixed `WaitAny` blocking must go through the per-thread
  `epoll` + `timerfd` broker and then resweep authoritative lane state in
  original handle order; do not regress that class back into sequential slice
  polling while the broker path exists.
  If a mixed broker wait still includes a non-pollable `server` lane, the
  broker must also arm periodic `timerfd` slice wakeups and authoritative
  resweeps; do not sleep indefinitely on carrier fds alone while a server lane
  participates.
  `NtSignalAndWaitForSingleObject` classification inside `Aesync` must
  normalize pseudo-handles before lane selection or server fallback, so that
  pseudo-handle waits stay inside the integrated broker contract instead of
  failing classification up front.
  Where backend-local cached wait-fd/type helpers already exist, `Aesync`
  classification and broker setup must use those helpers instead of issuing
  fresh raw server probes for lane/type discovery on every hot wait path.
  Once that backend-local bootstrap has already discovered the typed object
  class, `Aesync` must cache and reuse that type alongside lane/msgwait
  classification instead of immediately re-probing the backend again just to
  rebuild mixed-wait metadata for the same hot handle.
  For mixed `WaitAll` sets that stay within `fsync+esync`, `Aesync` must own
  the operation directly through one shared userspace coordinator word:
  final acquire happens only under that coordinator, backend-local rewind
  happens before it is released, and broker/slice sleeps happen only outside
  it. Same-lane `fsync` / `esync` waits and `Aesync`-owned signal-state
  mutation wrappers must honor that same coordinator, or the `WaitAll` class
  is not actually integrated.
  On alertable broker waits, `Aesync` must register every live APC carrier fd
  that the server-side composite wake path can signal, not just the first APC
  fd that happens to exist, so that the client-side alert path matches the
  server-side composite wake contract.
  Do not choose `binder`, `ashmem`, `dma_heap`, or `signalfd` as the primary
  NT wait carrier model for this line, and do not treat `futex_waitv` as the
  whole object model.
  The surveyed local `/dev` surfaces `/dev/synx_device`, `/dev/spec_sync`,
  `/dev/kgsl-3d0`, binder-family devices, `ashmem`, `dma_heap`, `tun`, `uhid`,
  `vsock`, and `vhost-vsock` do not provide a general NT dispatcher-object
  model for this line. Only process `pidfd`, server-internal `event_sync`
  carriers, explicit server-owned async carriers, `completion_wait`
  carriers, and the monotonic one-bit server objects `startup_info`, `job`,
  `thread_apc`, and `context` survived the bar for `Aesync` core, along with
  wrapper objects that can honestly unwrap to those same pollable inner sync
  surfaces such as current `D3DKMT` shared sync/resource handles.
  When a server-owned object already resolves through `get_sync()` to an
  `inproc_sync` object, `Aesync` may bootstrap that pollable fd into the broker
  as a non-drain kernel-object carrier instead of forcing it back into
  residual server slice ownership.
  Do not describe entry-shaped waits such as `keyed_event` as carrier-ready on
  object-local state alone on this line. They only cross the bar when they are
  reshaped into a dedicated async waiter protocol with keyed-event-owned
  hashed wait/release queues by `process+key`, one per-request waiter object,
  and an ordinary wait handle whose blocking path flows back through the
  generic `Aesync` broker. Without that protocol shape, `keyed_event` remains
  residual server ownership.
  The local `/dev/spec_sync` / Qualcomm `synx` / `qcom_sync_file` surface is
  part of the `dma_fence` / `sync_file` graphics-fence world, not a general NT
  dispatcher-object backend. It may be investigated for Vulkan/DXVK external
  fence interop only, not as `Aesync` core.
  On the current line, that graphics-fence interop means a narrow
  `sync_file`-aware fence import path:
  detect real `sync_file` fds with `SYNC_IOC_FILE_INFO`, prefer
  `VK_EXTERNAL_FENCE_HANDLE_TYPE_SYNC_FD_BIT` only for temporary host fence
  imports, and fall back to the existing `OPAQUE_FD` route otherwise.
  Do not silently widen that to semaphore timeline paths or to fence export
  without fresh source-backed proof, because `SYNC_FD` permanence semantics are
  weaker than `OPAQUE_FD`.
  Do not describe this userspace lane as semantic parity with kernel `ntsync`
  while `WaitAll` still depends on a split-lane userspace coordinator rather
  than one typed kernel object engine, or while alert/APC or queue/msgwait
  correctness still depends on split-lane or server ownership.
  During desktop-shell no-window bootstrap, do not blame the sync backend first
  while `wineboot`, loader-thread serialization, GUI message-wait ownership, or
  Android lifecycle pause remain unruled-out.

## Integrated Debug Loop

All non-trivial runtime tails now close through one integrated loop:

1. prove the app-side symptom in `Ae.solator`
2. verify WCP layout and runtime contract
3. debug the native `FreeWine11` layer
4. verify translator payloads (`FEX` / `wowbox64` / `box64`) only at the
   layer where they actually become active
5. sync the resulting rule/doc changes across all three repositories

Integration-first development is mandatory on this line:
design and bug-fix work must start from the live integrated product path
`Ae.solator -> WCP -> FreeWine11 -> translator/device evidence`, not from an
isolated repo-local view.
For Android-local `FreeWine11` builds on this line, keep one late
`parallel-profile` snapshot per session:
the logged profile and actual `make -j` must match,
all online CPUs are the default target on the `winlator-bionic` /
`bionic-native` lane,
and any lower jobs count must be backed by explicit live RAM proof while
preserving the current `build-wine` progress.
On that same smartphone-hosted `winlator-bionic` / `bionic-native` line,
the runtime contract is not "generic Linux, but bionic-tagged".
The core Wine launchers and Unix core modules must be Android-native, and the
sidecar module surface must match the actual Android host:
`alsa`, `pulse`, and `gstreamer` are allowed only when the live `Ae.solator`
rootfs ships the matching bionic libraries;
`wayland` is forbidden on the current smartphone host;
`usb` is disabled by default and the default smartphone build surface must
pass upstream `--without-usb` rather than merely pruning `wineusb` after the
fact.
If no glibc runtime is shipped, package forensics must not keep emitting
`glibcRuntime` ownership in `manifest.json`, `source-refs.json`,
`build-env.txt`, or `critical-sha256.tsv`.

## Whole-Frontier Source Closure Rules

- Runtime black-screen or bootstrap stalls that resolve into `FreeWine11`
  source contracts must be escalated as whole-frontier source batches rather
  than patched repo-by-repo or module-by-module.
- The default source proof for those tails is a freshest full `make -k all`
  harvest plus a parser-backed whole-tree reflection, not a first-error retry.
- Full harvest mode is the default opening move for structural runtime/source
  tails, not a late fallback after several one-module retries.
- If the app/runtime forensic surface is too thin to isolate the source
  contract, add the missing instrumentation first:
  `adb`/`run-as` samplers, symbolizers, route markers, and log reducers are
  part of the fix path, not optional helpers.
- Closing the visible compile symptom is not enough:
  the same batch must reflect the touched helper/provider/runtime route across
  the wider tree so dormant consumers or stale fallback paths do not reopen
  the same black-screen class on the next run.
- A pass is incomplete if it proves only the app-side symptom or only the
  source-side compile edge:
  app route, package/runtime layout, source/provider logic, and stale
  fallbacks must be reflected together before the batch is called closed.
- `app + runtime/package + source/parser` are only the minimum closure triad.
  If the measured frontier still spans lower static libs, helper objects,
  provider archives, direct-linked owners, generated artifacts, or stale
  integration bridges, the same batch must cover those levels too.
- Once a structural class crosses those layers, treat it as a whole-source-tree
  blast radius:
  lower static libs, helper objects, provider archives, direct-linked owners,
  parser classes, generated artifacts, package/runtime validators, and stale
  integration bridges remain in the same batch until the freshest full harvest
  proves they have all moved together.
- If the same observability blind spot appears twice, the next pass must add
  durable tooling for it instead of relying on another manual forensic read.
- Once a runtime rule or source-side operating principle changes, synchronize
  the rule across `Ae.solator`, `FreeWine11`, and `wcp-runtime-lanes` in the
  same pass.

## Delivery Rules

- Prefer a scratch-built `FreeWine11` prefix pack over donor repacks.
- Keep `prefixPack` and `WCP` provenance explicit.
- For `FreeWine11` `prefixPack.txz` scratch bootstrap on this line, timed-out
  `wineboot --init` alone is not a package failure once the prefix contract is
  complete.
  The prefix contract now includes both the old directory skeleton and a
  desktop-shell baseline.
  The builder may mark `acceptPartialBootstrap=1` only when the prefix already
  has the three registry hives, `dosdevices/c:` and `z:`, the Windows
  directory skeleton, `users/xuser/Desktop`, `users/Public`, `Program Files`,
  and `Program Files (x86)`, plus `windows/explorer.exe`,
  `windows/command/start.exe`, `windows/system.ini`, NLS sentinels such as
  `sortdefault.nls`, `locale.nls`, and `normnfd.nls`, `windows/inf/winebus.inf`,
  `windows/system32/drivers/winebus.sys`, user and ProgramData Start Menu
  directories, `Explorer\\Shell Folders` plus
  `Explorer\\User Shell Folders` in both `user.reg` and `userdef.reg`,
  `Software\\Wine\\Drivers` with `Graphics=x11` in both `user.reg` and
  `userdef.reg`, and
  `Software\\Wine\\Explorer\\Desktops` with explicit `shell`
  desktop settings in `user.reg`.
- On the smartphone-hosted `FreeWine11` line, `wfm.exe` plus
  `winhandler.exe` may enter `prefixPack` only as a narrow audited
  desktop-bridge adjunct from the current product line, e.g. a live prefix or
  forensic backup proven on-device, through an explicit allowlist/import lane.
  Do not widen that adjunct into `Installer` or MSI/package caches, `.NET`,
  `mono`, `assembly`, `Lang`, utility executables such as `7z`, `curl`, or
  `wget`, or Ajay shell baggage, and do not treat the bridge adjunct as part
  of the `acceptPartialBootstrap` minimum contract.
- For the smartphone-hosted `winlator-bionic` `FreeWine11` line, direct
  desktop-shell bootstrap must prefer the X11 user-driver path because the
  `Ae.solator` host engine itself is X11. Keep `explorer` desktop fallback
  order X11-first and seed `HKCU\\Software\\Wine\\Drivers\\Graphics=x11` for
  both the live user and `.Default` inside `prefixPack.txz`. Degrade to
  `android` only when the live runtime surface honestly lacks
  `winex11.so` / `winex11.drv`.
- On that same smartphone-hosted X11 line, Android-specific `winex11` donor
  logic is no longer a blanket reject. Transfer it as product-native drift:
  keep `XShm` source-owned through `android/android_sysvshm`, build and stage
  `libandroid-sysvshm.so` through a dedicated `ANDROID_SYSVSHM_LIBS` lane,
  and build that host-side Android sidecar through a probed Android-host
  compiler surface instead of inheriting the ambient global `CC`.
  Prefer the local host LLVM lane only when it can actually compile and link a
  minimal Android shared-object probe against the live Termux/bionic host
  surface; otherwise degrade explicitly to the Termux clang wrapper that
  already carries the correct Android target, sysroot, CRT objects, and bionic
  runtime search path.
  materialize that lane in configure truth and the generated top-level
  `Makefile`, and refresh generated rules through `config.status Makefile`
  after configure or config-truth sync so `winex11.so` does not silently keep
  a stale link line without `libandroid-sysvshm.so`.
  On this line, packaging/install closure must also treat `winex11` as part of
  the required staged runtime surface whenever the live `build-wine` already
  materializes both `dlls/winex11.drv/winex11.so` and the `aarch64-windows`
  `winex11.drv`; a stage that still lacks those files is stale and must be
  rematerialized through `make install DESTDIR=...`, not silently accepted.
  The configure-surface drift stamp for this lane must also retain
  `ANDROID_SYSVSHM_LIBS`; if a resume/reconfigure path drops that env, it must
  be treated as configure drift and rebuilt instead of silently regenerating a
  `Makefile` that links `winex11.so` without `libandroid-sysvshm.so`.
  Resume/configure normalization for that lane must verify the live generated
  top-level `build-wine/Makefile` too, not only a cached drift stamp:
  if expected `ANDROID_SYSVSHM_LIBS` is non-empty but the generated
  `Makefile` still materializes it as empty or mismatched, that is configure
  drift and must rerun `configure` before packaging.
  `validate_android_sysvshm_runtime_payload()` must fail explicitly on a
  missing staged `winex11.so` in that state, not exit via a bare non-zero
  `return` after `[[ -f ... ]]`.
  Loader closure for that lane must also prefer current emitted-artifact truth
  over historical probe trees: scanners must read the active
  `wcp-runtime-lanes/build-wine` `winex11.so` before any older
  `build-wine-winex11-*` artifact, and if a refreshed current
  `build-wine/dlls/winex11.drv/winex11.so` still lacks
  `DT_NEEDED libandroid-sysvshm.so`, that ELF is stale and must be invalidated
  for rebuild instead of being masked by an older green probe/hybrid artifact.
  The same loader-contract class applies to final native ELF links generally:
  generated `tools/makedep.c` rules for Unix `.so` and native host programs
  must depend on the generated top-level `Makefile`, or configure-derived link
  surface changes such as `ANDROID_SYSVSHM_LIBS`, `RUNPATH`, or similar
  loader-owned flags silently keep stale emitted artifacts even after
  `config.status Makefile`.
  On the Termux-hosted build lane, `winex11` also depends on an honest X11
  proto/header pkg-config surface, not just shared libraries:
  keep `xorgproto`/`xtrans`, `X11/X.h`, and `pkg-config x11 xext` available,
  and treat host X11 surface drift as full configure drift that must rerun
  `configure` before closure. Do not accept `dlls/winex11.drv` silently
  remaining in `DISABLED_SUBDIRS` because the host lacked X11 proto packages,
  and do not accept a staged `winex11.so` that still misses
  `DT_NEEDED libandroid-sysvshm.so` once the live build copy already links it;
  that class must invalidate `stage_has_installed_runtime_surface()` and force
  a fresh `make install DESTDIR=...` rematerialization before `compose/pack`,
  expose Android window identity hints `_NET_WM_PID` and `_NET_WM_HWND` from
  `winex11` with Win32-aware pid / handle semantics for `Ae.solator`
  window-tracking, keep app-side `_WINE_HWND` fallback for donor runtime
  compatibility, and treat `WINE_X11FORCEGLX` as a real `winex11` override
  contract instead of launcher folklore. Do not blindly inherit donor
  `--without-x*` policy from build scripts; compare that donor extension
  disable list against the actual `Ae.solator` X server extension surface
  first.
  On that same Termux-hosted build lane, optional native host providers must
  also stay stamp-visible and bionic-correct:
  do not keep glibc-oriented resolver probes that depend on `_res` /
  `RES_INIT` when the real host is bionic; the resolver configure probe must
  accept the Termux/Android-native `res_init + res_query + ns_initparse`
  surface as `none required` without forcing `-lresolv`.
  Likewise, optional Termux host providers such as `unixodbc` must participate
  in configure-surface drift through a dedicated optional-host stamp, so that
  newly materialized `sql.h` / `libodbc.so` / `odbc.pc` force an honest
  reconfigure before closure instead of leaving the build tree on stale
  "not found" folklore.
- Do not call the lane closed from compile-only proof; runtime/package/app
  proof are mandatory.
- Do not regress the line back to a generic `Proton` substitution story.
