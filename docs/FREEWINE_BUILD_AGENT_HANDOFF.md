# FreeWine Build Agent Handoff

Updated: `2026-03-14`

## Pending External Review

### Valve Wine commit for FreeWine builder

- Link:
  `https://github.com/ValveSoftware/wine/commit/6ccff11d0e7d620cd958b56b0904fcbd9a9bfb26`
- Target consumer:
  second agent / build lane responsible for `freewine11`
- Why this was captured:
  user explicitly flagged it as a likely useful upstream change for the
  `freewine` build flow and asked that it be forwarded into the build stream
  rather than lost inside the app/UI backlog.

### Full compare: Valve `proton_10.0` vs `GameNative/proton-wine:proton_10.0`

- Compare:
  `https://github.com/ValveSoftware/wine/compare/proton_10.0...GameNative:proton-wine:proton_10.0`
- Compare status on `2026-03-14`:
  `ahead_by=3`, `behind_by=0`, `total_commits=3`, `files_changed=70`
- Merge base:
  `b8fdff8e1f855b5276ec4ddca0f31b2792554322`

#### Commits in compare

1. `2d7b60b63d0f02f984c198f67406b3d289e5dcd4`
   `feature: add android support`
2. `6ccff11d0e7d620cd958b56b0904fcbd9a9bfb26`
   `Activates test-bylaws patches for ARM64EC builds`
3. `120d9174d70fbe7aedc473bb8bb0569729244417`
   `Reverts and patches winemenubuilder for Winlator`

#### Structural impact

- `55` files under `android/patches`
- `8` files under `android/android_sysvshm`
- new workflow/build automation under `.github/workflows`
- new local build entrypoints under `build-scripts/`
- `configure.ac` touched

#### What this downstream stack appears to add

- Android support scaffolding for the Wine tree
- dedicated `android_sysvshm` implementation and integration docs
- Android patch-set for networking, clipboard, winebrowser, winepulse,
  winebus, winex11, wow64, loader, explorer, wineboot, and MIDI
- ARM64EC-oriented `test-bylaws` patch activation
- Winlator-oriented `winemenubuilder` patch management
- build scripts for both `arm64ec` and `x86_64`
- workflow automation for artifact output

#### FreeWine-specific implication

- This is not a single cherry-pick candidate but a downstream Android/Winlator
  integration layer on top of `proton_10.0`.
- The `6ccff11` commit is only one slice of that layer; the build agent should
  evaluate it together with:
  `2d7b60b` for Android support/bootstrap and
  `120d917` for Winlator shortcut behavior.
- Highest-risk area for `freewine11` review:
  ARM64EC runtime behavior, wow64/syscall/threading patches, and whether the
  `test-bylaws` group conflicts with the current `freewine11` patch stack.

### Confirmed Proton artifact signal from `The412Banner/Nightlies`

- Release:
  `https://github.com/The412Banner/Nightlies/releases/tag/proton-bleeding-edge-20260312-b310f0c-run23`
- Release name:
  `Proton bleeding-edge ARM64EC (20260312)`
- Published:
  `2026-03-14T07:43:29Z`
- GitHub prerelease flag:
  `true`

#### Assets observed

- `proton-proton-bleeding-edge-20260312-b310f0c-arm64ec.wcp`
- `proton-proton-bleeding-edge-20260312-b310f0c-arm64ec.wcp.sha256`
- `proton-wine-proton-bleeding-edge-20260312-b310f0c-arm64ec.wcp.xz`
- `proton-wine-proton-bleeding-edge-20260312-b310f0c-arm64ec.wcp.xz.sha256`

#### Why this matters

- This is hard evidence that `Nightlies` is carrying packaged `Proton`
  runtimes, not just graphics or Box64 artifacts.
- The file naming shows two relevant packaging shapes:
  direct `.wcp` and compressed `.wcp.xz`, both ARM64EC-oriented.
- Any future `Ae.solator` donor lane or `freewine11` review must treat
  `Proton` as a first-class runtime stream in `The412Banner/Nightlies`, not as
  a hypothetical feed.

## Expected Follow-up In FreeWine Lane

- inspect what this upstream Wine commit changes;
- decide whether it applies cleanly to the current `freewine11` branch/base;
- assess whether it affects `arm64ec`, packaging, startup stability, or runtime
  compatibility for `Ae.solator`;
- treat the compare as a 3-commit downstream bundle, not as an isolated
  one-off patch;
- inspect `android/patches/test-bylaws/*`, `build-scripts/build-step-arm64ec.sh`,
  `build-scripts/build-step-x86_64.sh`, and the `winemenubuilder` patch path
  before deciding on cherry-pick vs selective port;
- record outcome back into the shared handoff/journal contract.

## Current Limitation

- The local workspace currently contains `aesolator`, but not a checkout of the
  `freewine11` build repository itself, so this handoff is recorded here as a
  cross-repo action item rather than applied directly in code.
