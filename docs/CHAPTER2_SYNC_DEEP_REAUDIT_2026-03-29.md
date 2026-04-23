# Chapter 2 Sync Deep Re-Audit 2026-03-29

## Scope

Re-check the current Chapter 2 sync narrative against:

- live product evidence from `Ae.solator -> WCP -> FreeWine11`
- current local `Aesync` source
- local literature already ingested
- a wide donor set of primary web sources

The goal is not another slogan like "Aesync is high-end".
The goal is to state exactly what is true, what is still inference, and which
claims from older notes need to be tightened.

## Method

This pass used:

- local corpus reread for:
  - `The Linux Programming Interface`
  - `Windows System Internals 7e Part 1`
  - `Windows Internals, Part 2`
  - `Peering Inside the PE`
- current local source for:
  - `dlls/ntdll/unix/aesync.c`
  - `server/aesync.c`
  - `WineSyncPolicy.java`
  - `CHAPTER2_LIVE_FORENSICS_2026-03-29.md`
- primary web sources across Linux, Wine, and Microsoft docs

## Broad Source Base

### Local literature

- [The Linux Programming Interface.txt](/data/data/com.termux/files/home/out/learning-base/embedded-arm/local-intake/chapter2-reading-2026-03-29/the_linux_programming_interface.txt)
- [Windows System Internals 7e Part 1.txt](/data/data/com.termux/files/home/out/learning-base/embedded-arm/local-intake/windows-internals-2026-03-29/windows_system_internals_7e_part_1.txt)
- [Windows Internals, P2.txt](/data/data/com.termux/files/home/out/learning-base/embedded-arm/local-intake/windows-internals-2026-03-29/mark_russinovich_windows_internals_p2.txt)
- [Peering Inside the PE.txt](/data/data/com.termux/files/home/out/learning-base/embedded-arm/local-intake/chapter2-reading-2026-03-29/peering_inside_the_pe_a_tour_of_the_win32_portable_executable_file_format.txt)

### Primary web sources

1. `futex(7)`:
   https://man7.org/linux/man-pages/man7/futex.7.html
2. `futex(2)`:
   https://man7.org/linux/man-pages/man2/futex.2.html
3. `eventfd(2)`:
   https://man7.org/linux/man-pages/man2/eventfd.2.html
4. `Futexes Are Tricky`:
   https://www.akkadia.org/drepper/futex.pdf
5. `Fuss, Futexes and Furwocks`:
   https://www.kernel.org/doc/ols/2002/ols2002-pages-479-495.pdf
6. Linux kernel `ntsync` uAPI:
   https://docs.kernel.org/userspace-api/ntsync.html
7. Wine MR `!7226`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/7226
8. Wine MR `!7815`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/7815
9. Wine MR `!7848`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/7848
10. Wine MR `!8426`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/8426
11. Wine MR `!8445`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/8445
12. Wine MR `!8875`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/8875
13. Wine MR `!9014`:
   https://gitlab.winehq.org/wine/wine/-/merge_requests/9014
14. `README.esync`:
   https://raw.githubusercontent.com/zfigura/wine/esync/README.esync
15. `wine-msync` README:
   https://github.com/marzent/wine-msync
16. Proton changelog:
   https://github.com/ValveSoftware/Proton/wiki/Changelog/7a7fdf5035d4ff64fb493f02a0da0d0555960da3
17. GE-Proton README:
   https://raw.githubusercontent.com/GloriousEggroll/proton-ge-custom/master/README.md
18. `WaitForMultipleObjectsEx`:
   https://learn.microsoft.com/en-us/windows/win32/api/synchapi/nf-synchapi-waitformultipleobjectsex
19. `MsgWaitForMultipleObjectsEx`:
   https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-msgwaitformultipleobjectsex
20. `KeWaitForMultipleObjects`:
   https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/wdm/nf-wdm-kewaitformultipleobjects
21. `Dynamic-Link Library Best Practices`:
   https://learn.microsoft.com/en-us/windows/win32/dlls/dynamic-link-library-best-practices
22. Raymond Chen on loader lock:
   https://devblogs.microsoft.com/oldnewthing/20040128-00/?p=40853
23. Raymond Chen on `CreateThread` from `DllMain`:
   https://devblogs.microsoft.com/oldnewthing/20070904-00/?p=25283

## Re-validated Facts

### 1. `esync` exists because `eventfd` gives multiplexable file descriptors

This part of the older narrative survives.

