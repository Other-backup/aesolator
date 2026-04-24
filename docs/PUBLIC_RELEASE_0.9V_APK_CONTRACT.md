# Ae.solator 0.9v APK Contract

## Release Surface

- Public APK version name: `0.9v`.
- CI APK lane: `wcp-runtime-lanes/.github/workflows/ci-aesolator-apk.yml`.
- FreeWine/WCP runtime packages are not built or pulled by the APK CI lane.
- The first public APK ships the app shell, bundled graphics payloads, translators, support overlays, and the GameNative-adapted rootfs base.

## Active APK Donors

- `GameNative`: active APK rootfs donor for `imagefs_gamenative.txz` and `imagefs_patches_gamenative.tzst`.
- `GameNative`: donor/source-map owner for the `ubuntufs` shell, but the dynamic feature is disabled by default for the public APK.
- `libadrenotools`, Mesa/Zink/Turnip/AeMali/VirGL/Vortek/Gladio lanes: active graphics payload donors through tracked `app/src/main/assets/graphics_driver/*.tzst` packages.

## Retired APK Payloads

- `imagefs_bionic.txz` is retired for the public APK and must not be packaged.
- Extra imagefs dynamic delivery is disabled unless `AEO_ENABLE_UBUNTUFS_FEATURE=1` or `-Pae.enableUbuntuFsFeature=true` is set for a deliberate donor audit build.

## Release Variants

- `public`: `by.aero.so.benchmark.public`
- `ludashi`: `by.aero.so.benchmark`
- `benchmark`: `by.aero.so.benchmark.benchmark`

All three variants are built from the same source commit, versioned as `0.9v`, and differ only by installable Android package identity / launcher label.
