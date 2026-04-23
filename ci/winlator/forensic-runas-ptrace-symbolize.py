#!/usr/bin/env python3
from __future__ import annotations

import argparse
import bisect
import json
import re
import subprocess
from collections import Counter
from functools import lru_cache
from pathlib import Path

FRAME_CHAIN_LIMIT = 8


def run_text(cmd: list[str]) -> str:
    try:
        return subprocess.check_output(cmd, text=True, stderr=subprocess.DEVNULL)
    except Exception:
        return ""


def read_jsonl(path: Path) -> list[dict]:
    rows = []
    for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            continue
    return rows


def parse_hex(value: str | None) -> int | None:
    if not value:
        return None
    try:
        return int(str(value), 16)
    except ValueError:
        return None


def parse_word_list(words: list[str] | None) -> list[int]:
    out: list[int] = []
    for word in words or []:
        value = parse_hex(word)
        if value is None:
            continue
        out.append(value)
    return out


@lru_cache(maxsize=None)
def image_base(path: str) -> int:
    text = run_text(["llvm-readobj", "--file-headers", path])
    match = re.search(r"ImageBase:\s+0x([0-9A-Fa-f]+)", text)
    return int(match.group(1), 16) if match else 0


@lru_cache(maxsize=None)
def raw_symbols_for(path: str) -> list[tuple[int, str, str]]:
    desc = file_description(path)
    if "PE32" in desc or Path(path).suffix.lower() in {".dll", ".exe"}:
        text = run_text(["llvm-nm", "-n", path])
        out: list[tuple[int, str, str]] = []
        base = image_base(path)
        for raw in text.splitlines():
            parts = raw.split(maxsplit=2)
            if len(parts) < 3:
                continue
            addr_s, kind, name = parts
            if not re.fullmatch(r"[0-9A-Fa-f]+", addr_s):
                continue
            if name.startswith("__imp_"):
                kind = "I"
            addr = int(addr_s, 16)
            if base and addr >= base:
                addr -= base
            name = name.strip()
            if not name:
                continue
            out.append((addr, kind, name))
        if out:
            out.sort()
            deduped: list[tuple[int, str, str]] = []
            seen: set[tuple[int, str, str]] = set()
            for item in out:
                if item in seen:
                    continue
                seen.add(item)
                deduped.append(item)
            return deduped

    text = run_text(["llvm-nm", "-n", path])
    if not text:
        text = run_text(["nm", "-n", path])
    base = image_base(path)
    out: list[tuple[int, str, str]] = []
    for raw in text.splitlines():
        parts = raw.split(maxsplit=2)
        if len(parts) < 3:
            continue
        addr_s, kind, name = parts
        if not re.fullmatch(r"[0-9A-Fa-f]+", addr_s):
            continue
        addr = int(addr_s, 16)
        if base and addr >= base:
            addr -= base
        out.append((addr, kind, name.strip()))
    out.sort()
    return out


@lru_cache(maxsize=None)
def symbols_for(path: str) -> list[tuple[int, str]]:
    out: list[tuple[int, str]] = []
    for addr, kind, name in raw_symbols_for(path):
        if kind not in {"T", "t", "W", "w", "I", "i"}:
            continue
        if name.startswith("__imp_"):
            continue
        clean = name.lstrip("#").strip()
        if not clean:
            continue
        out.append((addr, clean))
    return out


@lru_cache(maxsize=None)
def symbol_names_at(path: str) -> dict[int, list[str]]:
    out: dict[int, list[str]] = {}
    for addr, _kind, name in raw_symbols_for(path):
        out.setdefault(addr, []).append(name)
    return out


@lru_cache(maxsize=None)
def coff_import_owners(path: str) -> dict[str, list[str]]:
    text = run_text(["llvm-readobj", "--coff-imports", path])
    owners: dict[str, list[str]] = {}
    current = ""
    for raw in text.splitlines():
        if match := re.match(r"\s*Name:\s+(.+?\.dll)\s*$", raw, flags=re.IGNORECASE):
            current = match.group(1).strip()
            continue
        if not current:
            continue
        if match := re.match(r"\s*Symbol:\s+(.+?)\s+\(\d+\)\s*$", raw):
            name = match.group(1).strip()
            bucket = owners.setdefault(name, [])
            if current not in bucket:
                bucket.append(current)
    return owners


