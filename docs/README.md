# Docs Index (Aeolator)

This repo stores only application-layer documentation.

## Current State

- App source-of-truth: `aeolator`
- Runtime source-of-truth: `freewine11`
- Archive/release host: `wcp-runtime-lanes`
- Graphics/provider lane host: `wcp-graphics-lanes`
- User-facing package provenance must stay explicit (`WCP Archive` vs `WCPHub`)

## Source Of Truth

- `docs/REPO_SPLIT_TOPOLOGY.md` - split repository contract and ownership.
- `docs/CONTENTS_QA_CHECKLIST.md` - active Contents/UI closure checklist.
- `docs/ADB_HARVARD_DEVICE_FORENSICS.md` - device forensic runbook.
- `docs/ADB_WIFI_DEBUG.md` - local Termux Wi-Fi ADB pairing, reconnect, install, and launch helper runbook.
- `docs/AEOLATOR_FORENSIC_SYNC_CONTRACT.md` - forensic sync contract for app/runtime diagnostics.
- `docs/TERMUX_LOCAL_BUILD.md` - verified Termux ARM64 local build and Wi-Fi ADB install runbook.
- `docs/DONOR_REFLECTIVE_ROADMAP.md` - donor extraction roadmap with pre/during/post reflective gates.
- `docs/DONOR_ROUND_QUEUE.md` - strict donor execution queue (`1 round = 1 donor`) and closure checklist.
- `docs/R10_R14_STATUS_SNAPSHOT.md` - normalized closure state for frame-generation donor rounds `R10-R14` (`integrated in tree` vs `still gate_hold`).
- `docs/DUAL_DEV_OPERATING_MODEL.md` - mixed-mode two-Codex operating contract for runtime owner vs app owner.
- `docs/ACTIVE_OWNERSHIP_MAP.md` - live ownership split for active work areas and overlap policy.
- `docs/rounds/R1_GAMENATIVE_MATRIX.md` - Round 1 donor transfer matrix (`GameNative`) currently in `gate_hold`.
- `docs/rounds/R1_GAMENATIVE_FILE_COVERAGE.md` - file-level coverage control for full donor exhaustion in Round 1.
- `docs/rounds/R1_GAMENATIVE_GATE.md` - Round 1 gate checklist (`gate -> closed`) and mandatory CI/runtime checks.
- `docs/rounds/R2_MICEWINE_MATRIX.md` - Round 2 donor transfer matrix (`MiceWine-Application`), closed.
- `docs/rounds/R2_MICEWINE_FILE_COVERAGE.md` - file-level coverage control for Round 2 donor sweep, closed.
- `docs/rounds/R3_UMU_MATRIX.md` - Round 3 donor transfer matrix (`umu-launcher`), closed.
- `docs/rounds/R3_UMU_FILE_COVERAGE.md` - file-level coverage control for Round 3 donor sweep, closed.
- `docs/rounds/R4_EXAGEAR_MATRIX.md` - Round 4 donor transfer matrix (`ExagearAndroidX11Server`), closed.
- `docs/rounds/R4_EXAGEAR_FILE_COVERAGE.md` - file-level coverage control for Round 4 donor sweep, closed.
- `docs/rounds/R5_MOBOX_MATRIX.md` - Round 5 donor transfer matrix (`mobox`), closed.
- `docs/rounds/R5_MOBOX_FILE_COVERAGE.md` - file-level coverage control for Round 5 donor sweep, closed.
- `docs/rounds/R6_TERMUXX11_MATRIX.md` - Round 6 donor transfer matrix (`termux-x11-fork`), closed.
- `docs/rounds/R6_TERMUXX11_FILE_COVERAGE.md` - file-level coverage control for Round 6 donor sweep, closed.
- `docs/rounds/R7_WINLATORFORK_MATRIX.md` - Round 7 donor transfer matrix (`winlator-fork`), closed.
- `docs/rounds/R7_WINLATORFORK_FILE_COVERAGE.md` - file-level coverage control for Round 7 donor sweep, closed.
- `docs/rounds/R8_COFFINCOLORS_MATRIX.md` - Round 8 donor transfer matrix (`coffincolors/winlator`), closed.
- `docs/rounds/R8_COFFINCOLORS_FILE_COVERAGE.md` - file-level coverage control for Round 8 donor sweep, closed.
- `docs/rounds/R9_GAMEHUBAPK_MATRIX.md` - Round 9 donor transfer matrix (`GameHub-Lite-5.3.3-RC2.apk`), closed.
- `docs/rounds/R9_GAMEHUBAPK_FILE_COVERAGE.md` - file-level coverage control for Round 9 APK donor sweep, closed.
- `docs/rounds/R10_MOBFGSR_MATRIX.md` - Round 10 donor transfer matrix (`Mob-FGSR/MobFGSR`), `gate_hold`.
- `docs/rounds/R10_MOBFGSR_FILE_COVERAGE.md` - file-level coverage control for Round 10 donor sweep, `gate_hold`.
- `docs/rounds/R11_LINUXFG_MATRIX.md` - Round 11 donor transfer matrix (`xXJSONDeruloXx/linux-fg`), `gate_hold`.
- `docs/rounds/R11_LINUXFG_FILE_COVERAGE.md` - file-level coverage control for Round 11 donor sweep, `gate_hold`.
- `docs/rounds/R12_DLSSGTOFSR3_MATRIX.md` - Round 12 donor transfer matrix (`Nukem9/dlssg-to-fsr3`), `gate_hold`.
- `docs/rounds/R12_DLSSGTOFSR3_FILE_COVERAGE.md` - file-level coverage control for Round 12 donor sweep, `gate_hold`.
- `docs/rounds/R13_FRAMEGENAPP_MATRIX.md` - Round 13 donor transfer matrix (`proqaz2-design/Frame-generation-`), `gate_hold`.
- `docs/rounds/R13_FRAMEGENAPP_FILE_COVERAGE.md` - file-level coverage control for Round 13 donor sweep, `gate_hold`.
- `docs/rounds/R14_OPTISCALER_MATRIX.md` - Round 14 donor transfer matrix (`optiscaler/OptiScaler`), `gate_hold`.
- `docs/rounds/R14_OPTISCALER_FILE_COVERAGE.md` - file-level coverage control for Round 14 donor sweep, `gate_hold`.
- `docs/rounds/R15_EDENAPK_MATRIX.md` - Round 15 donor transfer matrix (`Eden-Android-9d2341eaea-standard.apk`), `closed`.
- `docs/rounds/R15_EDENAPK_FILE_COVERAGE.md` - file-level coverage control for Round 15 donor sweep, `closed`.
- `docs/rounds/R15_EDENAPK_HEX_ASM_NOTES.md` - IDE/HEX/ASM extraction notes and transfer boundary decisions for Round 15.
- `docs/DONOR_REFLECTIVE_ROUND2_GAPS.md` - historical second-pass snapshot (legacy baseline).
- `docs/TASK_MANAGER_WINMONITOR_CONTRACT.md` - task manager alignment contract against extracted `winmonitor` schema.
- `docs/GAMENATIVE_X11_RENDERER_DRIVER_AUDIT.md` - focused donor audit for `GameNative` X11 / renderer / driver layers.
- `docs/DEVICE_MIGRATION_BOOTSTRAP.md` - migration bootstrap path for bringing the full `Ae.solator` workspace onto a new `Termux/Android` device without reopening environment tails.
- `docs/GAMENATIVE_FULL_TRANSFER_MATRIX.md` - full donor transfer matrix for `GameNative` runtime, payload, container, launcher, Wine/Proton, and renderer lanes.
- `docs/GAMENATIVE_RUNTIME_GAP_INVENTORY.md` - current donor runtime/code gap status before the first honest compile.
- `docs/GAMENATIVE_LIBWINLATOR11_SOURCE_AUDIT.md` - source-backed reconstruction status for donor `libwinlator_11.so` behavior.
- `docs/GAMENATIVE_SECOND_SWEEP_INVENTORY.md` - second broad donor sweep outside `com/winlator/*`, focused on storefront/external-display/Compose/rootfs delivery lanes.
- `docs/IMAGEFS_HYBRID_PLAN.md` - hybrid rootfs plan for combining the freshest `GameNative` UbuntuFS/imagefs pieces with `Ae.solator` runtime and forensic contracts.
- `docs/IMAGEFS_LAYER_OWNERSHIP_TABLE.md` - explicit layer ownership map for local/donor `imagefs_*`, overlays, prefix/audio layers, and archived orphan residue such as `imagefs.txz.02`.
- `docs/IMAGEFS_PER_LIBRARY_ADOPTION_TABLE.md` - donor-rootfs-first per-library adoption map for Vulkan/OpenGL/Pulse/sysvshm/base tools, overlays, helper libs, compat bridges, and legacy hold lanes.
- `docs/ROOTFS_RUNTIME_STATIC_AUDIT.md` - pre-compile audit of donor-rootfs-first wiring, legacy `imagefs` baggage, `glibc`/`bionic` split, and `Contents` runtime resolution blockers.
- `docs/PAYLOAD_ROOTFS_STATIC_INVENTORY.md` - post-patch static inventory of rootfs/payload closure before the first honest compile.

## Generated In CI

- `docs/WINLATOR_LUDASHI_REFLECTIVE_ANALYSIS.md`
