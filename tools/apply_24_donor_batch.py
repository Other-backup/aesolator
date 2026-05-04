#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import subprocess
from pathlib import Path

DONORS_CFG = Path('tools/donor_sources_2026_05_04.json')
OUT = Path('docs/DONOR_24_APPLY_REPORT_2026-05-04.md')

FALLBACK_DONOR_DIR = {
    'brunodev85_winlator': 'ci/winlator/templates',
}


def run(cmd: list[str]) -> tuple[int, str]:
    p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    return p.returncode, p.stdout


def count_statuses(csv_path: Path):
    counts = {}
    if not csv_path.exists():
        return counts
    with csv_path.open(encoding='utf-8') as f:
        r = csv.DictReader(f)
        for row in r:
            st = row.get('status', 'unknown')
            k = st.split(':', 1)[0]
            counts[k] = counts.get(k, 0) + 1
    return counts


def main():
    cfg = json.loads(DONORS_CFG.read_text(encoding='utf-8'))
    donors = cfg.get('donors', [])
    out = ['# 24 Donor Apply Report', '', 'Updated: `2026-05-04`', '']

    for donor in donors:
        key = donor['key']
        label = donor['label']
        local_dir = Path('donors') / key
        donor_dir = Path(FALLBACK_DONOR_DIR[key]) if key in FALLBACK_DONOR_DIR else local_dir
        csv_path = Path(f'out/{key}_apply.csv')

        out.append(f'## {label}')
        out.append(f'- donor dir: `{donor_dir}`')
        if not donor_dir.exists():
            out.append('- status: `blocked (missing donor dir)`')
            out.append('')
            continue

        cmd = [
            'python', 'tools/transfer_from_donor.py',
            '--donor-label', label,
            '--donor-dir', str(donor_dir),
            '--include-pending',
            '--log-csv', str(csv_path),
            '--max-delete-ratio', '0.90',
        ]
        code, output = run(cmd)
        counts = count_statuses(csv_path)
        out.append(f'- transfer exit: `{code}`')
        out.append(f'- summary: `{counts}`')
        out.append(f'- log: `{csv_path}`')
        out.append('')

    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
    print(f'Wrote {OUT}')


if __name__ == '__main__':
    main()
