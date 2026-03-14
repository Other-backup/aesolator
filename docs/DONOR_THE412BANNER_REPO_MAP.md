# The412Banner Repo Map

Updated: `2026-03-14`

## Scope

Inventory of public repositories under `https://github.com/The412Banner`
ranked by relevance to `Ae.solator`.

## High Relevance

### `Gamehub-Components`

- Role: raw component source for GameHub packages
- Current `Ae.solator` status:
  partially integrated via `GameHub` feed handling in `Contents`
- Why it matters:
  raw package links, release assets, and installable component payloads

### `Nightlies`

- Role: dedicated nightly artifact repository
- Current `Ae.solator` status:
  not yet a first-class source lane
- Why it matters:
  missing donor-backed nightly packages are likely to come from here before
  anywhere else in the `The412Banner` ecosystem
- Confirmed signal:
  release `proton-bleeding-edge-20260312-b310f0c-run23` already ships ARM64EC
  packaged Proton artifacts as both `.wcp` and `.wcp.xz`

### `BannersComponentInjector`

- Role: donor application implementing download/install/search/source logic
- Current `Ae.solator` status:
  audited as donor logic, not yet used as a live backend
- Why it matters:
  source model, release-tag browsing, search/sort UX, artifact metadata,
  custom repos, and multi-endpoint repositories

### `gamehub-revanced-patches`

- Role: ReVanced patch set for GameHub
- Why it matters:
  useful for understanding how component-management hooks are injected into the
  GameHub app surface

### `bannerhub`

- Role: GameHub APK rebuild pipeline
- Why it matters:
  likely owns or documents artifact assembly and rebuild logic around GameHub
  variants

### `gamehub-lite`

- Role: maintained GameHub Lite fork
- Why it matters:
  relevant for runtime/artifact expectations on the target app family

### `ghl-add`

- Role: auxiliary Java tooling around GameHub Lite ecosystem
- Why it matters:
  likely part of donor-side packaging or patch orchestration

## Medium Relevance

### `WinlatorWCPHub`

- Role: fork of the WCP hub
- Why it matters:
  overlaps with package distribution logic already present in `Ae.solator`

### `freedreno_turnip-CI`

- Role: scheduled Turnip driver build pipeline
- Why it matters:
  relevant for graphics-driver artifact production and release cadence

### `Winlator-Ludashi`

- Role: Winlator fork
- Why it matters:
  possible source of app/runtime integration ideas, but not primary donor

### `proton-wine`

- Role: fork of GameNative proton-wine
- Why it matters:
  relevant for Proton/Wine runtime understanding, but separate from current
  package-feed integration

### `GameNative-project`

- Role: GameNative fork
- Why it matters:
  related ecosystem context, especially around proton-wine expectations

## Lower Relevance

### `NewTermux`

- Termux fork, useful for device tooling context, not for `Ae.solator`
  package/runtime logic

### `Pypetto-box64`

- Box64-adjacent but not obviously a direct package-feed input for the current
  app

### `Ayaneo-PocketFit-tools`

- device-specific utilities, not directly relevant to `Ae.solator`

### `Banners-No-PC-Retroid-Overclock`

- device overclock files/guides, not relevant to `Ae.solator`

## Practical Conclusions

- For `Ae.solator`, the `The412Banner` ecosystem is not one donor repo but a
  stack:
  package feeds (`Gamehub-Components`, `Nightlies`),
  app logic (`BannersComponentInjector`),
  and patch/build infrastructure (`gamehub-lite`, `bannerhub`,
  `gamehub-revanced-patches`, `ghl-add`)
- The next highest-value integration target is `Nightlies` as a first-class
  source lane, especially for donor `Proton` and other nightly runtime streams
- The next highest-value research target after that is the GameHub-side build
  and patch chain:
  `gamehub-lite`, `bannerhub`, `gamehub-revanced-patches`, `ghl-add`
