# Chapter 2 FreeWine-Ae.solator Contract

## Scope

`Chapter 2` starts when `Ae.solator` stops treating `Wine/Proton` as
interchangeable inputs and moves onto one dedicated runtime line:
`FreeWine11`.

The deliverable is one product stack:

- `Ae.solator` launcher / container owner
- `FreeWine11` native runtime source tree
- `wcp-runtime-lanes` package/archive owner
- one `bionic-only` `freewine11-arm64ec` WCP line with a `FreeWine11`
  `prefixPack.txz`

## Invariants

1. `FreeWine11` is the runtime source-of-truth for this line, not Valve Proton.
2. The target artifact is the `Ae.solator` runtime package:
   `freewine11-arm64ec.wcp` plus compressed release variants and a
   `FreeWine11`-built `prefixPack.txz`.
3. `Ae.solator` is the consumer and runtime-integrator:
   container creation, content routing, runtime model selection, translator
   payload routing, and forensic proof stay app-owned.
4. `wcp-runtime-lanes` is the packaging/publish lane, not the runtime source.
5. `FEX`, `wowbox64`, and related translators are runtime dependencies for
   guest `x86/x64` execution, not the default root-cause for native
   `aarch64` bootstrap failures such as `wineboot --init`.
6. `esync` and `fsync` stay enabled by intent. If `fsync` cannot run because
   the Android app sandbox blocks `futex_waitv`, that must stay a capability
   truth reported by runtime proof, not a policy-level silent disable.
7. `bionic-only` means the runtime package must stay valid for Android-native
   deployment and must not drift toward a generic desktop Proton archive.

## Integrated Debug Loop

All non-trivial runtime tails now close through one integrated loop:

1. prove the app-side symptom in `Ae.solator`
2. verify WCP layout and runtime contract
3. debug the native `FreeWine11` layer
4. verify translator payloads (`FEX` / `wowbox64` / `box64`) only at the
   layer where they actually become active
5. sync the resulting rule/doc changes across all three repositories

## Delivery Rules

- Prefer a scratch-built `FreeWine11` prefix pack over donor repacks.
- Keep `prefixPack` and `WCP` provenance explicit.
- Do not call the lane closed from compile-only proof; runtime/package/app
  proof are mandatory.
- Do not regress the line back to a generic `Proton` substitution story.
