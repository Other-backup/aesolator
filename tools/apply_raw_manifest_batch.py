#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import urllib.request
from pathlib import Path


def fetch_text(url: str) -> str:
    headers = {'User-Agent': 'aesolator-raw-manifest/1.0'}
    token = os.environ.get('GITHUB_TOKEN', '').strip()
    if token:
        headers['Authorization'] = f'Bearer {token}'
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode('utf-8')


def main():
    import argparse
    ap = argparse.ArgumentParser(description='Apply donor raw-file manifest without local donor clones.')
    ap.add_argument('--manifest', default='docs/RAW_DONOR_MANIFEST_TEMPLATE.json')
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--max-delete-ratio', type=float, default=0.90)
    ap.add_argument('--log', default='docs/RAW_DONOR_MANIFEST_APPLY_REPORT.md')
    args = ap.parse_args()

    manifest = json.loads(Path(args.manifest).read_text(encoding='utf-8'))
    entries = manifest.get('entries', [])
    lines = ['# RAW Donor Manifest Apply Report', '', f'- manifest: `{args.manifest}`', '']

    for e in entries:
        local = Path(e['local_path'])
        raw_url = e['raw_url']
        donor = e.get('donor', 'unknown')
        try:
            content = fetch_text(raw_url)
        except Exception as ex:
            lines.append(f'- ❌ `{local}` <- `{donor}` failed fetch: `{ex}`')
            continue

        donor_size = len(content.encode('utf-8'))
        local_size = local.stat().st_size if local.exists() else 0
        ratio = donor_size / local_size if local_size else 1.0
        if local_size and ratio < args.max_delete_ratio:
            lines.append(f'- ⚠️ `{local}` skipped shrink ratio={ratio:.3f} donor={donor}')
            continue

        if args.dry_run:
            lines.append(f'- ✅ `{local}` candidate ready donor={donor} ratio={ratio:.3f}')
            continue

        local.parent.mkdir(parents=True, exist_ok=True)
        local.write_text(content, encoding='utf-8')
        lines.append(f'- ✅ `{local}` applied donor={donor} ratio={ratio:.3f}')

    Path(args.log).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'Wrote {args.log}')


if __name__ == '__main__':
    main()
