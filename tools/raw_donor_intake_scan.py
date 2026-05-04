#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import urllib.request
from collections import defaultdict
from pathlib import Path

CFG = Path('tools/donor_sources_2026_05_04.json')
MATRIX = Path('docs/DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md')
OUT = Path('docs/DONOR_24_RAW_INTAKE_REPORT_2026-05-04.md')
UA = {'User-Agent': 'aesolator-raw-intake/1.0'}
TOKEN = os.environ.get('GITHUB_TOKEN', '').strip()


def parse_matrix_classes(path: Path):
    lines = path.read_text(encoding='utf-8').splitlines()
    table = [ln.strip() for ln in lines if ln.strip().startswith('|')]
    header = [c.strip() for c in table[0].strip('|').split('|')]
    classes = []
    for ln in table[2:]:
        cols = [c.strip() for c in ln.strip('|').split('|')]
        if len(cols) < len(header):
            cols += [''] * (len(header) - len(cols))
        row = dict(zip(header, cols))
        classes.append(row['Class'])
    return classes


def fetch_tree(owner: str, repo: str):
    for branch in ('main', 'master'):
        url = f'https://api.github.com/repos/{owner}/{repo}/git/trees/{branch}?recursive=1'
        headers = dict(UA)
        if TOKEN:
            headers['Authorization'] = f'Bearer {TOKEN}'
        req = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=25) as r:
                data = json.loads(r.read().decode('utf-8'))
                if 'tree' in data:
                    return branch, [x['path'] for x in data['tree'] if x.get('type') == 'blob']
        except Exception:
            continue
    return None, []


def main():
    donors = json.loads(CFG.read_text(encoding='utf-8')).get('donors', [])
    classes = parse_matrix_classes(MATRIX)
    local_names = defaultdict(list)
    for c in classes:
        local_names[Path(c).name].append(c)

    out = ['# 24 Donor RAW Intake Report', '', 'Updated: `2026-05-04`', '',
           'Remote GitHub tree scan without local clone; candidate mapping by filename/path suffix.', '']

    for d in donors:
        label = d['label']
        owner, repo = label.split('/', 1)
        out.append(f'## {label}')
        branch, paths = fetch_tree(owner, repo)
        if not paths:
            out.append('- status: `blocked (tree fetch failed)`')
            out.append('- hint: set `GITHUB_TOKEN` and rerun (especially for private repos / tighter API limits)')
            out.append('')
            continue
        by_name = defaultdict(list)
        for p in paths:
            if p.endswith('.java') or p.endswith('.kt'):
                by_name[Path(p).name].append(p)

        match_count = 0
        suffix_count = 0
        sample = []
        for name, locals_for_name in local_names.items():
            if name not in by_name:
                continue
            match_count += len(locals_for_name)
            for local_rel in locals_for_name:
                for remote in by_name[name]:
                    if remote.endswith(local_rel.replace('app/src/main/java/', 'app/src/main/java/').split('/', 3)[-1]):
                        suffix_count += 1
                        if len(sample) < 8:
                            sample.append((local_rel, remote))
                        break

        out.append(f'- branch: `{branch}`')
        out.append(f'- java/kotlin blobs: `{sum(1 for p in paths if p.endswith((".java", ".kt")))}`')
        out.append(f'- local classes with filename hit: `{match_count}`')
        out.append(f'- strong suffix-path hits: `{suffix_count}`')
        if sample:
            out.append('- sample candidates:')
            for l, r in sample:
                out.append(f'  - `{l}` <- `{r}`')
        out.append('')

    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
    print(f'Wrote {OUT}')


if __name__ == '__main__':
    main()