def nearest_symbol(path: str, rva: int) -> tuple[str, int] | None:
    syms = symbols_for(path)
    if not syms:
        return None
    addrs = [addr for addr, _name in syms]
    idx = bisect.bisect_right(addrs, rva) - 1
    if idx < 0:
        return None
    addr, name = syms[idx]
    return name, rva - addr


def parse_addr2line_location(raw: str) -> dict:
    raw = raw.strip()
    if not raw or raw == "??:0":
        return {"location": "", "source_file": "", "source_line": 0, "source_column": 0}
    match = re.match(r"^(?P<file>.+):(?P<line>\d+)(?::(?P<column>\d+))?$", raw)
    if not match:
        return {"location": raw, "source_file": "", "source_line": 0, "source_column": 0}
    return {
        "location": raw,
        "source_file": match.group("file"),
        "source_line": int(match.group("line")),
        "source_column": int(match.group("column") or 0),
    }


@lru_cache(maxsize=None)
def addr2line_frames(path: str, rva: int) -> tuple[dict, ...]:
    query = rva
    desc = file_description(path)
    if "PE32" in desc or Path(path).suffix.lower() in {".dll", ".exe"}:
        base = image_base(path)
        if base:
            query += base
    text = run_text(["llvm-addr2line", "-f", "-C", "-i", "-e", path, f"0x{query:x}"])
    if not text:
        return ()

    lines = [line.strip() for line in text.splitlines() if line.strip()]
    frames: list[dict] = []
    for idx in range(0, len(lines), 2):
        function = lines[idx]
        location = lines[idx + 1] if idx + 1 < len(lines) else ""
        if function == "??" and location in {"", "??:0"}:
            continue
        parsed = parse_addr2line_location(location)
        frames.append(
            {
                "function": "" if function == "??" else function,
                "location": parsed["location"],
                "source_file": parsed["source_file"],
                "source_line": parsed["source_line"],
                "source_column": parsed["source_column"],
            }
        )
    return tuple(frames)


def sign_extend(value: int, bits: int) -> int:
    sign = 1 << (bits - 1)
    return (value & (sign - 1)) - (value & sign)


def arm64_instructions(words: list[str] | None) -> list[int]:
    insns: list[int] = []
    for word in words or []:
        value = parse_hex(word)
        if value is None:
            continue
        insns.append(value & 0xFFFFFFFF)
        insns.append((value >> 32) & 0xFFFFFFFF)
    return insns


def decode_adrp(insn: int, pc_abs: int) -> tuple[int, int] | None:
    if (insn & 0x9F000000) != 0x90000000:
        return None
    rd = insn & 0x1F
    immlo = (insn >> 29) & 0x3
    immhi = (insn >> 5) & 0x7FFFF
    imm = sign_extend((immhi << 2) | immlo, 21) << 12
    page = (pc_abs & ~0xFFF) + imm
    return rd, page


def decode_add_imm_64(insn: int) -> tuple[int, int, int] | None:
    if (insn & 0x7F000000) != 0x11000000:
        return None
    if ((insn >> 30) & 0x1) != 0x0:
        return None
    if ((insn >> 31) & 0x1) != 0x1:
        return None
    rd = insn & 0x1F
    rn = (insn >> 5) & 0x1F
    imm12 = (insn >> 10) & 0xFFF
    shift = ((insn >> 22) & 0x1) * 12
    return rd, rn, imm12 << shift


def decode_ldr_uimm_64(insn: int) -> tuple[int, int, int] | None:
    if (insn & 0xFFC00000) != 0xF9400000:
        return None
    rt = insn & 0x1F
    rn = (insn >> 5) & 0x1F
    imm12 = (insn >> 10) & 0xFFF
    return rt, rn, imm12 * 8


def decode_br(insn: int) -> int | None:
    if (insn & 0xFFFFFC1F) != 0xD61F0000:
        return None
    return (insn >> 5) & 0x1F


