# Termux donor matrix export runbook

1. Open Termux and `cd` into your `aesolator` repo root.
2. Run:

```bash
bash tools/termux_export_donor_matrix.sh
```

Optional custom output dir:

```bash
bash tools/termux_export_donor_matrix.sh /path/to/aesolator /path/to/output_dir
```

## What it does

- Rebuilds donor matrix
- Rebuilds matrix audit
- Rebuilds 24-donor batch plan
- Runs 24-donor apply report
- Runs RAW intake scan
- Exports all generated docs + CSV logs into one folder and `.tar.gz`

## What to send back

Send me:

- `out/termux_donor_export_*/DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md`
- `out/termux_donor_export_*/DONOR_CLASS_SWEEP_AUDIT_2026-05-04.md`
- `out/termux_donor_export_*/DONOR_24_APPLY_REPORT_2026-05-04.md`
- `out/termux_donor_export_*/DONOR_24_RAW_INTAKE_REPORT_2026-05-04.md`
- all CSV files from `out/termux_donor_export_*/out_logs/`

If possible, send the archive directly:

- `out/termux_donor_export_*.tar.gz`
