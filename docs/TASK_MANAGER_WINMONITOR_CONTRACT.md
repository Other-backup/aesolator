# Task Manager WinMonitor Contract (Aeolator)

Generated: `2026-03-05`

## Goal

Align Aeolator Task Manager runtime contract with extracted `com/winemu/core/server/winmonitor` model (APK donor lane), without breaking existing WinHandler packet protocol.

## Current Aeolator Baseline (Already Present)

Source:
- `app/src/main/java/com/winlator/cmod/winhandler/TaskManagerDialog.java`
- `app/src/main/java/com/winlator/cmod/winhandler/WinHandler.java`
- `app/src/main/java/com/winlator/cmod/winhandler/ProcessInfo.java`

Implemented today:
- Windows tab + Linux Runtime tab.
- 1s live refresh loop.
- Windows process list from WinHandler (`LIST_PROCESSES/GET_PROCESS` path).
- Process actions: affinity, bring-to-front, terminate.
- Architecture lane filters (`WoW64`, `ARM64EC`, `Native`).
- Linux telemetry (`cpu/memory/io/fd/sockets/psi/load/net`).
- Window correlation via XServer (`pid -> window`) with details panel.
- `ProcessInfo.path` prepared in Aeolator model/UI (currently optional/future-populated).
- Windows process details enriched with live `/proc` runtime metrics (`cpu`, `threads`, `io`, `state`, `cmd`) for the same PID.
- Windows details now include thread preview (`TID`, thread name, thread state, scheduling priority/nice) via `/proc/<pid>/task`.
- Windows process search now includes `name/path/pid/arch/window title/window class`.
- `WinHandler` receive path hardened for process telemetry:
  - datagram max length reset before each `receive()`
  - `ByteBuffer.limit(receivedLength)` applied before decode
  - `GET_PROCESS` minimum-size guard (`remaining() >= 57`)
  - receive datagram capacity expanded (`2048`) so optional `path` tails are not truncated at legacy `64` bytes
- Task Manager now emits periodic refresh-cycle forensic markers:
  - `TASKMGR_REFRESH` with lane (`windows`/`linux`), `visible/total` counters, and `path_support` signal (`present`/`legacy`).

## Extracted WinMonitor Schema (APK)

Source package:
- `.../jadx/sources/com/winemu/core/server/winmonitor/*`

Observed data model:
- `ProcessInfo`: `name`, `pid`, `path`
- `ProcessListResponse`: `status`, `data[]`, `message`
- `ThreadInfo`: `thread_id`, `owner_pid`, `priority`, `name`
- `ThreadListResponse`: `status`, `data[]`, `message`
- `StackFrame`: `address`, `module`, `symbol`
- `ThreadDetailInfo`: thread fields + `stack_frames[]`
- `ThreadStackResponse`: `status`, `data`, `message`
- `CommandRequest`: `command`, optional `pid`, optional `thread_id`
- `KillProcessResponse`: `status`, `success`, `message`

Additional DEX-level signals (string scan):
- `Lcom/winemu/core/embedded/WinMonitorEmbedded;`
- `Lcom/winemu/core/server/winmonitor/WinMonitorClient;`
- `WINMONITOR_PORT`
- `winmonitor.exe`
- `thread_id`, `owner_pid`, `stack_frames`
- `UNEXP_KILL_PROCESS`, `UNEXP_REASON_KILL_PROCESS`

Single-class extraction (jadx targeted run):
- `jadx-single-winmonitor/sources/com/winemu/core/server/winmonitor/WinMonitorClient.java`
  - constructor takes monitor port (`int`)
  - transport uses `DatagramSocket`
  - socket timeout set to `5000 ms`
  - JSON codec uses `Gson`
- `jadx-single-winmonitor-embedded/sources/com/winemu/core/embedded/WinMonitorEmbedded.java`
  - contains embedded `winmonitor.exe` payload as Base64 chunks
  - payload includes Windows-side diagnostic symbols (`StackWalk64`, `SymFromAddr`, `Process32First/Next`, `Thread32First/Next`, `TerminateProcess`, etc.)

## Delta Matrix (What To Add)

1. Process path
- Current: optional `path` field is already present in Aeolator model/UI.
- Target: complete runtime-side emission of path for all process lanes.

2. Thread list per process
- Current: lightweight thread preview (top N threads) is present in Windows details.
- Target: full thread table + targeted controls/stack fetch when runtime capability exists.

3. Stack trace preview
- Current: absent.
- Target: optional stack frame section (`module!symbol @ address`) behind explicit refresh action.

4. Structured command envelope
- Current: binary request codes (`kill`, `list`, `affinity`, `bring_to_front`).
- Target: keep binary protocol as primary, introduce internal command envelope adapter so forensic can log uniform request semantics.

5. Response status/message normalization
- Current: implicit success/failure per request path.
- Target: normalized status + message mapping into forensic timeline.

## Compatibility Rules

- Do not break current WinHandler packet framing.
- New fields must be optional and feature-gated.
- If thread/stack endpoints are unavailable in runtime, UI must show `N/A` without errors.
- Forensic entries must include source lane (`winhandler`, `linux-telemetry`, `xserver`) and timestamp.

## Integration Order

1. Add model-layer optional fields (`path`, `threads`, `stackFrames`) in UI-side DTOs only.
2. Add forensic event schema for task-manager actions and refresh cycles.
3. Add runtime capability probe and UI feature gates.
4. Add thread/stack UI panels only after capability probe is stable.
5. Keep no-regression checks on current Windows+Linux tabs before enabling new panels by default.

## Risk Notes

- APK full decompile ended with OOM at ~65%, but all `winmonitor` model classes were extracted and are sufficient for contract planning.
- Kotlin metadata decode errors in these classes are expected from obfuscation; field names from `@SerializedName` remain usable.
