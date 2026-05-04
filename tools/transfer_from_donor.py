#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
from pathlib import Path



def parse_matrix_table(matrix_path: Path):
    lines = matrix_path.read_text(encoding='utf-8').splitlines()
    table = [ln.strip() for ln in lines if ln.strip().startswith('|')]
    header = [c.strip() for c in table[0].strip('|').split('|')]
    rows = []
    for ln in table[2:]:
        cols = [c.strip() for c in ln.strip('|').split('|')]
        if len(cols) < len(header):
            cols += [''] * (len(header) - len(cols))
        rows.append(dict(zip(header, cols)))
    return header, rows


def main() -> None:
    ap = argparse.ArgumentParser(description='Apply deterministic donor file transfer using class sweep matrix evidence.')
    ap.add_argument('--matrix', default='docs/DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md')
    ap.add_argument('--donor-label', required=True, help='Donor label exactly as used in matrix header, e.g. brunodev85/winlator')
    ap.add_argument('--donors-root', default='donors')
    ap.add_argument('--donor-dir', default='', help='Explicit donor directory override (bypasses donors-root/label mapping)')
    ap.add_argument('--include-pending', action='store_true', help='Also attempt pending rows by filename lookup when donor path is missing')
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--limit', type=int, default=0, help='Optional max number of files to transfer')
    ap.add_argument('--log-csv', default='out/donor_transfer_log.csv')
    ap.add_argument('--max-delete-ratio', type=float, default=0.35, help='Safety guard: skip copy if donor bytes are too small vs local file')
    ap.add_argument('--class-prefix', default='app/src/main/', help='Only transfer classes whose path starts with this prefix')
    ap.add_argument('--exclude-prefix', default='ci/', help='Skip classes under this local prefix')
    args = ap.parse_args()

    matrix_path = Path(args.matrix)
    _, rows = parse_matrix_table(matrix_path)

    status_key = f'{args.donor_label}:status'
    path_key = f'{args.donor_label}:path'

    donors_root = Path(args.donors_root)
    donor_key = args.donor_label.split('/')[-1]
    donor_root = Path(args.donor_dir) if args.donor_dir else (donors_root / donor_key)

    if not donor_root.exists():
        raise SystemExit(f'Donor root not found: {donor_root}')

    donor_name_index = {}
    if args.include_pending:
        for path in donor_root.rglob('*'):
            if path.is_file() and path.suffix in {'.java', '.kt'}:
                donor_name_index.setdefault(path.name, []).append(path)

    transfers = []
    for row in rows:
        row_status = row.get(status_key, 'pending')
        if row_status not in {'review'} and not (args.include_pending and row_status == 'pending'):
            continue
        local_rel = row['Class']
        if args.class_prefix and not local_rel.startswith(args.class_prefix):
            continue
        if args.exclude_prefix and local_rel.startswith(args.exclude_prefix):
            continue
        donor_rel = row.get(path_key, '-')
        if donor_rel in {'', '-'}:
            if not args.include_pending:
                continue
            by_name = donor_name_index.get(Path(local_rel).name, [])
            if not by_name:
                continue
            donor_rel = by_name[0].relative_to(donor_root).as_posix()
        transfers.append((local_rel, donor_rel, row_status))

    if args.limit > 0:
        transfers = transfers[: args.limit]

    if not transfers:
        print('No review rows eligible for transfer.')
        return

    Path(args.log_csv).parent.mkdir(parents=True, exist_ok=True)
    with Path(args.log_csv).open('w', newline='', encoding='utf-8') as f:
        w = csv.writer(f)
        w.writerow(['local_rel', 'donor_rel', 'status'])

        for local_rel, donor_rel, row_status in transfers:
            src = donor_root / donor_rel
            dst = Path(local_rel)
            if not src.exists():
                w.writerow([local_rel, donor_rel, f'missing-source:{row_status}'])
                continue
            if dst.exists() and src.exists():
                donor_size = src.stat().st_size
                local_size = dst.stat().st_size
                if local_size > 0:
                    ratio = donor_size / float(local_size)
                    if ratio < args.max_delete_ratio:
                        w.writerow([local_rel, donor_rel, f'skipped-shrink:{row_status}:{ratio:.3f}'])
                        continue
            if args.dry_run:
                w.writerow([local_rel, donor_rel, f'dry-run:{row_status}'])
                continue
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(src.read_bytes())
            w.writerow([local_rel, donor_rel, f'copied:{row_status}'])

    mode = 'DRY-RUN' if args.dry_run else 'APPLY'
    print(f'{mode}: processed {len(transfers)} transfer candidate(s), log={args.log_csv}')


if __name__ == '__main__':
    main()
