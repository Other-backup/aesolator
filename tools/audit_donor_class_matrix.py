#!/usr/bin/env python3
from __future__ import annotations

from collections import Counter, defaultdict
from pathlib import Path

MATRIX = Path('docs/DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md')
OUT = Path('docs/DONOR_CLASS_SWEEP_AUDIT_2026-05-04.md')


def parse_table(lines: list[str]):
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
    lines = MATRIX.read_text(encoding='utf-8').splitlines()
    header, rows = parse_table(lines)
    donor_status_cols = [h for h in header if h.endswith(':status')]

    totals = Counter()
    by_donor = {d: Counter() for d in donor_status_cols}
    by_lane = defaultdict(Counter)

    for r in rows:
        lane = r.get('Lane', 'unknown')
        for d in donor_status_cols:
            st = r.get(d, 'pending') or 'pending'
            totals[st] += 1
            by_donor[d][st] += 1
            by_lane[lane][st] += 1

    unresolved = totals['pending'] + totals['review']

    out = []
    out.append('# Donor Class Sweep Audit (2026-05-04)')
    out.append('')
    out.append(f'- Matrix rows: `{len(rows)}`')
    out.append(f'- Donor status columns: `{len(donor_status_cols)}`')
    out.append(f'- Total status cells: `{len(rows) * len(donor_status_cols)}`')
    out.append(f'- Unresolved cells (`pending` + `review`): `{unresolved}`')
    out.append('')
    out.append('## Global status counts')
    out.append('')
    for k in ['matched', 'review', 'pending', 'hold']:
        out.append(f'- `{k}`: `{totals[k]}`')
    out.append('')
    out.append('## Per-lane unresolved counts')
    out.append('')
    for lane, c in sorted(by_lane.items()):
        out.append(f'- `{lane}`: pending={c["pending"]}, review={c["review"]}, matched={c["matched"]}, hold={c["hold"]}')
    out.append('')
    out.append('## Per-donor unresolved counts')
    out.append('')
    for donor, c in sorted(by_donor.items()):
        out.append(f'- `{donor}`: pending={c["pending"]}, review={c["review"]}, matched={c["matched"]}, hold={c["hold"]}')
    out.append('')
    out.append('## Closure gate')
    out.append('')
    if unresolved == 0:
        out.append('- ✅ Matrix closure reached: no `pending`/`review` cells remain.')
    else:
        out.append('- ❌ Matrix is not closed. Full donor transfer cannot be claimed yet.')
        out.append('- Required next step: materialize donor trees and execute per-class transfer decisions until unresolved=0.')

    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
    print(f'Wrote {OUT} (rows={len(rows)}, donors={len(donor_status_cols)}, unresolved={unresolved})')


if __name__ == '__main__':
    main()