- `README.esync` is explicit: the main reason is the ability to wait on
  multiple objects through `select/poll/epoll`, something the author says is
  not available with futexes in the same shape.
- `eventfd(2)` matches that design: the object is a kernel counter exposed as
  a file descriptor and can be monitored by `poll`, `select`, and `epoll`.
- `TLPI` independently describes `eventfd()` as a kernel-maintained counter
  exposed through a file descriptor for synchronization.

This means:

- `esync` is not just "FD-based because someone preferred files";
- it is a deliberate answer to multi-object wait multiplexing in userspace.

### 2. `esync` really carries a structural FD ceiling

This also survives re-checking.

- `README.esync` says it creates one `eventfd` per synchronization object.
- `eventfd(2)` documents ordinary file-descriptor resource limits:
  `EMFILE` and `ENFILE`.
- `README.esync` warns directly about "Too many open files".

This is not folklore.
The FD pressure is part of the design cost, not an accidental implementation
detail.

### 3. `fsync` exists because shared-memory plus futex narrows the gap

This survives, but needs stricter wording.

- `futex(2)` and `futex(7)` are explicit that futexes are a shared-memory
  synchronization primitive where uncontended work stays in userspace and the
  kernel arbitrates contended waits.
- `TLPI` says Linux mutexes are implemented with futexes and cites both
  Drepper and the original OLS paper as the deeper references.
- Proton's changelog described `fsync` as "futex-based in-process
  synchronization primitives".

This means:

- `fsync` should be described as a futex/shared-memory backend lane,
  not as a generic "faster esync";
- its design center is different from `esync`.

### 4. `FUTEX_FD` is not the answer and helps explain why `esync` uses `eventfd`

This point was under-emphasized before and should now be treated as part of the
core reflection.

- Drepper states that `FUTEX_FD` was removed and calls it not really usable.
- The same paper explains the old file-descriptor form only as historical
  background.

This sharpens the donor map:

- `eventfd` is not just a random alternative to futex;
- it filled the multiplexable-fd role that raw futexes did not provide in a
  usable upstream form.

### 5. `ntsync` is not "just faster"; it is a dedicated NT object model in the kernel

This is the most important re-check result.

- Linux `ntsync` docs say the interface exists because existing user-space
  tools cannot match Windows performance while offering accurate semantics.
- The same docs expose typed objects:
  semaphore, mutex, auto/manual-reset event.
- Objects are represented by files.
- `/dev/ntsync` is an instance namespace.
- `WAIT_ANY` and `WAIT_ALL` are explicit ioctls with atomic acquisition rules.
- `alert` is an explicit extra event in the wait ABI.
- owner death and abandoned mutex semantics are first-class.
- Wine MR `!7226` mirrors this and explains that each waitable server object
  gets an `inproc_sync` object, cached separately, with fallback only for some
  internal handles.

This means older wording like "Aesync closes the gap to ntsync" is acceptable,
but wording like "Aesync reaches ntsync semantics" is not.

### 6. `queue/msgwait/APC` are not tail noise; they are central to NT wait correctness

This is the second most important re-check result.

- `WaitForMultipleObjectsEx` includes alertable waits, APC completion, and
  warns that GUI threads should use `MsgWaitForMultipleObjectsEx`.
- `MsgWaitForMultipleObjectsEx` includes message queue input, APC wakeups, and
  `WAITALL` semantics that combine objects plus queue state.
- `KeWaitForMultipleObjects` documents APC restrictions, especially once a
  mutex is acquired.
- `Windows System Internals` confirms the `NtWaitForMultipleObjects ->
  ObWaitForMultipleObjects -> USER32 MsgWaitForMultipleObjectsEx` stack and
  alertable APC delivery.
- Wine MR `!8445` says message queues need both inproc sync and server sync.
- Wine MR `!9014` says user APC signaling still needs a dedicated inproc sync.
- Wine MR `!7815` shows that even upstream server sync refactoring had to split
  sync entities first to make `ntsync` integration cleaner.

This invalidates any temptation to treat queue/APC/msgwait as secondary polish.

## Re-audit of Current `Aesync`

### What `Aesync` genuinely does today

Local source confirms all of this:

- app policy is modular from above through
  [WineSyncPolicy.java](/data/data/com.termux/files/home/aesolator/app/src/main/java/com/winlator/cmod/runtimeprofile/WineSyncPolicy.java)
- the userspace lane can enable both `fsync` and `esync` together
- `aesync_wait_objects()` classifies each handle into `fsync`, `esync`, or
  `server`