def decode_arm64_import_thunk(local_path: str, pc_rva: int, words: list[str] | None) -> dict | None:
    desc = file_description(local_path)
    if "COFF-ARM64" not in desc and "PE32" not in desc:
        return None

    base = image_base(local_path)
    insns = arm64_instructions(words)
    if len(insns) < 3:
        return None

    adrp = decode_adrp(insns[0], base + pc_rva)
    add = decode_add_imm_64(insns[1]) if len(insns) >= 2 else None
    if not adrp or not add:
        return None

    reg, page = adrp
    add_rd, add_rn, add_imm = add
    if add_rd != reg or add_rn != reg:
        return None
    slot_abs = page + add_imm
    kind = "jump_slot"

    if len(insns) >= 4:
        ldr = decode_ldr_uimm_64(insns[2])
        br = decode_br(insns[3])
        if ldr and br is not None:
            rt, rn, off = ldr
            if rt == reg and rn == reg and br == reg:
                slot_abs += off
                kind = "iat_load"
            else:
                return None
        else:
            br = decode_br(insns[2])
            if br is None or br != reg:
                return None
    else:
        br = decode_br(insns[2])
        if br is None or br != reg:
            return None

    slot_rva = slot_abs - base
    names = symbol_names_at(local_path).get(slot_rva, [])
    slot_symbol = next((name for name in names if name.startswith("__imp_")), names[0] if names else "")
    import_name = slot_symbol[len("__imp_"):] if slot_symbol.startswith("__imp_") else slot_symbol.lstrip("#")
    owners = coff_import_owners(local_path).get(import_name, []) if import_name else []
    return {
        "kind": kind,
        "slot_rva": f"0x{slot_rva:x}",
        "slot_symbol": slot_symbol,
        "import_symbol": import_name,
        "owner_dlls": owners,
    }


def discover_local_binary(workspace: Path, remote_module_path: str) -> Path | None:
    remote = Path(remote_module_path)
    basename = remote.name
    arch_hint = ""
    for token in ("aarch64-windows", "arm64ec-windows", "i386-windows", "x86_64-windows"):
        if f"/{token}/" in remote_module_path:
            arch_hint = token
            break

    roots = [
        workspace / "wcp-runtime-lanes" / "build-wine",
        workspace / "wcp-runtime-lanes" / "stage" / "usr" / "arm64-v8a" / "lib" / "wine",
    ]
    candidates: list[Path] = []
    for root in roots:
        if not root.exists():
            continue
        candidates.extend(root.rglob(basename))
    if arch_hint:
        hinted = [p for p in candidates if arch_hint in str(p)]
        if hinted:
            candidates = hinted
    if not candidates:
        return None
    candidates.sort(key=lambda p: (0 if "build-wine" in str(p) else 1, len(str(p))))
    return candidates[0]


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="ignore").strip()


def parse_maps(path: Path) -> list[dict]:
    rows: list[dict] = []
    if not path.exists():
        return rows
    for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        match = re.match(
            r"^(?P<start>[0-9A-Fa-f]+)-(?P<end>[0-9A-Fa-f]+)\s+"
            r"(?P<perms>\S+)\s+(?P<offset>[0-9A-Fa-f]+)\s+\S+\s+\d+\s*(?P<path>.*)$",
            line,
        )
        if not match:
            continue
        item = match.groupdict()
        item["path"] = item["path"].strip()
        rows.append(item)
    return rows


def find_map_for_addr(rows: list[dict], addr: int | None) -> dict | None:
    if addr is None:
        return None
    for row in rows:
        start = parse_hex(row.get("start"))
        end = parse_hex(row.get("end"))
        file_off = parse_hex(row.get("offset"))
        if start is None or end is None or file_off is None:
            continue
        if start <= addr < end:
            return {
                "start": f"0x{start:016x}",
                "end": f"0x{end:016x}",
                "offset": f"0x{addr - start + file_off:016x}",
                "perms": row.get("perms", ""),
                "path": row.get("path", "").strip(),
            }
    return None


@lru_cache(maxsize=None)
def file_description(path: str) -> str:
    return run_text(["file", "-b", path]).strip()


@lru_cache(maxsize=None)
def elf_details(path: str) -> dict:
    text = run_text(["readelf", "-h", path])
    dyn = run_text(["readelf", "-d", path])
    class_match = re.search(r"Class:\s+(.+)", text)
    machine_match = re.search(r"Machine:\s+(.+)", text)
    soname_match = re.search(r"\(SONAME\).*?\[(.+?)\]", dyn)
    needed = re.findall(r"\(NEEDED\).*?\[(.+?)\]", dyn)
    return {
        "class": class_match.group(1).strip() if class_match else "",
        "machine": machine_match.group(1).strip() if machine_match else "",
        "soname": soname_match.group(1).strip() if soname_match else "",
        "needed": needed,
    }


