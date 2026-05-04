# Donor Black-Diamond Engineering Analysis (2026-05-04)

## Why prior base was insufficient

The donor base used by the matrix was too narrow for a literal whole-GitHub transfer frontier.
A black-diamond pass needs:

1. app/runtime donors,
2. package/feed donors,
3. rootfs donors,
4. graphics/driver donors,
5. translator/runtime-core donors,
6. device-special donors,
7. archaeology/control donors.

## Expanded donor universe now encoded

Source of truth for the matrix donor set is now:

- `tools/donor_sources_2026_05_04.json`

The file includes 24 donors with explicit `tier` and `lane` metadata to prevent accidental single-donor bias.

## Engineering transfer model

For every class row and every donor:

- `pending` = donor checkout unavailable or class not mapped yet,
- `matched` = same SHA12 digest,
- `review` = donor candidate exists and differs,
- `hold` = explicit non-transfer decision with evidence.

### Deterministic evidence requirements

Per row, transfer cannot close without:

- donor path evidence,
- hash evidence,
- accept/adapt/hold decision,
- defect-class explanation when adapted,
- contract note for app/runtime impact.

## Critical constraints

- Do not bulk-copy from Tier B/C donors into Tier A runtime lanes.
- Use package/feed donors for metadata completeness, not runtime ownership.
- Keep `FreeWine11` app/runtime boundary and Chapter 2 split ownership contracts intact.
- Preserve provenance: every imported behavior must reference donor origin.

## Execution sequence

1. Materialize donor checkouts in `donors/<key>/`.
2. Re-run matrix generator.
3. Sort by `review` count per donor and lane.
4. Transfer high-impact runtime classes first (`com/winlator/*`).
5. Update evidence docs (`GAMENATIVE_*`, roadmap, journal) each batch.

## Known residual

This pass hardens analysis + matrix reach.
Literal full-code transfer still requires donor checkout materialization and per-class patching in iterative commits.