- mixed `WaitAny` is brokered in
  [aesync.c](/data/data/com.termux/files/home/.push-worktrees/freewine11-head/dlls/ntdll/unix/aesync.c)
  by zero-timeout probes and sliced waits across the three lanes
- mixed `WaitAll` still falls back to server ownership when a real `server`
  lane participates
- `NtSignalAndWaitForSingleObject` is owned, but via lane fallback
- queue/msgwait is coordinated through paired `fsync_msgwait` and
  `esync_msgwait`
- mixed `WaitAll` for `fsync+esync` sets is now owned directly in
  `aesync_wait_objects()` through one shared userspace coordinator word:
  final zero-timeout acquire happens under that coordinator, backend-local
  rewind happens before it is released, and broker/slice sleep only happens
  outside that coordinator
- same-lane `fsync` / `esync` waits and `Aesync`-owned signal-state mutation
  wrappers now honor that same coordinator instead of bypassing the `WaitAll`
  ownership path
- server-side lifecycle ownership for process/thread/queue/APC exists in
  [server/aesync.c](/data/data/com.termux/files/home/.push-worktrees/freewine11-head/server/aesync.c)

### What `Aesync` still is not

The same source, combined with the donor base above, makes the limits clear.

`Aesync` is still not:

- a kernel-backed typed object ABI
- a `/dev/ntsync`-style per-instance namespace
- a per-object file model
- a kernel-atomic cross-lane `WAIT_ALL` that spans every lane
- a true single wait engine with native `alert` semantics
- a single object model spanning all lanes

Concrete current evidence:

- mixed `WaitAll` no longer immediately resolves to
  `aesync_server_wait_objects(...)` when the set stays inside `fsync+esync`;
  it now runs through a split-lane direct coordinator with shared locked
  final acquisition, backend-local rewind, and retry
- that same `WaitAll` path is still not one atomic typed kernel engine: it
  remains a userspace coordinator over separate backend object models, and it
  still falls back to server ownership when a `server` lane participates or a
  backend surface is not locally rewindable
- `aesync_signal_and_wait()` now classifies both sides after pseudo-handle
  normalization and falls back to server before mutation on cross-lane cases,
  but same-lane ownership still delegates to `fsync` / `esync` rather than to
  one new typed wait engine
- `msgwait` is still coordinated by toggling `fsync_msgwait` or
  `esync_msgwait` according to the chosen queue lane, not by one new typed
  backend surface

### Honest verdict

`Aesync` is the strongest current userspace broker we have for the
locked/rootless Chapter 2 line.

It is not a semantic peer of kernel `ntsync`.

The right description is:

- best current Chapter 2 userspace broker
- composite `fsync + esync + server` dispatcher with explicit mixed-wait and
  msgwait ownership
- still below `ntsync` on object-model and atomic multi-object semantics

## Product-Level Re-check of the Live Stall

### What remains true

The older live forensic report still holds on these points:

- direct `explorer.exe` route is real
- runtime selection is correct
- `FEX`, `DXVK`, `VKD3D`, and wrapper route are real
- `wine`, `wineserver`, and `wineboot.exe --init` emerge
- stale `esync` env drift was fixed and `Aesync` now activates correctly

### What must be stated more carefully

The remaining no-window stall should no longer be described loosely as "maybe
sync".

Broad re-audit says:

1. there is no positive evidence that the current live stall is caused by
   missing `ntsync`;
2. there is no positive evidence that `Aesync` policy selection is still wrong;
3. the remaining class lives closer to:
   - bootstrap / `wineboot.exe --init`
   - loader / thread-start / `DllMain` serialization
   - message queue or window-materialization timing
   - Android lifecycle pause

This is still an inference, not a closed proof.
But it is a much tighter inference than before.

The Microsoft loader-lock sources matter here:

- `DllMain` runs under the loader lock;
- waiting or creating threads badly in that path can deadlock or delay startup;
- `WaitForMultipleObjectsEx` is explicitly the wrong primitive for GUI threads
  that create windows, where `MsgWaitForMultipleObjectsEx` semantics matter.

That does **not** prove the current issue is a loader-lock bug.
It proves only that sync-plane blame is weaker than bootstrap/message-pump
owners unless new evidence appears.

## Corrected Hard Rules

1. Do not describe `Aesync` as equivalent to kernel `ntsync`.
   `Aesync` is the best current userspace broker, not semantic parity.