@lru_cache(maxsize=None)
def coff_details(path: str) -> dict:
    text = run_text(["llvm-readobj", "--file-headers", "--coff-imports", "--coff-exports", path])
    machine_match = re.search(r"Machine:\s+(.+)", text)
    import_dlls = sorted(set(re.findall(r"Name:\s+([A-Za-z0-9_.-]+\.dll)", text, flags=re.IGNORECASE)))
    export_count = len(re.findall(r"Export \{", text))
    return {
        "machine": machine_match.group(1).strip() if machine_match else "",
        "imports": import_dlls,
        "export_count": export_count,
    }


@lru_cache(maxsize=None)
def pe_sections(path: str) -> list[dict]:
    text = run_text(["llvm-readobj", "--sections", path])
    sections: list[dict] = []
    current: dict | None = None
    for raw in text.splitlines():
        line = raw.strip()
        if line == "Section {":
            current = {}
            continue
        if line == "}":
            if current and {"raw_ptr", "raw_size", "virt_addr", "virt_size"} <= current.keys():
                sections.append(current)
            current = None
            continue
        if current is None:
            continue
        if match := re.match(r"Name:\s+(.+?)\s+\(", line):
            current["name"] = match.group(1)
        elif match := re.match(r"VirtualSize:\s+0x([0-9A-Fa-f]+)", line):
            current["virt_size"] = int(match.group(1), 16)
        elif match := re.match(r"VirtualAddress:\s+0x([0-9A-Fa-f]+)", line):
            current["virt_addr"] = int(match.group(1), 16)
        elif match := re.match(r"RawDataSize:\s+([0-9]+)", line):
            current["raw_size"] = int(match.group(1), 10)
        elif match := re.match(r"PointerToRawData:\s+0x([0-9A-Fa-f]+)", line):
            current["raw_ptr"] = int(match.group(1), 16)
    return sections


def pe_file_offset_to_rva(path: str, offset: int) -> int:
    for section in pe_sections(path):
        raw_ptr = int(section["raw_ptr"])
        raw_size = max(int(section["raw_size"]), int(section["virt_size"]))
        if raw_ptr <= offset < raw_ptr + raw_size:
            return int(section["virt_addr"]) + (offset - raw_ptr)
    return offset


def module_metadata(local_path: Path) -> dict:
    desc = file_description(str(local_path))
    is_pe = local_path.suffix.lower() in {".dll", ".exe"} or "PE32" in desc
    details = coff_details(str(local_path)) if is_pe else elf_details(str(local_path))
    return {
        "description": desc,
        "format": "pe" if is_pe else "elf",
        **details,
    }


def inventory_modules(workspace: Path, maps_path: Path) -> list[dict]:
    inventory: list[dict] = []
    seen: set[str] = set()
    for row in parse_maps(maps_path):
        remote_path = row.get("path", "")
        if not remote_path or remote_path.startswith("["):
            continue
        if remote_path in seen:
            continue
        seen.add(remote_path)
        local = discover_local_binary(workspace, remote_path)
        entry = {
            "remote_path": remote_path,
            "map_start": f"0x{int(row['start'], 16):x}",
            "map_end": f"0x{int(row['end'], 16):x}",
            "perms": row.get("perms", ""),
            "map_offset": f"0x{int(row['offset'], 16):x}",
            "local_path": str(local) if local else "",
        }
        if local:
            entry.update(module_metadata(local))
        inventory.append(entry)
    return inventory


