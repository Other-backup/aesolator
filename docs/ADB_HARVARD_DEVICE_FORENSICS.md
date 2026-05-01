# ADB Harvard Device Forensics

This runbook defines the device-side automation loop for the Winlator matrix:

1. capture a single-issue bundle by default,
2. seed/clone container matrix when comparison is needed,
3. refresh latest artifact payloads when payload drift is suspected,
4. run the full forensic matrix,
5. classify runtime mismatch vs baseline,
6. export per-scenario forensic bundles.

## Master Engineering Directive

Device forensics follows `docs/MASTER_ENGINEERING_DIRECTIVE.md`.

## No-ADB fallback (current migration lane)

When `adb` is unavailable, do not block forensic closure work. Use the
app-owned external forensic outputs as the primary source:

- `/storage/emulated/0/Ae.solator/logs/forensics/*.jsonl`
- `/storage/emulated/0/Ae.solator/logs/fatal_crash_*.txt`
- runtime stream files under `/storage/emulated/0/Ae.solator/logs/`

In this mode, the user provides exported bundles/screenshots after autonomous
installs, and repository-side closure continues from those artifacts plus
code-path audits.
Forensic logs are evidence portals, not the target: if a runtime/app/device
defect can be fixed in source, config, package routing, or tooling, the agent
must apply the systemic fix and verify it instead of only describing the
finding.
For matrix harvests, wait for the complete capture before declaring the active
frontier unless the harness itself is broken.

## Scripts

- `ci/winlator/forensic-adb-issue-capture.sh`
  - default one-shot capture for a single broken container/session; collects a
    full ADB bundle, app-private `run-as` evidence, fresh runtime logs, and the
    parser outputs needed for first-pass root-cause work.
- `ci/winlator/adb-container-seed-matrix.sh`
  - clones `xuser-N` trees from a seed container and rewrites `.container` metadata (`id`, `name`, profile keys).
- `ci/winlator/adb-ensure-artifacts-latest.sh`
  - downloads latest WCP artifacts from `ci/winlator/artifact-source-map.json` and installs into app-private `files/contents` using `run-as`.
- `ci/winlator/forensic-adb-harvard-suite.sh`
  - orchestrates complete matrix capture, mismatch analysis, extra dumpsys/psi snapshots, and bundle export.
- `ci/winlator/forensic-adb-arm64-autotune-matrix.sh`
  - validates arm64 autotune contract end-to-end on device: SoC class detection, expected profile (`conservative/balanced/aggressive`), and env matrix from runtime log markers.
- `ci/winlator/forensic-adb-core-upscale-loop.sh`
  - non-breaking single-runtime loop for Warcraft III / upscale black-screen work (`freewine11`, baseline `freewine11`).
- `ci/winlator/adb-network-source-diagnostics.sh`
  - probes active source endpoints (GitHub releases/raw for `aesolator`, `wcp-runtime-lanes`, `wcp-graphics-lanes`) from device context and captures proxy/private-DNS/connectivity diagnostics for VPN triage.

## Quick Start

```bash
# 0) Single-issue capture (default for one broken session)
bash ci/winlator/forensic-adb-issue-capture.sh \
  --serial <device> \
  --package com.winlator.cmod

# 1) Seed matrix (optional)
ADB_SERIAL=<device> \
WLT_PACKAGE=com.winlator.cmod \
WLT_SEED_CONTAINER_ID=1 \
WLT_TARGET_CONTAINERS="1" \
WLT_CONTAINER_PROFILE_MAP="1:wineVersion=freewine11-arm64ec-1;runtimeProfile=S8G1_SUPER" \
bash ci/winlator/adb-container-seed-matrix.sh

# 2) Refresh artifacts (optional)
ADB_SERIAL=<device> \
WLT_PACKAGE=com.winlator.cmod \
WLT_TARGET_KEYS="freewine11 vulkansdkarm64 vulkansdkx86_64 aedxvkgplasync aedxvkgplasyncarm64ec aevkd3dproton aevkd3dprotonarm64ec" \
bash ci/winlator/adb-ensure-artifacts-latest.sh

# 3) Full suite
ADB_SERIAL=<device> \
WLT_PACKAGE=com.winlator.cmod \
WLT_SCENARIO_MATRIX="freewine11:1" \
WLT_BASELINE_LABEL=freewine11 \
WLT_RUN_SEED=0 \
WLT_RUN_ARTIFACT_REFRESH=0 \
WLT_FAIL_ON_SEVERITY_AT_OR_ABOVE=medium \
WLT_FAIL_ON_CONFLICT_SEVERITY_AT_OR_ABOVE=medium \
bash ci/winlator/forensic-adb-harvard-suite.sh

# 4) Arm64 autotune matrix validation (SoC/profile/env)
ADB_SERIAL=<device> \
WLT_PACKAGE=com.winlator.cmod \
WLT_CONTENT_NAME=freewine11-arm64ec-1 \
WLT_REQUESTED_PROFILE=auto \
WLT_CONTAINER_ID=1 \
bash ci/winlator/forensic-adb-arm64-autotune-matrix.sh

# 5) Core upscale / Warcraft III loop
ADB_SERIAL=<device> \
WLT_PACKAGE=com.winlator.cmod \
WLT_SCENARIO_MATRIX="freewine11:1" \
WLT_BASELINE_LABEL=freewine11 \
WLT_ARTIFACT_KEYS="freewine11" \
bash ci/winlator/forensic-adb-core-upscale-loop.sh
```

