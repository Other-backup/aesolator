#!/usr/bin/env python3

import argparse
import shutil
import sys
import tarfile
from pathlib import Path


def parse_args():
    parser = argparse.ArgumentParser(description="Extract a single member from a .tar.xz archive.")
    parser.add_argument("--archive", required=True, help="Path to the source .tar.xz archive")
    parser.add_argument("--member", required=True, help="Archive member name to extract")
    parser.add_argument("--output", required=True, help="Destination path")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    archive_path = Path(args.archive)
    output_path = Path(args.output)
    if not archive_path.is_file():
        print(f"archive not found: {archive_path}", file=sys.stderr)
        return 1

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive_path, "r:xz") as archive:
        try:
            member = archive.getmember(args.member)
        except KeyError:
            print(f"member not found: {args.member}", file=sys.stderr)
            return 1
        if not member.isfile():
            print(f"member is not a regular file: {args.member}", file=sys.stderr)
            return 1
        source = archive.extractfile(member)
        if source is None:
            print(f"unable to extract member: {args.member}", file=sys.stderr)
            return 1
        with source, output_path.open("wb") as destination:
            shutil.copyfileobj(source, destination)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