def symbolize_map(workspace: Path, map_obj: dict | None) -> dict | None:
    if not isinstance(map_obj, dict):
        return None
    remote_path = str(map_obj.get("path", "")).strip()
    offset = parse_hex(str(map_obj.get("offset", "")))
    if not remote_path or offset is None:
        return None
    local = discover_local_binary(workspace, remote_path)
    if not local:
        return {
            "remote_path": remote_path,
            "offset": f"0x{offset:x}",
            "rva": f"0x{offset:x}",
            "local_path": "",
            "symbol": "",
            "delta": "",
        }
    desc = file_description(str(local))
    rva = pe_file_offset_to_rva(str(local), offset) if "PE32" in desc or local.suffix.lower() in {".dll", ".exe"} else offset
    sym = nearest_symbol(str(local), rva)
    addr2line = list(addr2line_frames(str(local), rva))
    first_frame = addr2line[0] if addr2line else {}
    return {
        "remote_path": remote_path,
        "offset": f"0x{offset:x}",
        "rva": f"0x{rva:x}",
        "local_path": str(local),
        "symbol": first_frame.get("function") or (sym[0] if sym else ""),
        "delta": f"0x{sym[1]:x}" if sym else "",
        "source": first_frame.get("location", ""),
        "source_file": first_frame.get("source_file", ""),
        "source_line": first_frame.get("source_line", 0),
        "source_column": first_frame.get("source_column", 0),
        "inline_chain": addr2line,
    }


def symbolize_addr(workspace: Path, maps_rows: list[dict], addr: int | None) -> dict | None:
    return symbolize_map(workspace, find_map_for_addr(maps_rows, addr))


def decode_exception_record_candidate(workspace: Path, maps_rows: list[dict], window: dict) -> dict | None:
    words = parse_word_list(window.get("words"))
    if len(words) < 4:
        return None

    first = words[0]
    code = first & 0xFFFFFFFF
    flags = (first >> 32) & 0xFFFFFFFF
    nested = words[1]
    exception_addr = words[2]
    number_parameters = words[3] & 0xFFFFFFFF

    if not code or (flags & ~0x7F) or number_parameters > 15 or not exception_addr:
        return None

    exception_map = find_map_for_addr(maps_rows, exception_addr)
    if not exception_map:
        return None

    nested_symbolized = symbolize_addr(workspace, maps_rows, nested)
    exception_symbolized = symbolize_map(workspace, exception_map)
    return {
        "code": f"0x{code:08x}",
        "flags": f"0x{flags:x}",
        "nested_record": f"0x{nested:016x}",
        "nested_record_symbolized": nested_symbolized,
        "exception_address": f"0x{exception_addr:016x}",
        "exception_address_symbolized": exception_symbolized,
        "number_parameters": number_parameters,
    }