## Outputs

`forensic-adb-issue-capture.sh` writes a timestamped bundle under the default
issue output root and includes:

- clean-session `logcat -b all`,
- `run-as` app-private evidence,
- pulled runtime stream files,
- `dumpsys` surfaces,
- `runtime-log-assembler` outputs,
- bundle markdown/index artifacts.

`forensic-adb-harvard-suite.sh` writes to `WLT_OUT_DIR`:

- scenario folders (`<label>/...`) from `forensic-adb-complete-matrix.sh`,
- per-scenario `runtime-log-assembler.{tsv,json,summary.txt,md}`,
- `runtime-mismatch-matrix.{tsv,md,json,summary.txt}`,
- `runtime-conflict-contour.{tsv,md,json,summary.txt}`,
- `network/endpoint-probes.tsv` + `network/endpoint-probes.summary.json` (when `WLT_CAPTURE_NETWORK_DIAG=1`),
- `bundles/index.tsv` + `bundles/index.json`,
- per-scenario zips (`bundles/<label>.zip`) when `WLT_BUNDLE_MODE=per_scenario|both`,
- optional full bundle zip when `WLT_BUNDLE_MODE=single|both`.

## Notes

- For a single runtime/container failure, do the issue capture first and only
  widen into the full Harvard suite after the first parser pass.
- `WLT_FAIL_ON_SEVERITY_AT_OR_ABOVE` propagates mismatch classifier exit thresholds (`off|info|low|medium|high`).
- `WLT_FAIL_ON_CONFLICT_SEVERITY_AT_OR_ABOVE` applies the same threshold contract to strict runtime logging contour (`RUNTIME_SUBSYSTEM_SNAPSHOT`, `RUNTIME_LOGGING_CONTRACT_SNAPSHOT`, `RUNTIME_LIBRARY_COMPONENT_*`).
- `WLT_CAPTURE_CONFLICT_LOGS=1` (default in `forensic-adb-complete-matrix.sh`) writes per-scenario `logcat-runtime-conflict-contour.txt` + `runtime-conflict-contour.summary.txt`.
- `forensic-runtime-log-assembler.py` merges `logcat`,
  `forensics-jsonl-tail`, and `/storage/emulated/0/Ae.solator/logs/*` into a
  single per-scenario low-level report for loader/assert/ABI failures.
- `WLT_CAPTURE_NETWORK_DIAG=1` runs source endpoint diagnostics before scenario launches.
- network summary includes `problemEndpoints` (non-zero curl status / HTTP 4xx-5xx / code 000) for quick outage triage.
- For unstable VPN/DNS conditions, run artifact refresh first and then launch suite with `WLT_RUN_ARTIFACT_REFRESH=0`.
- The maximum non-root contract also includes privilege/appops/whitelist state
  and a `run-as` tree walk through the staged runtime files when loader / ABI /
  path mismatches are suspected.
- When unix-side Wine modules fail after bootstrap, inspect pulled ELF
  `RUNPATH` / `RPATH` early; donor absolute app-private paths such as
  `/data/data/app.gamenative/files/imagefs/usr/lib` can survive inside the
  runtime payload and masquerade as later `winex11.drv` / `nodrv_CreateWindow`
  failures.
- Keep the wide suite baseline on `freewine11` (single active runtime lane).
