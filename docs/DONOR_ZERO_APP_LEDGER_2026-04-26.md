# Ae.solator Donor-Zero App Ledger

Date: `2026-04-26`

Scope:
- Repository: `aesolator`.
- Excluded in this batch: `freewine11`, Wine/Proton source trees, and `wcp-runtime-lanes`.
- Rule: no intermediate APK/build gate until the donor-zero app batch reaches a written closure point.

## Source Frontier

Canonical frontier cache:
`/data/data/com.termux/files/home/.cache/donor-zero/aesolator-donor-zero-frontier.json`

Fresh donor refs:
- `utkarshdalal/GameNative`: `cbea7f75f7bd5b1dd5f665148c91251cf4a89b39`.
- `MaxsTechReview/WinNative`: `b4297a39ade7ca46e3505b2c26ceafa5f0f69146`.
- `palazos/winlatorCmod`: `9a27d00aeaaed884624355539c0d352769d285d9`.

Measured app-tree frontier:
- `gamenative`: `mapped=469`, `equal_sha=42`, `equal_norm=219`, `changed=208`, `donor_only=350`.
- `winnative`: `mapped=435`, `equal_sha=20`, `equal_norm=41`, `changed=374`, `donor_only=115`.
- `palazos`: `mapped=1322`, `equal_sha=510`, `equal_norm=302`, `changed=510`, `donor_only=20`.

## Accepted / Merged

- `XInput2`: GameNative raw motion/button event surface, extension registration, selection cleanup, and 7-button pointer mapping are merged into the local X server path.
- `Renderer`: GameNative scene/read/write FBO split, render-scale hooks, source filters, FSR1 EASU/RCAS, scaling mode, and vivid effects are merged without keeping donor `drawFrame()` recursion.
- `Process lifecycle`: GameNative stale Wine process cleanup is merged into the local native lifecycle reaper and launch forensic stage.
- `Input controls`: WinNative range-button fix is generalized into a binding guard; palazos/Winlator-family hotplug stability is merged through soft fakeinput slot release, physical-device dedupe, and fingerprint/uinput rejection.
- `X11 pointer ownership`: GameNative pointer confinement/ClipCursor semantics are merged through `GrabManager` and X server pointer-delta clamping.
- `PatchElf`: WinNative native JNI rewrite surface is merged into the local `libpatchelf.so` owner, with Java mutation code calling the native section rewrite path.
- `Contents install`: user forensic failures are closed at the identity/schema/runtime-payload class level:
  foreign profile aliases, bionic-over-generic-glibc precedence, rolling
  bleeding-edge capability suffixes such as `10.0.99-arm64ec-ntsync`, and
  profile-less Winlator/GameHub/GameNative/WinNative WCP packages are recovered
  through payload evidence.
- `WCP intake`: `.wcp`, `.wcp.xz`, `.wcp.zst`, `.txz`, `.tzst`, raw `.tar`,
  and `.zip` package probes are ordered by artifact suffix with safe fallback;
  Wine/Proton runtime model is classified from install-root sentinels and ELF
  markers (`Android`/bionic vs `GLIBC_`/`ld-linux`) before choosing the
  canonical runtime install root.
- `Install forensics`: content import failures and recovery events now include
  archive format, root shape, payload classifier scores, runtime model, and
  install-root diagnostics so foreign-device logs explain why a package was
  accepted, repaired, or rejected.

## Preserved / Rejected

- `preserve-local`: local `virglbridge`, rootfs routing, bionic sidecars, and product CMake owners stay stronger than importing donor full `virglrenderer` trees wholesale.
- `preserve-local`: local Vulkan/Adrenotools routing, OEM ICD preload, API-level handling, and package namespace remain the product owner; donor split `gpu_image` / `vulkan` code is reference evidence only where weaker.
- `preserve-local`: existing Ae.solator assets and public-release branding are not overwritten by donor launcher icons or donor UI skins in this pass.
- `reject-app-scope`: donor Wine/Proton source, proot/rootfs build scripts, Steam/workshop storefront logic, cloud sync shells, and Windows guest `winhandler.c` are not app-repo imports; they belong to FreeWine/WCP/runtime lanes if accepted later.
- `reject-unsafe`: generic donor container helpers without a local JNI caller or with ambiguous element-free ownership are not imported into shared app native libraries.

## Verification Gate

Required before closure:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.winlator.cmod.contents.ContentProfileParserTest \
  --tests com.winlator.cmod.contents.ContentProfileIdentityTest \
  --tests com.winlator.cmod.inputcontrols.InputDeviceHeuristicsTest \
  --tests com.winlator.cmod.core.ProcessHelperSplitCommandTest \
  --tests com.winlator.cmod.core.TarCompressorUtilsTest \
  :app:assembleDebug --stacktrace --no-build-cache
```

Current status:
- Implementation ledger written.
- Final unified gate passed on `2026-04-26`.
- Local install proof: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  to `192.168.43.4:39057` returned `Success`.
- Debug APK SHA-256:
  `bc0773611d6a03831ba74601a148cd45d16b10ed54360b0c8e6d29c70c101000`.