def summarize_file(workspace: Path, path: Path) -> tuple[list[str], list[dict]]:
    rows = read_jsonl(path)
    symbolized: list[dict] = []
    lines: list[str] = [f"[{path.name}]"]
    cmdline = read_text(path.with_suffix(".cmdline"))
    exe = read_text(path.with_suffix(".exe"))
    cwd = read_text(path.with_suffix(".cwd"))
    maps_rows = parse_maps(path.with_suffix(".maps"))
    modules = inventory_modules(workspace, path.with_suffix(".maps"))
    if not rows:
        lines.append("samples=0")
        return lines, symbolized

    pcs = Counter()
    lrs = Counter()
    comms = Counter()
    wchans = Counter()
    exception_candidates: Counter[tuple[str, str, str, str]] = Counter()

    for row in rows:
        pc_info = symbolize_map(workspace, row.get("pc_map"))
        lr_info = symbolize_map(workspace, row.get("lr_map"))
        frames = []
        for frame in row.get("frame_chain", []) or []:
            info = symbolize_map(workspace, frame.get("map"))
            frame_copy = dict(frame)
            frame_copy["symbolized"] = info
            frames.append(frame_copy)
        enriched = dict(row)
        enriched["pc_symbolized"] = pc_info
        enriched["lr_symbolized"] = lr_info
        enriched["pc_thunk"] = None
        enriched["lr_thunk"] = None
        if isinstance(row.get("pc_thunk_live"), dict):
            enriched["pc_thunk_live"] = dict(row["pc_thunk_live"])
            enriched["pc_thunk_live"]["slot_value_symbolized"] = symbolize_map(
                workspace, enriched["pc_thunk_live"].get("slot_value_map")
            )
        if isinstance(row.get("lr_thunk_live"), dict):
            enriched["lr_thunk_live"] = dict(row["lr_thunk_live"])
            enriched["lr_thunk_live"]["slot_value_symbolized"] = symbolize_map(
                workspace, enriched["lr_thunk_live"].get("slot_value_map")
            )
        if pc_info and pc_info.get("local_path") and pc_info.get("rva"):
            enriched["pc_thunk"] = decode_arm64_import_thunk(
                str(pc_info["local_path"]),
                int(str(pc_info["rva"]), 16),
                row.get("pc_words"),
            )
        if lr_info and lr_info.get("local_path") and lr_info.get("rva"):
            enriched["lr_thunk"] = decode_arm64_import_thunk(
                str(lr_info["local_path"]),
                int(str(lr_info["rva"]), 16),
                row.get("lr_words"),
            )
        pointer_windows = []
        for window in row.get("pointer_windows", []) or []:
            pointer_copy = dict(window)
            symbolized_ptr = symbolize_map(workspace, pointer_copy.get("map"))
            if symbolized_ptr:
                pointer_copy["symbolized"] = symbolized_ptr
            candidate = decode_exception_record_candidate(workspace, maps_rows, pointer_copy)
            if candidate:
                pointer_copy["exception_candidate"] = candidate
                exception_symbolized = candidate.get("exception_address_symbolized") or {}
                exception_candidates[
                    (
                        str(pointer_copy.get("reg", "")),
                        str(candidate.get("code", "")),
                        str(exception_symbolized.get("symbol", "")),
                        str(candidate.get("exception_address", "")),
                    )
                ] += 1
            pointer_windows.append(pointer_copy)
        enriched["pointer_windows_symbolized"] = pointer_windows
        enriched["frame_chain_symbolized"] = frames
        symbolized.append(enriched)

        if pc_info:
            pcs[(pc_info.get("symbol", ""), pc_info.get("delta", ""), pc_info.get("remote_path", ""), pc_info.get("offset", ""))] += 1
        if lr_info:
            lrs[(lr_info.get("symbol", ""), lr_info.get("delta", ""), lr_info.get("remote_path", ""), lr_info.get("offset", ""))] += 1
        comms[str(row.get("comm", ""))] += 1
        wchans[str(row.get("wchan", ""))] += 1

    top_pc, top_pc_count = pcs.most_common(1)[0] if pcs else (("", "", "", ""), 0)
    top_lr, top_lr_count = lrs.most_common(1)[0] if lrs else (("", "", "", ""), 0)
    first = symbolized[0]

    lines.extend(
        [
            f"samples={len(rows)}",
            f"cmdline={cmdline}",
            f"exe={exe}",
            f"cwd={cwd}",
            f"stable_pc={1 if top_pc_count == len(rows) and len(rows) > 0 else 0}",
            f"stable_lr={1 if top_lr_count == len(rows) and len(rows) > 0 else 0}",
            f"comm={comms.most_common(1)[0][0] if comms else ''}",
            f"wchan={wchans.most_common(1)[0][0] if wchans else ''}",
            f"pc={top_pc[0]}{top_pc[1] and '+' + top_pc[1] or ''} @ {top_pc[3]}",
            f"lr={top_lr[0]}{top_lr[1] and '+' + top_lr[1] or ''} @ {top_lr[3]}",
            f"module_count={len(modules)}",
        ]
    )

    first_pc = first.get("pc_symbolized") or {}
    first_lr = first.get("lr_symbolized") or {}
    if first_pc.get("source"):
        lines.append(f"pc_source={first_pc['source']}")
    if first_lr.get("source"):
        lines.append(f"lr_source={first_lr['source']}")

    frame_lines = []
    for frame in first.get("frame_chain_symbolized", [])[:FRAME_CHAIN_LIMIT]:
        info = frame.get("symbolized") or {}
        symbol = str(info.get("symbol", "")).strip() or "?"
        delta = str(info.get("delta", "")).strip()
        offset = ""
        if isinstance(frame.get("map"), dict):
            offset = str(frame["map"].get("offset", "")).strip()
        rva = ""
        if frame.get("symbolized"):
            rva = str(frame["symbolized"].get("rva", "")).strip()
        source = ""
        if frame.get("symbolized"):
            source = str(frame["symbolized"].get("source", "")).strip()
        suffix = f" @ {offset}"
        if rva and rva != offset:
            suffix += f" (rva {rva})"
        if source:
            suffix += f" {source}"
        frame_lines.append(f"frame[{frame.get('depth', '?')}]={symbol}{delta and '+' + delta or ''}{suffix}")
    if frame_lines:
        lines.extend(frame_lines)

    if exception_candidates:
        lines.append(f"exception_candidate_count={sum(exception_candidates.values())}")
        for idx, ((reg, code, symbol, addr), count) in enumerate(exception_candidates.most_common(5), 1):
            pretty = symbol or "?"
            lines.append(
                f"exception_candidate[{idx}]={reg} code={code} addr={addr} symbol={pretty} samples={count}"
            )

    for side in ("pc", "lr"):
        thunk = first.get(f"{side}_thunk") or {}
        if not thunk:
            continue
        owners = ",".join(thunk.get("owner_dlls", []))
        lines.append(
            f"{side}_thunk={thunk.get('kind','')} "
            f"slot={thunk.get('slot_symbol','')} "
            f"slot_rva={thunk.get('slot_rva','')} "
            f"import={thunk.get('import_symbol','')} "
            f"owners={owners}"
        )
        live = first.get(f"{side}_thunk_live") or {}
        if live:
            target = live.get("slot_value_symbolized") or {}
            lines.append(
                f"{side}_thunk_live="
                f"slot_addr={live.get('slot_addr','')} "
                f"slot_value={live.get('slot_value','')} "
                f"target={target.get('symbol','')}{target.get('delta') and '+' + str(target.get('delta')) or ''} "
                f"target_rva={target.get('rva','')} "
                f"target_module={target.get('remote_path','')}"
            )

    return lines, symbolized


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenario-dir", required=True)
    parser.add_argument("--workspace", default="/data/data/com.termux/files/home")
    args = parser.parse_args()

    scenario_dir = Path(args.scenario_dir).resolve()
    workspace = Path(args.workspace).resolve()
    runas_dir = scenario_dir / "runas-ptrace"
    summary_path = scenario_dir / "runas-ptrace.summary.txt"
    symbolized_dir = scenario_dir / "runas-ptrace-symbolized"
    symbolized_dir.mkdir(parents=True, exist_ok=True)
    revdbg_summary_path = scenario_dir / "runas-revdbg.summary.txt"

    blocks: list[str] = []
    revdbg_blocks: list[str] = []
    for path in sorted(runas_dir.glob("*.jsonl")):
        lines, rows = summarize_file(workspace, path)
        blocks.append("\n".join(lines))
        (symbolized_dir / f"{path.stem}.json").write_text(json.dumps(rows, indent=2), encoding="utf-8")
        modules = inventory_modules(workspace, path.with_suffix(".maps"))
        (symbolized_dir / f"{path.stem}.modules.json").write_text(json.dumps(modules, indent=2), encoding="utf-8")
        revdbg_lines = [f"[{path.name}]"]
        revdbg_lines.append(f"cmdline={read_text(path.with_suffix('.cmdline'))}")
        revdbg_lines.append(f"exe={read_text(path.with_suffix('.exe'))}")
        revdbg_lines.append(f"cwd={read_text(path.with_suffix('.cwd'))}")
        revdbg_lines.append(f"module_count={len(modules)}")
        for module in modules[:20]:
            line = (
                f"module={module.get('remote_path','')} "
                f"format={module.get('format','')} "
                f"machine={module.get('machine','')} "
                f"local={module.get('local_path','')}"
            )
            if module.get("soname"):
                line += f" soname={module['soname']}"
            if module.get("imports"):
                line += f" imports={','.join(module['imports'][:8])}"
            if module.get("needed"):
                line += f" needed={','.join(module['needed'][:8])}"
            if module.get("export_count") is not None:
                line += f" exports={module.get('export_count', 0)}"
            revdbg_lines.append(line)
        revdbg_blocks.append("\n".join(revdbg_lines))

    if not blocks:
        summary_path.write_text("runas_ptrace_summary\nsamples=0\n", encoding="utf-8")
        revdbg_summary_path.write_text("runas_revdbg_summary\nsamples=0\n", encoding="utf-8")
        return 0

    summary_path.write_text("runas_ptrace_summary\n\n" + "\n\n".join(blocks) + "\n", encoding="utf-8")
    revdbg_summary_path.write_text("runas_revdbg_summary\n\n" + "\n\n".join(revdbg_blocks) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
