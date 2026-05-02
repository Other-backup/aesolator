# Donor Runtime Component Frontier - 2026-05-03

## Source Evidence

- `GameNative/proton-wine` latest checked release:
  `build-20260502-1-sdk35`, published `2026-05-02T02:01:57Z`,
  Proton 11.0-1 commit `7c98acd6eafe1b1bac00e20d108f77ba39f3ff06`.
  Artifact contract remains split: `proton-*.wcp` is bionic/GameNative;
  `proton-wine-*.wcp.xz` is glibc Winlator CMOD/Ludashi.
  Source: `https://github.com/GameNative/proton-wine/releases/tag/build-20260502-1-sdk35`
- `AndreVto/proton-wine` latest checked release:
  `build-20260429-1-sdk28`, published `2026-04-29T20:25:19Z`,
  Proton 11.0-1 commit `d923a16729f6f2e03973239dce07c4cba94c1d5d`.
  Source: `https://github.com/AndreVto/proton-wine/releases/tag/build-20260429-1-sdk28`
- `The412Banner/Nightlies` latest checked release:
  `nightly-20260502-094707`, published `2026-05-02T09:47:21Z`.
  It ships Box64, Box64 Hybrid, WOWBox64, FEXCore, DXVK, VKD3D-Proton,
  and Turnip packages in the same release family.
  Source: `https://github.com/The412Banner/Nightlies/releases/tag/nightly-20260502-094707`
- `The412Banner/Nightlies` `nightlies_components.json` includes current
  `nightly-latest` component rows plus Banners-Turnip rows typed as
  `GpuDriver`, including `Turnip-v26.2.0-20260502-r2`,
  `Turnip-v26.2.0-20260502-r2-A8xx`, and
  `Turnip-v26.2.0-20260502-r2-710-720-Test`.
  Source: `https://github.com/The412Banner/Nightlies/blob/main/nightlies_components.json`
- `Xnick417x/Winlator-Bionic-Nightly-wcp` latest checked release:
  `dxvk-nightly-d1b0151c`, published `2026-05-02T04:34:31Z`.
  This remains a bionic/component donor lane for DXVK/VKD3D/FEX/Box64 style
  components, not a glibc Wine root.
  Source: `https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp/releases/tag/dxvk-nightly-d1b0151c`
- `Waim908/wine-winlator` latest checked release:
  `proton10-hostei`, published `2026-04-26T01:50:14Z`, artifact
  `wine-10.99.whp`; retained as legacy glibc evidence below Proton 11.
  Source: `https://github.com/Waim908/wine-winlator/releases/tag/proton10-hostei`
- `moze30/winlator-wcp` latest checked release:
  `zmod-v1`, published `2026-04-21T11:52:27Z`, artifacts
  `wine-9.2-custom.wcp`, `wine-10.10-arm64ec.wcp`, and `box64-0.4.2.wcp`.
  Source: `https://github.com/moze30/winlator-wcp/releases/tag/zmod-v1`

## Applied Transfer

- `GpuDriver`, `gpu driver`, and `vulkan driver` donor aliases now map to
  Ae.solator `TurnipDriver`.
- Nightlies feed support now includes `TurnipDriver`, so the current
  The412Banner/Banners-Turnip package line is visible from Contents instead
  of being silently dropped.
- GitHub release normalization now classifies `Turnip-v*.zip` assets as
  `TurnipDriver` with `displayCategory=Turnip`.
- Remote merge priority now treats `TurnipDriver` and `OpenGLDriver` as real
  graphics packages with archive-format priority, not as generic leftovers.
- Existing Proton/Wine split remains preserved: GameNative/AndreVto
  `proton-*` bionic packages do not become glibc, and `proton-wine-*` glibc
  packages do not become bionic.

## Verification Contract

- `RemoteFeedPayloadLoaderTest` covers the latest Nightlies asset shape:
  Box64 Bionic, Box64 Hybrid WOW64, FEXCore, DXVK GPLAsync,
  VKD3D-Proton ARM64EC, and Turnip A8xx.
- `RuntimeFeedRegistryTest` covers Turnip exposure from the Nightlies source
  mode.
- `ContentProfileParserTest` covers the donor `GpuDriver` / `Vulkan Driver`
  aliases.

Executed verification:

- `./gradlew :app:testDebugUnitTest --tests 'com.winlator.cmod.contents.RemoteFeedPayloadLoaderTest' --tests 'com.winlator.cmod.contents.RuntimeFeedRegistryTest' --tests 'com.winlator.cmod.contents.ContentProfileParserTest' --tests 'com.winlator.cmod.contents.RemoteProfileFeedMergerTest' --tests 'com.winlator.cmod.contents.RuntimeLaunchPolicyTest'`
- `./gradlew :app:assembleDebug -Pae.applicationId=by.aero.so.benchmark -Pae.versionCode=201237 -Pae.versionName=0.9v-local7`
- APK SHA256:
  `0fe394d014f09461a97efb1dc424c9d2417e2ef96b1ead35586e62e4fe4b6556`
- Device proof remains blocked in this environment: `adb devices -l` is
  empty, and the previous `10.108.201.54:39707` plus `10.108.201.54:5555`
  endpoints timed out.
