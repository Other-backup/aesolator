# HOLD 2026-03-21 DXSDK Checkpoint

This file freezes the exact continuation point for the current omega pass.
Resume from here without reopening broader scope and without starting a new
build by reflex.

## Frozen State

- The user explicitly requested one single final omega compile.
- That compile already happened successfully:
  `./gradlew --no-daemon testDebugUnitTest assembleDebug`
- The resulting APK was already installed and used for the last live proofs.
- Do not run another Gradle build as the first step when this pass resumes.

## Active Blocker

- The only active product blocker left in this pass is:
  `Prefix Pack -> legacy_dx_sdk -> DXSDK_Jun10.exe`
- Fresh user screenshots proved the state is now narrower than before:
  - the path is no longer just `exe does not start`
  - the lane sometimes reaches the real DirectX SDK installer window
  - it can progress to `Copying Files`
  - then it hangs or falls back into the legacy managed prerequisite path
  - the visible failure text is still the `.NET Framework 2.0 redist`
    `51023` family

## What Is Already Closed

- `Task Manager` is not the active blocker here.
- `Debug / Logs` was already moved onto the same forensic-family surface.
- `Starting up` / loader centering was already corrected in the previous pass.
- Blank helper `cmd` windows are no longer the main failure class for the
  current `legacy_dx_sdk` tail.
- Managed-runtime repair on the live prefix already moved the state forward:
  - live `user.reg` managed overrides were repaired back to `builtin`
  - live `system.reg` `.NET InstallRoot` was corrected manually on device
  - `.NET` lane reached `success`
  - `XNA` lane reached `success`

## Current Code State

- Repo asset already contains a newer unrebuilt DXSDK lane script:
  [install-directx-sdk-tools.cmd](/data/data/com.termux/files/home/aesolator/app/src/main/assets/prefixpack/windows/install-directx-sdk-tools.cmd)
- That script was patched after the single final build.
- Therefore the next continuation step must prefer a live rootfs sync of that
  one script instead of launching a second compile.

## First Step After Resume

1. Restore working wireless `adb`.
2. Copy the patched repo file:
   `app/src/main/assets/prefixpack/windows/install-directx-sdk-tools.cmd`
   into live rootfs:
   `files/imagefs/opt/ae/prefix-pack/windows/install-directx-sdk-tools.cmd`
3. Re-run only the `legacy_dx_sdk` lane.
4. Capture one fresh forensic bundle for that lane only:
   - lane state
   - launcher log
   - `directx-sdk-jun10-install.log`
   - one unlocked runtime screenshot
5. Only after that decide whether the remaining blocker is:
   - the lane script itself
   - the guest-side installer dispatch bridge
   - or another still-missing legacy CLR proof edge

## What Not To Reopen First

- Do not restart donor hunting first.
- Do not reopen system debloat / storage cleanup first.
- Do not reopen already-stable UI families without fresh contradictory proof.
- Do not start from `Prefix Pack` geometry unless a newer screenshot disproves
  the last live state.
- Do not launch a new compile before exhausting the live rootfs sync path.

## Reference Artifacts

- [final_omega_closure_20260321_$(date +%H%M%S)](/data/data/com.termux/files/home/aesolator/out/final_omega_closure_20260321_$(date%20+%25H%25M%25S))
- [final_omega_dxsdk_postsync_20260321_$(date +%H%M%S)](/data/data/com.termux/files/home/aesolator/out/final_omega_dxsdk_postsync_20260321_$(date%20+%25H%25M%25S))
- [SECOND_DEV_ROADMAP.md](/data/data/com.termux/files/home/aesolator/docs/SECOND_DEV_ROADMAP.md)
- [SECOND_DEV_REFLECTIVE_JOURNAL.md](/data/data/com.termux/files/home/aesolator/docs/SECOND_DEV_REFLECTIVE_JOURNAL.md)

