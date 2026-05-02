# Glibc/Bionic Runtime Feed Ledger - 2026-05-02

## Source Evidence

- `AndreVto/proton-wine` release `build-20260429-1-sdk35`, published
  `2026-04-29T20:24:51Z`, is Proton 11.0-1 from
  `d923a16729f6f2e03973239dce07c4cba94c1d5d`.
- `AndreVto/proton-wine` release `build-20260429-1-sdk28`, published
  `2026-04-29T20:25:19Z`, carries the same Proton 11.0-1 commit.
- `GameNative/proton-wine` release `build-20260502-1-sdk35`, published
  `2026-05-02T02:01:57Z`, is Proton 11.0-1 from
  `7c98acd6eafe1b1bac00e20d108f77ba39f3ff06`.
- `GameNative/proton-wine` release `build-20260502-1-sdk28`, published
  `2026-05-02T02:01:50Z`, carries the same Proton 11.0-1 commit.
- Both release families declare the same artifact split:
  `proton-*.wcp` is the GameNative Proton type, and
  `proton-wine-*.wcp.xz` is the Wine type for Winlator for CMOD and Ludashi.

## Product Contract

- `proton-*.wcp` remains a bionic runtime package and must resolve into a
  bionic runtime root.
- `proton-wine-*.wcp.xz` from the AndreVto/GameNative Proton release lanes is
  a glibc Winlator-family runtime package and must resolve into a glibc
  runtime root.
- Glibc launch hydration must query the current Proton/Wine feeds before old
  community glibc-only feeds, otherwise a missing Proton 11 glibc payload
  leaves launch stuck on stale `wine-glibc-10.10-*` installs.
- Installed runtime profile registration must reject profile/root mismatches
  such as a glibc profile under `imagefs-runtime-bionic-*`. Payload evidence
  may correct a profile, but an empty or profile-only mismatched root is not a
  usable installed runtime.
- Runtime version aliasing is valid only for Wine/Proton packages. Component
  packages such as DXVK/VKD3D keep exact version identity.

## Applied Owner Decisions

- Accept: AndreVto Proton/Wine release lane as a dedicated Contents feed.
- Accept: GameNative and AndreVto release artifact split as feed-level runtime
  model evidence.
- Merge: glibc hydration now includes the current Proton/Wine feeds plus Waim
  and moze glibc feeds.
- Preserve: bionic hydration still includes GameNative, AndreVto, Alexoqool,
  and Xnick bionic feeds.
- Reject: bionic-root/glibc-profile registration without payload proof.

## Verification

- `./gradlew :app:testDebugUnitTest --tests com.winlator.cmod.contents.RemoteFeedPayloadLoaderTest --tests com.winlator.cmod.contents.RuntimeFeedRegistryTest --tests com.winlator.cmod.contents.RemoteProfileFeedMergerTest`
- `./gradlew :app:testDebugUnitTest --tests com.winlator.cmod.contents.RemoteFeedPayloadLoaderTest --tests com.winlator.cmod.contents.RuntimeFeedRegistryTest --tests com.winlator.cmod.contents.RemoteProfileFeedMergerTest --tests com.winlator.cmod.contents.ImportedContentHeuristicsTest --tests com.winlator.cmod.contents.ContentProfileIdentityTest --tests com.winlator.cmod.contents.ContentProfileParserTest`
- `./gradlew :app:assembleDebug -Pae.applicationId=by.aero.so.benchmark -Pae.versionCode=201235 -Pae.versionName=0.9v-local5`

APK proof:

- `app/build/outputs/apk/debug/app-debug.apk`
- SHA256 `95fd33eb9af829ec33de90ec844415be3e4f742ae45cef23a132d607af7eec1b`

## Residual Gate

Device install and live glibc launch proof are blocked until ADB reconnects.
The last attempted endpoints, `10.108.201.54:39707` and `10.108.201.54:5555`,
timed out from this host.

## 2026-05-03 Launch Promotion Addendum

Fresh donor check:

- `GameNative/proton-wine` latest release:
  `build-20260502-1-sdk35`, published `2026-05-02T02:01:57Z`,
  Proton 11.0-1 commit `7c98acd6eafe1b1bac00e20d108f77ba39f3ff06`.
  `proton-11.0-1-*.wcp` remains GameNative/bionic, while
  `proton-wine-11.0-1-*.wcp.xz` remains Winlator CMOD/Ludashi glibc.
  Source: `https://github.com/GameNative/proton-wine/releases/tag/build-20260502-1-sdk35`
