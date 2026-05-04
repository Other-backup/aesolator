#!/usr/bin/env python3
from __future__ import annotations
import argparse, base64, hashlib, json, os, time
from pathlib import Path
from urllib import request, error

REPO_ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = REPO_ROOT / 'docs' / 'DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md'
DONOR_CFG = REPO_ROOT / 'tools' / 'donor_sources_2026_05_04.json'

API = 'https://api.github.com'


def gh_get(url: str, token: str | None):
    req = request.Request(url, headers={'Accept':'application/vnd.github+json', **({'Authorization':f'Bearer {token}'} if token else {})})
    with request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode('utf-8'))


def file_sha12_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()[:12]


def parse_matrix(path: Path):
    lines = path.read_text(encoding='utf-8').splitlines()
    t = [ln for ln in lines if ln.startswith('|')]
    hdr = [c.strip() for c in t[0].strip('|').split('|')]
    out=[]
    for i,ln in enumerate(t[2:], start=2):
        cols=[c.strip() for c in ln.strip('|').split('|')]
        if len(cols)<len(hdr): cols += ['']*(len(hdr)-len(cols))
        out.append((i,dict(zip(hdr,cols))))
    return lines,hdr,out


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--max-files-per-donor', type=int, default=2000)
    ap.add_argument('--write', action='store_true')
    args=ap.parse_args()
    token=os.getenv('GITHUB_TOKEN')
    donors=json.loads(DONOR_CFG.read_text(encoding='utf-8'))['donors']
    lines,hdr,rows=parse_matrix(MATRIX_PATH)

    class_map={Path(r['Class']).name: r for _,r in rows if r.get('Class')}
    updates=0
    for d in donors:
        label=d['label']
        repo=label
        st_col=f'{label}:status'; p_col=f'{label}:path'; h_col=f'{label}:sha12'
        if st_col not in hdr: continue
        try:
            tree=gh_get(f'{API}/repos/{repo}/git/trees/HEAD?recursive=1', token)
        except Exception:
            continue
        items=[x for x in tree.get('tree',[]) if x.get('type')=='blob' and x.get('path','').endswith(('.java','.kt'))][:args.max_files_per_donor]
        by_name={}
        for it in items:
            by_name.setdefault(Path(it['path']).name,[]).append(it['path'])
        for idx,r in rows:
            name=Path(r.get('Class','')).name
            cands=by_name.get(name)
            if not cands: continue
            cpath=sorted(cands, key=lambda p:(0 if '/com/winlator/' in p else 1, len(p)))[0]
            try:
                blob=gh_get(f'{API}/repos/{repo}/contents/{cpath}', token)
                content=base64.b64decode(blob['content'])
            except Exception:
                continue
            sha12=file_sha12_bytes(content)
            local=r.get('Local SHA12','')
            status='matched' if sha12==local else 'review'
            r[st_col]=status; r[p_col]=cpath; r[h_col]=sha12
            updates+=1
        time.sleep(0.2)

    if args.write and updates:
        table=["| "+" | ".join(hdr)+" |","|"+"|".join([" --- " for _ in hdr])+"|"]
        for _,r in rows:
            table.append("| "+" | ".join(r.get(c,'') for c in hdr)+" |")
        # replace first table block
        start=next(i for i,l in enumerate(lines) if l.startswith('|'))
        end=start
        while end < len(lines) and lines[end].startswith('|'): end+=1
        new_lines=lines[:start]+table+lines[end:]
        MATRIX_PATH.write_text('\n'.join(new_lines)+'\n', encoding='utf-8')
    print(f'updates={updates}')

if __name__=='__main__':
    main()