2. Do not collapse donor logic.
   `esync` is the multiplexable-`eventfd` lane;
   `fsync` is the futex/shared-memory lane;
   `ntsync` is the dedicated NT object-model lane.
3. Do not treat `queue/msgwait/APC` as optional polish.
   They are part of the wait model itself.
4. Do not blame the sync backend for a no-window bootstrap until
   `loader/msgwait/bootstrap/lifecycle` owners are actively ruled out.
5. Do not call mixed `WaitAll` solved just because ownership exists.
   If the implementation still falls back to server for the atomic operation,
   say so explicitly.

## Current Chapter 2 Verdict

- `Aesync` policy in `Ae.solator` is now correctly modular and correctly gated
  to the Chapter 2 `FreeWine11` runtime line.
- `Aesync` source implementation is strong enough to keep as the userspace
  closure target on locked/rootless Android.
- `Aesync` must still be documented as a composite broker below `ntsync`.
- The current integrated live stall is more likely to be
  `bootstrap/window-materialization/lifecycle` than sync-policy selection.

## Post Re-Audit Source Batch

After this re-audit, the implementation was tightened in two concrete places:

- cross-lane `NtSignalAndWaitForSingleObject` no longer chains
  `fsync -> esync -> server` blindly;
  `Aesync` now classifies both sides first and falls back to server before any
  mutation if the signal and wait objects live on different lanes
- mixed `WaitAny` no longer starts from a hard-coded
  `server -> fsync -> esync` bias;
  it now sorts active lanes by the lowest original handle index and uses that
  order for the zero-timeout sweep, with rotated slice waits afterward

This does not give `ntsync` parity.
It does remove one real correctness hazard in cross-lane `signal_and_wait` and
one arbitrary backend-class bias in mixed `WaitAny`.

After the next integrated broker pass, the implementation tightened again:

- mixed `WaitAny` no longer risks sleeping indefinitely when pollable carrier
  lanes are mixed with a non-pollable `server` lane;
  the per-thread `epoll` broker now uses periodic `timerfd` slice wakeups and
  authoritative resweeps while a server lane participates
- broker coverage is now wider than the original `esync`-only pollable set:
  runtime-provided `fsync` carrier fds now feed the broker too
- `aesync_signal_and_wait()` now normalizes pseudo-handles before
  classification, so pseudo-handle signal/wait paths stay inside the
  integrated `Aesync` contract instead of failing lane classification early

This still does not give `ntsync` parity.
It does close the strongest remaining mixed-wait liveness hole in the current
userspace broker and removes one more correctness gap at the API boundary.

After the direct-call flattening pass, the implementation tightened again:

- `Aesync` classification no longer raw-probes `get_fsync_idx` /
  `get_esync_fd` directly for hot lane discovery;
  it now reuses backend-local wait-fd/type helpers, so lane/type discovery and
  broker setup share one object-surface bootstrap path
- `fsync` now keeps a persistent local carrier-fd cache instead of reopening a
  server-provided carrier fd on each broker registration or pulse path
- `Aesync` broker now consumes borrowed cached `fsync` carrier fds instead of
  per-wait duplicate/close churn, making the userspace path closer to the
  `ntsync` shape of "bootstrap once, wait locally"
- `Aesync` classification cache now retains backend type alongside lane and
  msgwait state, so direct mixed `WaitAll` bootstrap stops immediately
  re-probing `fsync` / `esync` type on the same hot handle just to rebuild
  local metadata
- alertable broker waits now register both APC carrier fds when both backends
  are active, matching the existing server-side composite APC wake path rather
  than choosing only the first available APC fd

This still does not create a kernel object ABI.
It does materially reduce repeated server traffic and local carrier churn in
the hottest mixed-wait path, which is the closest transferable form of
`ntsync`'s direct-kernel wait model on the current rootless line.

## Local Source Anchors

- [WineSyncPolicy.java](/data/data/com.termux/files/home/aesolator/app/src/main/java/com/winlator/cmod/runtimeprofile/WineSyncPolicy.java)
- [aesync.c](/data/data/com.termux/files/home/.push-worktrees/freewine11-head/dlls/ntdll/unix/aesync.c)
- [server/aesync.c](/data/data/com.termux/files/home/.push-worktrees/freewine11-head/server/aesync.c)
- [CHAPTER2_LIVE_FORENSICS_2026-03-29.md](/data/data/com.termux/files/home/aesolator/docs/CHAPTER2_LIVE_FORENSICS_2026-03-29.md)
