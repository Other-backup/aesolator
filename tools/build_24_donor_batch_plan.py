#!/usr/bin/env python3
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

MATRIX = Path('docs/DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md')
DONORS_CFG = Path('tools/donor_sources_2026_05_04.json')
OUT = Path('docs/DONOR_24_BATCH_PLAN_2026-05-04.md')


def parse_matrix(path: Path):
    lines = path.read_text(encoding='utf-8').splitlines()
    table = [ln.strip() for ln in lines if ln.strip().startswith('|')]
    header = [c.strip() for c in table[0].strip('|').split('|')]
    rows = []
    for ln in table[2:]:
        cols = [c.strip() for c in ln.strip('|').split('|')]
        if len(cols) < len(header):
            cols += [''] * (len(header) - len(cols))
        rows.append(dict(zip(header, cols)))
    return header, rows


def main():
    cfg = json.loads(DONORS_CFG.read_text(encoding='utf-8'))
    donors = cfg.get('donors', [])
    header, rows = parse_matrix(MATRIX)

    out = [
        '# 24-Donor Point Transfer Batch Plan',
        '',
        'Updated: `2026-05-04`',
        '',
        'Generated from donor config + class sweep matrix for constrained cloud RAW workflow.',
        ''
    ]

    total_rows = len(rows)
    out.append(f'- Matrix rows: `{total_rows}`')
    out.append(f'- Donors: `{len(donors)}`')
    out.append('')
    out.append('## Donor execution grid')
    out.append('')

    for donor in donors:
        label = donor['label']
        key = donor['key']
        status_col = f'{label}:status'
        path_col = f'{label}:path'
        c = Counter(r.get(status_col, 'pending') for r in rows)
        review_with_path = sum(1 for r in rows if r.get(status_col) == 'review' and r.get(path_col, '-') not in {'', '-'})
        local_root = Path('donors') / key
        out.append(f'### {label}')
        out.append(f'- key: `{key}`')
        out.append(f'- local checkout: `{local_root}` -> `{ "present" if local_root.exists() else "missing" }`')
        out.append(f'- matched: `{c["matched"]}` | review: `{c["review"]}` | pending: `{c["pending"]}` | hold: `{c["hold"]}`')
        out.append(f'- review rows with donor path: `{review_with_path}`')
        out.append('')

    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
    print(f'Wrote {OUT}')


if __name__ == '__main__':
    main()
