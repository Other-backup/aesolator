# Black Diamond Asset Pack Import 2026-04-24

## Source Evidence

- Asset archive: `/storage/emulated/0/Download/aesolator_bd_cleanroom_final.zip`
- Asset archive SHA-256: `fce18e638102078243b2be631e9b3987f2772777aacdf4085b8bdbd919e67975`
- Banner: `/storage/emulated/0/Download/ChatGPT Image Apr 24, 2026, 10_43_38 PM.png`
- Banner SHA-256: `361cd89a9d830284fb98a39690db8149d178f4d4f6d2ff79c2cf2ed988f8f2c6`
- Banner dimensions: `2172 x 724`

## Archive Forensics

- Total ZIP entries: `430`
- Root layout: `res/` `419`, `integration/` `3`, `extras/` `8`
- Payload extensions: `.png` `423`, `.xml` `5`, `.txt` `2`
- Path safety: no absolute paths, parent traversal, drive-root paths, or symlink entries were present.
- Integration manifest snippet requires `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.

## Applied Surface

- Merged the full `res/` overlay into `app/src/main/res`.
- Preserved original integration notes and master renders under
  `docs/assets/black-diamond-cleanroom/`.
- Preserved the app manifest launcher contract; `AndroidManifest.xml` already points to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.
- Replaced adaptive launcher XMLs with foreground, background, and Android 13 monochrome icon layers.
- Added `values/aesolator_blackdiamond_colors.xml` and `values-night/aesolator_blackdiamond_colors.xml` for explicit light/dark brand colors.
- Added the new Black Diamond banner as `docs/assets/aesolator-banner.png`.
- Replaced legacy banner aliases `docs/assets/winlator-cmod-aesolator-logo.png`
  and root `logo.png` with the same 2026-04-24 banner.
- The WCP Archive companion banner is tracked in
  `wcp-runtime-lanes/docs/assets/aesolator-banner.png`, with
  `docs/assets/aesolator-freewine-banner.png` kept there as a compatibility
  mirror.

## Banner Alpha Hardening

- Rebuilt `docs/assets/aesolator-banner.png` as an RGBA transparent PNG.
- Removed the fake checkerboard background, including enclosed checker holes
  inside letterforms and separators.
- Added a light/cyan outer rim plus a soft dark shadow so the banner remains
  legible on dark README, release, and app-gallery backgrounds.
- Removed the decorative slash separators between `box64`, `FEX`, `WINE`,
  and `PROTON`; those product marks now stand as independent icons.
- Mirrored the same hardened bytes to `docs/assets/winlator-cmod-aesolator-logo.png`,
  root `logo.png`, and the WCP Archive banner mirrors.

## Resource Delta

- Source overlay files: `419`
- Existing files changed by content: `150`
- New resource files: `126`
- Existing files already byte-identical: `143`

## Verification Contract

- XML resources must parse under `xml.etree.ElementTree`.
- PNG resources must remain valid PNG files by `file(1)`.
- Android resource processing must pass through Gradle before release packaging.

## Verification Performed

- Overlay parity: `419/419` archive `res/` files match byte-for-byte under
  `app/src/main/res`.
- XML parse: `0` parse errors across the app resource tree.
- PNG probe: banner and cleanroom master renders are valid PNG files.
- Gradle/AAPT: `./gradlew --no-daemon :app:processDebugResources` completed
  with `RC=0`.
- Gradle log:
  `out/black-diamond-asset-import/logs/processDebugResources-20260424T2310.log`
