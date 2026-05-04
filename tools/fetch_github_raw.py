#!/usr/bin/env python3
"""Fetch a GitHub file via raw URL for no-clone donor scanning.
Usage: python3 tools/fetch_github_raw.py owner/repo path/in/repo [--ref main]
"""
from __future__ import annotations
import argparse
import urllib.request
import urllib.error

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('repo', help='owner/repo')
    ap.add_argument('path', help='path in repository')
    ap.add_argument('--ref', default='main', help='branch/tag/sha (default: main)')
    args = ap.parse_args()

    url = f"https://raw.githubusercontent.com/{args.repo}/{args.ref}/{args.path.lstrip('/')}"
    try:
        with urllib.request.urlopen(url, timeout=20) as r:
            data = r.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {url}")
        return 2
    except Exception as e:
        print(f"ERROR: {e}\nURL: {url}")
        return 1

    print(data)
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