- `AndreVto/proton-wine` latest SDK 28 release:
  `build-20260429-1-sdk28`, published `2026-04-29T20:25:19Z`,
  Proton 11.0-1 commit `d923a16729f6f2e03973239dce07c4cba94c1d5d`.
  Source: `https://github.com/AndreVto/proton-wine/releases/tag/build-20260429-1-sdk28`
- `The412Banner/Nightlies` latest release:
  `nightly-20260502-094707`, published `2026-05-02T09:47:21Z`,
  with Box64 `234105f`, FEX `8ab0075`, VKD3D-Proton `497357c`,
  DXVK `d1b0151`, and Turnip `v26.2.0-20260502-r2`.
  Source: `https://github.com/The412Banner/Nightlies/releases/tag/nightly-20260502-094707`
- `Xnick417x/Winlator-Bionic-Nightly-wcp` latest release observed by
  GitHub latest-release API is `dxvk-nightly-d1b0151c`, published
  `2026-05-02T04:34:31Z`; this remains a bionic/component donor lane,
  not a glibc Wine root.
  Source: `https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp/releases/tag/dxvk-nightly-d1b0151c`
- `Waim908/wine-winlator` latest release:
  `proton10-hostei`, published `2026-04-26T01:50:14Z`, artifact
  `wine-10.99.whp`; useful as glibc legacy evidence, not preferred over
  current Proton 11 when a Proton 11 glibc artifact is available.
  Source: `https://github.com/Waim908/wine-winlator/releases/tag/proton10-hostei`
- `moze30/winlator-wcp` latest release:
  `zmod-v1`, published `2026-04-21T11:52:27Z`, artifacts
  `wine-9.2-custom.wcp`, `wine-10.10-arm64ec.wcp`, and
  `box64-0.4.2.wcp`; this is now explicitly classified as a legacy
  glibc launch-risk donor for automatic promotion.
  Source: `https://github.com/moze30/winlator-wcp/releases/tag/zmod-v1`

Applied launch contract:

- A new shared `RuntimeLaunchPolicy` owns launch scoring and promotion for
  Wine/Proton runtimes. XServer no longer embeds the scoring constants alone.
- Glibc launch now promotes old installed Wine 9.x/10.x or known moze/Zmod
  glibc runtimes to a stronger Proton 11 glibc profile when such profile is
  installed or remotely downloadable.
- Promotion happens before `ensureLaunchRootfsReady()`, so rootfs/imagefs
  binding is created for the promoted runtime identity instead of the stale
  requested runtime.
- A ready old glibc runtime now runs a one-shot donor-feed hydration probe
  before guest bootstrap. If hydration exposes a better Proton 11 glibc
  package, Ae.solator installs it and relaunches; if not, the one-shot marker
  prevents an infinite retry loop and the existing runtime continues.
- Bionic runtime selection is explicitly excluded from glibc promotion. The
  `proton-*.wcp` bionic lane and `proton-wine-*.wcp.xz` glibc lane remain
  separate rootfs/runtime identities.

2026-05-03 verification:

- `./gradlew :app:testDebugUnitTest --tests 'com.winlator.cmod.contents.RuntimeLaunchPolicyTest'`
- `./gradlew :app:testDebugUnitTest --tests 'com.winlator.cmod.contents.RuntimeLaunchPolicyTest' --tests 'com.winlator.cmod.contents.RemoteFeedPayloadLoaderTest' --tests 'com.winlator.cmod.contents.RuntimeFeedRegistryTest' --tests 'com.winlator.cmod.contents.RemoteProfileFeedMergerTest' :app:assembleDebug -Pae.applicationId=by.aero.so.benchmark -Pae.versionCode=201236 -Pae.versionName=0.9v-local6`
- APK SHA256:
  `0af0977bf565d621c7a73d988739c51ee86655da6bc4c545a071b7c6b46a8164`
- ADB install and live glibc forensic proof remain blocked: `adb devices -l`
  is empty, and `10.108.201.54:39707` plus `10.108.201.54:5555` timed out.
