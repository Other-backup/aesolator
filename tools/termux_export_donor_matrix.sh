#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT_DIR="${1:-$PWD}"
OUT_DIR="${2:-$ROOT_DIR/out/termux_donor_export_$(date +%Y%m%d_%H%M%S)}"
mkdir -p "$OUT_DIR"

cd "$ROOT_DIR"

echo "[1/7] Python version"
python --version

echo "[2/7] Rebuild donor class matrix"
python tools/generate_donor_class_matrix.py

echo "[3/7] Rebuild matrix audit"
python tools/audit_donor_class_matrix.py

echo "[4/7] Build 24-donor batch plan"
python tools/build_24_donor_batch_plan.py

echo "[5/7] Run 24-donor apply report (local/fallback lanes)"
python tools/apply_24_donor_batch.py || true

echo "[6/7] Run no-clone RAW intake scan"
python tools/raw_donor_intake_scan.py || true

echo "[7/7] Export artifacts"
cp -f docs/DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md "$OUT_DIR/" || true
cp -f docs/DONOR_CLASS_SWEEP_AUDIT_2026-05-04.md "$OUT_DIR/" || true
cp -f docs/DONOR_24_BATCH_PLAN_2026-05-04.md "$OUT_DIR/" || true
cp -f docs/DONOR_24_APPLY_REPORT_2026-05-04.md "$OUT_DIR/" || true
cp -f docs/DONOR_24_RAW_INTAKE_REPORT_2026-05-04.md "$OUT_DIR/" || true
cp -f docs/RAW_DONOR_MANIFEST_APPLY_REPORT.md "$OUT_DIR/" || true
cp -f tools/donor_sources_2026_05_04.json "$OUT_DIR/" || true

if [ -d out ]; then
  mkdir -p "$OUT_DIR/out_logs"
  find out -maxdepth 1 -type f -name '*apply*.csv' -exec cp -f {} "$OUT_DIR/out_logs/" \; || true
fi

( cd "$(dirname "$OUT_DIR")" && tar -czf "$(basename "$OUT_DIR").tar.gz" "$(basename "$OUT_DIR")" )

echo "DONE"
echo "Export dir: $OUT_DIR"
echo "Archive: ${OUT_DIR}.tar.gz"
