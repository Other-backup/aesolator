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
