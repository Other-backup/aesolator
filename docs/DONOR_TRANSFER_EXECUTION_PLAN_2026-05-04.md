# Donor Transfer Execution Plan (2026-05-04)

## What `10416` means

`10416` is the number of unresolved matrix cells (`434` classes × `24` donors), not the number of already-confirmed improvements.
A cell can only become a real transfer decision after donor source is materialized and compared class-by-class.

## Deterministic transfer loop

1. Materialize donor trees into `donors/<key>/` for every donor from `tools/donor_sources_2026_05_04.json`.
2. Regenerate matrix via `tools/generate_donor_class_matrix.py`.
3. For each class row, classify donor candidates as `import now` / `adapt later` / `hold` in evidence docs.
4. Apply source transfers only for `import now` decisions with contract-safe adaptation.
5. Re-run matrix + `tools/audit_donor_class_matrix.py` until unresolved reaches `0` with factual evidence.

## Batch strategy (required for safe closure)

- Batch A: infrastructure/runtime binding classes.
- Batch B: container + launcher routing classes.
- Batch C: graphics/X11/JNI bridge classes.
- Batch D: repo-support tooling/documentation classes.

Each batch must end with:

- code diff,
- docs sync,
- matrix status update,
- regression verification.

## Closure conditions

Transfer can be claimed complete only when all are true:

- `docs/DONOR_CLASS_SWEEP_AUDIT_2026-05-04.md` shows unresolved `0`.
- Every promoted cell has evidence path and transfer rationale.
- Runtime/docs contracts remain aligned.
