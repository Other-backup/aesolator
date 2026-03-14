# BannersComponentInjector Donor Audit

Updated: `2026-03-14`

## Scope

Audit notes for harvesting safe improvements from
`The412Banner/BannersComponentInjector` into `Ae.solator`.

## Observed Donor Strengths

- multiple online sources instead of a single hard-coded release feed
- release-tag browsing instead of newest-only package lookup
- search and sort surfaces for large package lists
- richer release metadata such as notes and published dates
- broader component coverage across runtime and graphics-related artifacts

## Verified Built-In Source Model

From the current donor source tree on `2026-03-14`, the built-in repository
set includes:

- `StevenMXZ`
- `Arihany WCPHub`
- `Xnick417x`
- `AdrenoToolsDrivers (K11MCH1)`
- `freedreno Turnip CI (whitebelyash)`
- `MaxesTechReview (MTR)`
- `HUB Emulators (T3st31)`
- `Nightlies by The412Banner`

The donor repository models sources as:

- one primary endpoint with an explicit format
- optional extra endpoints per source for type-specific fetching
- optional release tags promoted into browseable categories
- cached `RemoteItem` records carrying:
  `displayName`, `versionName`, `downloadUrl`, `sourceName`, `publishedAt`,
  `sizeBytes`, and `description`

This is useful to `Ae.solator` because it confirms that richer artifact
metadata and release-tag browsing are not speculative features; they are
already solved in a nearby donor implementation.

## Safe Transfer Targets

- full GitHub-release pagination for donor-backed sources
- clearer separation between release lanes and raw component feeds
- stronger package-link normalization before presenting install actions
- richer visible ordering:
  source lane, channel, version, architecture, then package format
- optional release-notes/detail sheet for remote packages
- artifact metadata carry-through:
  source label, release tag, artifact name, published date, and release notes

## Guardrails

- do not collapse `WCP Archive`, `WCPHub`, and donor feeds into one unlabeled
  source
- do not expose raw skeleton payloads as `Wine`/`Proton` runtimes unless the
  extracted package is a complete installable runtime
- verify every donor package family against the actual extracted payload before
  enabling install in `Contents`
- keep donor source labeling explicit in row metadata and install paths

## Near-Term Actions

1. finish paginated donor release ingestion in `Contents`
2. normalize architecture/channel sorting for donor-backed packages
3. inspect donor package families one by one:
   `Wine`, `Proton`, `DXVK`, `VKD3D`, `Box64`, `WOWBox64`, `FEXCore`,
   graphics drivers
4. port detail-sheet/search/sort improvements only after the package model is
   stable
5. decide whether `Nightlies by The412Banner` should become a first-class
   source lane in `Ae.solator` or stay donor-only package intelligence
