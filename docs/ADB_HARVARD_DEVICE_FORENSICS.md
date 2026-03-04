# ADB Harvard Device Forensics

This runbook defines the device-side automation loop for the Winlator matrix:

1. seed/clone container matrix,
2. refresh latest artifact payloads,
3. run full forensic matrix,
4. classify runtime mismatch vs baseline,
5. export per-scenario forensic bundles.

## Scripts

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
# 1) Seed matrix (optional)
ADB_SERIAL=<device> \
WLT_PACKAGE=by.aero.so.benchmark \
WLT_SEED_CONTAINER_ID=1 \
WLT_TARGET_CONTAINERS="1" \
WLT_CONTAINER_PROFILE_MAP="1:wineVersion=freewine11-arm64ec-1;runtimeProfile=S8G1_SUPER" \
bash ci/winlator/adb-container-seed-matrix.sh

# 2) Refresh artifacts (optional)
ADB_SERIAL=<device> \
WLT_PACKAGE=by.aero.so.benchmark \
WLT_TARGET_KEYS="freewine11 vulkansdkarm64 vulkansdkx86_64 aedxvkgplasync aedxvkgplasyncarm64ec aevkd3dproton aevkd3dprotonarm64ec" \
bash ci/winlator/adb-ensure-artifacts-latest.sh

# 3) Full suite
ADB_SERIAL=<device> \
WLT_PACKAGE=by.aero.so.benchmark \
WLT_SCENARIO_MATRIX="freewine11:1" \
WLT_BASELINE_LABEL=freewine11 \
WLT_RUN_SEED=0 \
WLT_RUN_ARTIFACT_REFRESH=0 \
WLT_FAIL_ON_SEVERITY_AT_OR_ABOVE=medium \
WLT_FAIL_ON_CONFLICT_SEVERITY_AT_OR_ABOVE=medium \
bash ci/winlator/forensic-adb-harvard-suite.sh

# 4) Arm64 autotune matrix validation (SoC/profile/env)
ADB_SERIAL=<device> \
WLT_PACKAGE=by.aero.so.benchmark \
WLT_CONTENT_NAME=freewine11-arm64ec-1 \
WLT_REQUESTED_PROFILE=auto \
WLT_CONTAINER_ID=1 \
bash ci/winlator/forensic-adb-arm64-autotune-matrix.sh

# 5) Core upscale / Warcraft III loop
ADB_SERIAL=<device> \
WLT_PACKAGE=by.aero.so.benchmark \
WLT_SCENARIO_MATRIX="freewine11:1" \
WLT_BASELINE_LABEL=freewine11 \
WLT_ARTIFACT_KEYS="freewine11" \
bash ci/winlator/forensic-adb-core-upscale-loop.sh
```

## Outputs

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

- `WLT_FAIL_ON_SEVERITY_AT_OR_ABOVE` propagates mismatch classifier exit thresholds (`off|info|low|medium|high`).
- `WLT_FAIL_ON_CONFLICT_SEVERITY_AT_OR_ABOVE` applies the same threshold contract to strict runtime logging contour (`RUNTIME_SUBSYSTEM_SNAPSHOT`, `RUNTIME_LOGGING_CONTRACT_SNAPSHOT`, `RUNTIME_LIBRARY_COMPONENT_*`).
- `WLT_CAPTURE_CONFLICT_LOGS=1` (default in `forensic-adb-complete-matrix.sh`) writes per-scenario `logcat-runtime-conflict-contour.txt` + `runtime-conflict-contour.summary.txt`.
- `forensic-runtime-log-assembler.py` merges `logcat`, `forensics-jsonl-tail`, and `/sdcard/Winlator/logs/*` into a single per-scenario low-level report for loader/assert/ABI failures.
- `WLT_CAPTURE_NETWORK_DIAG=1` runs source endpoint diagnostics before scenario launches.
- network summary includes `problemEndpoints` (non-zero curl status / HTTP 4xx-5xx / code 000) for quick outage triage.
- For unstable VPN/DNS conditions, run artifact refresh first and then launch suite with `WLT_RUN_ARTIFACT_REFRESH=0`.
- Keep the wide suite baseline on `freewine11` (single active runtime lane).
