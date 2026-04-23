#!/data/data/com.termux/files/usr/bin/python3
from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
import re
import sys
import time
from dataclasses import dataclass, asdict
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen


USER_AGENT = "ae-source-intake-recon/1.0"
DEFAULT_MAX_PAGES = 8
DEFAULT_MAX_ARTIFACTS = 48
DEFAULT_MAX_DEPTH = 2
MAX_FETCH_BYTES = 2 * 1024 * 1024

SCRIPT_RE = re.compile(r"(?is)<script\b[^>]*>(.*?)</script>")
SOURCEMAP_RE = re.compile(r"(?m)^[ \t]*//[#@][ \t]*sourceMappingURL=([^\s]+)[ \t]*$")
SERVICE_WORKER_RE = re.compile(r"serviceWorker\.register\(\s*['\"]([^'\"]+)['\"]")
URL_RE = re.compile(r"""(?ix)
    (?:
      https?://[^\s'"<>`]+ |
      /[A-Za-z0-9._~:/?#\[\]@!$&()*+,;=%-]{2,}
    )
""")

AUTH_PATTERNS = [
    r"\bauth\b",
    r"\blogin\b",
    r"\bsignin\b",
    r"\bsign-in\b",
    r"\boauth\b",
    r"\btoken\b",
    r"\bsession\b",
    r"authorization",
]
CAPTCHA_PATTERNS = [
    r"recaptcha",
    r"hcaptcha",
    r"turnstile",
    r"\bcaptcha\b",
    r"\bchallenge\b",
    r"cf-chl",
]
FINGERPRINT_PATTERNS = [
    r"fingerprint",
    r"canvas",
    r"webgl",
    r"audiocontext",
    r"useragentdata",
    r"navigator\.plugins",
    r"navigator\.hardwareconcurrency",
]
ENCRYPTION_PATTERNS = [
    r"crypto\.subtle",
    r"sha-?256",
    r"\baes\b",
    r"\brsa\b",
    r"encrypt",
    r"decrypt",
    r"\batob\b",
    r"\bbtoa\b",
]
API_HINT_PATTERNS = [
    r"/api/",
    r"/graphql",
    r"/rest/",
    r"/v[0-9]+/",
    r"/oauth/",
    r"/auth/",
]


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8", errors="ignore")).hexdigest()


def sanitize_name(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", value).strip("-")
    return cleaned[:96] or "artifact"


def same_origin(a: str, b: str) -> bool:
    pa = urlparse(a)
    pb = urlparse(b)
    return (pa.scheme, pa.netloc) == (pb.scheme, pb.netloc)


def safe_urljoin(base_url: str, raw: str) -> str | None:
    try:
        resolved = urljoin(base_url, raw)
        parsed = urlparse(resolved)
    except ValueError:
        return None
    if parsed.scheme not in {"http", "https"}:
        return None
    return resolved


def is_html_content_type(content_type: str) -> bool:
    return "text/html" in content_type or "application/xhtml+xml" in content_type


def is_text_artifact_url(url: str) -> bool:
    path = urlparse(url).path.lower()
    return path.endswith((".js", ".mjs", ".cjs", ".map", ".json", ".wasm", ".txt", ".html", ".htm"))


def compile_patterns(patterns: Iterable[str]) -> list[re.Pattern[str]]:
    return [re.compile(pattern, re.IGNORECASE) for pattern in patterns]


AUTH_RE = compile_patterns(AUTH_PATTERNS)
CAPTCHA_RE = compile_patterns(CAPTCHA_PATTERNS)
FINGERPRINT_RE = compile_patterns(FINGERPRINT_PATTERNS)
ENCRYPTION_RE = compile_patterns(ENCRYPTION_PATTERNS)
API_HINT_RE = compile_patterns(API_HINT_PATTERNS)


@dataclass
class FetchRecord:
    url: str
    final_url: str
    status: int
    content_type: str
    depth: int
    discovered_from: str
    kind: str
    size_bytes: int
    sha256: str
    local_path: str


class DiscoveryParser(HTMLParser):
    def __init__(self, page_url: str) -> None:
        super().__init__(convert_charrefs=True)
        self.page_url = page_url
        self.page_links: set[str] = set()
        self.artifact_links: set[str] = set()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attr_map = {key.lower(): value for key, value in attrs}
        if tag == "a":
            href = attr_map.get("href")
            if href:
                self._add_link(href, html_page=True)
        elif tag in {"script", "iframe"}:
            src = attr_map.get("src")
            if src:
                self._add_link(src, html_page=(tag == "iframe"))
        elif tag == "link":
            href = attr_map.get("href")
            rel = (attr_map.get("rel") or "").lower()
            if href:
                self._add_link(href, html_page=("preload" not in rel and href.endswith((".html", ".htm"))))

    def _add_link(self, raw: str, html_page: bool) -> None:
        resolved = urljoin(self.page_url, raw)
        parsed = urlparse(resolved)
        if parsed.scheme not in {"http", "https"}:
            return
        if html_page:
            self.page_links.add(resolved)
        else:
            self.artifact_links.add(resolved)


class SourceIntakeRecon:
    def __init__(self, max_pages: int, max_artifacts: int, max_depth: int, max_runtime_s: int, output_dir: Path) -> None:
        self.max_pages = max_pages
        self.max_artifacts = max_artifacts
        self.max_depth = max_depth
        self.max_runtime_s = max_runtime_s
        self.output_dir = output_dir
        self.raw_dir = output_dir / "artifacts" / "raw"
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self._started_monotonic = time.monotonic()

        self._visited_pages: set[str] = set()
        self._visited_artifacts: set[str] = set()
        self._records: list[FetchRecord] = []
        self._errors: list[dict[str, str]] = []
        self._service_worker_urls: set[str] = set()
        self._sourcemap_urls: set[str] = set()
        self._detected_urls: set[str] = set()
        self._auth_hits: set[str] = set()
        self._captcha_hits: set[str] = set()
        self._fingerprint_hits: set[str] = set()
        self._encryption_hits: set[str] = set()
        self._api_hits: set[str] = set()

    def deadline_exceeded(self) -> bool:
        return time.monotonic() - self._started_monotonic >= self.max_runtime_s

    def remaining_runtime_s(self) -> float:
        return max(1.0, self.max_runtime_s - (time.monotonic() - self._started_monotonic))

    def fetch_text(self, url: str) -> tuple[str, str, int, str]:
        req = Request(url, headers={"User-Agent": USER_AGENT})
        timeout_s = min(20.0, self.remaining_runtime_s())
        with urlopen(req, timeout=timeout_s) as resp:
            final_url = resp.geturl()
            status = getattr(resp, "status", 200)
            content_type = resp.headers.get("Content-Type", "")
            body = resp.read(MAX_FETCH_BYTES + 1)
        if len(body) > MAX_FETCH_BYTES:
            body = body[:MAX_FETCH_BYTES]
        encoding = "utf-8"
        text = body.decode(encoding, errors="replace")
        return text, final_url, status, content_type

    def write_artifact(self, url: str, text: str) -> str:
        parsed = urlparse(url)
        base = sanitize_name(parsed.netloc + "-" + (Path(parsed.path).name or "index"))
        digest = sha256_text(text)[:12]
        filename = f"{base}-{digest}.txt"
        path = self.raw_dir / filename
        path.write_text(text, encoding="utf-8")
        return str(path.relative_to(self.output_dir))

    def record_text_artifact(
        self,
        url: str,
        text: str,
        final_url: str,
        status: int,
        content_type: str,
        depth: int,
        discovered_from: str,
        kind: str,
    ) -> None:
        local_path = self.write_artifact(final_url, text)
        self._records.append(
            FetchRecord(
                url=url,
                final_url=final_url,
                status=status,
                content_type=content_type,
                depth=depth,
                discovered_from=discovered_from,
                kind=kind,
                size_bytes=len(text.encode("utf-8", errors="ignore")),
                sha256=sha256_text(text),
                local_path=local_path,
            )
        )
        self.scan_text(final_url, text)

    def scan_text(self, base_url: str, text: str) -> None:
        for match in SERVICE_WORKER_RE.findall(text):
            resolved = safe_urljoin(base_url, html.unescape(match))
            if resolved:
                self._service_worker_urls.add(resolved)
        for match in SOURCEMAP_RE.findall(text):
            resolved = safe_urljoin(base_url, html.unescape(match))
            if resolved:
                self._sourcemap_urls.add(resolved)
        for match in URL_RE.findall(text):
            resolved = safe_urljoin(base_url, html.unescape(match))
            if resolved:
                self._detected_urls.add(resolved)
        self._auth_hits.update(self.find_pattern_hits(text, AUTH_RE))
        self._captcha_hits.update(self.find_pattern_hits(text, CAPTCHA_RE))
        self._fingerprint_hits.update(self.find_pattern_hits(text, FINGERPRINT_RE))
        self._encryption_hits.update(self.find_pattern_hits(text, ENCRYPTION_RE))
        self._api_hits.update(self.find_pattern_hits(text, API_HINT_RE))

    @staticmethod
    def find_pattern_hits(text: str, patterns: list[re.Pattern[str]]) -> set[str]:
        hits: set[str] = set()
        for pattern in patterns:
            for match in pattern.finditer(text):
                start = max(0, match.start() - 48)
                end = min(len(text), match.end() + 48)
                snippet = re.sub(r"\s+", " ", text[start:end]).strip()
                if snippet:
                    hits.add(snippet[:220])
        return hits

    def crawl(self, start_url: str) -> None:
        root = start_url
        queue: list[tuple[str, int, str]] = [(start_url, 0, "root")]
        while queue and len(self._visited_pages) < self.max_pages:
            if self.deadline_exceeded():
                self._errors.append({"url": start_url, "stage": "timeout", "error": f"runtime budget exceeded ({self.max_runtime_s}s)"})
                break
            url, depth, discovered_from = queue.pop(0)
            if url in self._visited_pages or depth > self.max_depth:
                continue
            if not same_origin(root, url):
                continue
            self._visited_pages.add(url)
            try:
                text, final_url, status, content_type = self.fetch_text(url)
            except (HTTPError, URLError, TimeoutError, ValueError) as exc:
                self._errors.append({"url": url, "stage": "page", "error": str(exc)})
                continue

            if not is_html_content_type(content_type):
                self.record_text_artifact(url, text, final_url, status, content_type, depth, discovered_from, "artifact")
                continue

            self.record_text_artifact(url, text, final_url, status, content_type, depth, discovered_from, "page")
            parser = DiscoveryParser(final_url)
            parser.feed(text)

            inline_scripts = SCRIPT_RE.findall(text)
            for index, script in enumerate(inline_scripts, start=1):
                if script.strip():
                    synthetic_url = f"{final_url}#inline-script-{index}"
                    self.scan_text(synthetic_url, script)

            for next_url in sorted(parser.page_links):
                if len(self._visited_pages) + len(queue) >= self.max_pages:
                    break
                if same_origin(root, next_url) and next_url not in self._visited_pages:
                    queue.append((next_url, depth + 1, final_url))

            artifact_candidates = sorted(parser.artifact_links | self._service_worker_urls | self._sourcemap_urls)
            for artifact_url in artifact_candidates:
                if len(self._visited_artifacts) >= self.max_artifacts:
                    break
                self.fetch_artifact(root, artifact_url, depth + 1, final_url)

    def fetch_artifact(self, root: str, url: str, depth: int, discovered_from: str) -> None:
        if url in self._visited_artifacts or depth > self.max_depth:
            return
        if self.deadline_exceeded():
            self._errors.append({"url": url, "stage": "timeout", "error": f"runtime budget exceeded ({self.max_runtime_s}s)"})
            return
        if not same_origin(root, url):
            return
        if not is_text_artifact_url(url):
            return
        self._visited_artifacts.add(url)
        try:
            text, final_url, status, content_type = self.fetch_text(url)
        except (HTTPError, URLError, TimeoutError, ValueError) as exc:
            self._errors.append({"url": url, "stage": "artifact", "error": str(exc)})
            return
        self.record_text_artifact(url, text, final_url, status, content_type, depth, discovered_from, "artifact")

    def build_report(self, id_value: str, source_label: str, start_url: str, download_url: str | None) -> str:
        page_records = [record for record in self._records if record.kind == "page"]
        artifact_records = [record for record in self._records if record.kind == "artifact"]
        lines = [
            "# Source Intake Recon",
            "",
            f"- id: `{id_value}`",
            f"- source_label: `{source_label}`",
            f"- start_url: {start_url}",
        ]
        if download_url:
            lines.append(f"- download_url: {download_url}")
        lines.extend(
            [
                f"- pages: {len(page_records)}",
                f"- artifacts: {len(artifact_records)}",
                f"- errors: {len(self._errors)}",
                "",
                "## Surface",
                "",
                f"- service_worker_urls: {len(self._service_worker_urls)}",
                f"- sourcemap_urls: {len(self._sourcemap_urls)}",
                f"- detected_urls: {len(self._detected_urls)}",
                f"- auth_hits: {len(self._auth_hits)}",
                f"- captcha_hits: {len(self._captcha_hits)}",
                f"- fingerprint_hits: {len(self._fingerprint_hits)}",
                f"- encryption_hits: {len(self._encryption_hits)}",
                f"- api_hits: {len(self._api_hits)}",
                "",
                "## Records",
                "",
            ]
        )
        for record in self._records:
            lines.append(
                f"- [{record.kind}] depth={record.depth} status={record.status} "
                f"url={record.final_url} local={record.local_path}"
            )
        if self._errors:
            lines.extend(["", "## Errors", ""])
            for error in self._errors:
                lines.append(f"- {error['stage']}: {error['url']} -> {error['error']}")
        return "\n".join(lines) + "\n"

    def build_recommendations(self, start_url: str, download_url: str | None) -> str:
        source_host = urlparse(start_url).netloc
        download_host = urlparse(download_url).netloc if download_url else ""
        lines = [
            "# Intake Recommendations",
            "",
            "## Contract",
            "",
            "- Treat this dossier as a staged intake proof. Do not pin a new catalog row",
            "  or overwrite provenance until the source page, download URL, and fetched",
            "  surface all agree on what the payload is and how it is delivered.",
            "",
            "## Findings",
            "",
        ]

        findings: list[str] = []
        if download_url and download_host and download_host != source_host:
            findings.append(
                f"download host differs from source host: `{source_host}` -> `{download_host}`;"
                " keep both URLs in the catalog so provenance remains inspectable"
            )
        if self._captcha_hits:
            findings.append("captcha/challenge markers detected; unattended fetch may be brittle")
        if self._auth_hits:
            findings.append("auth/session markers detected; page behavior may vary by client state")
        if self._service_worker_urls:
            findings.append("service worker surface detected; source page may hydrate or rewrite links at runtime")
        if self._sourcemap_urls:
            findings.append("source maps detected; keep them for later provenance or JS-side inspection")
        if not self._records:
            findings.append("no same-origin analyzable records were captured; manual provenance review is required")
        if self._errors:
            findings.append("fetch errors occurred; unresolved pages/artifacts may still hide important surface")
        if not findings:
            findings.append("no immediate intake blockers detected in the bounded same-origin crawl")

        for finding in findings:
            lines.append(f"- {finding}")

        lines.extend(
            [
                "",
                "## Next Actions",
                "",
                "- Keep the human-readable source page URL and the concrete download URL together.",
                "- If the download endpoint lives on a different host, pin checksum and provenance in the same pass.",
                "- Review `surface.json`, `records.tsv`, and `errors.json` before updating catalog rows.",
            ]
        )
        return "\n".join(lines) + "\n"

    def build_records_tsv(self) -> str:
        header = [
            "kind",
            "depth",
            "status",
            "discovered_from",
            "url",
            "final_url",
            "content_type",
            "size_bytes",
            "sha256",
            "local_path",
        ]
        rows = ["\t".join(header)]
        for record in self._records:
            rows.append(
                "\t".join(
                    [
                        record.kind,
                        str(record.depth),
                        str(record.status),
                        record.discovered_from,
                        record.url,
                        record.final_url,
                        record.content_type,
                        str(record.size_bytes),
                        record.sha256,
                        record.local_path,
                    ]
                )
            )
        return "\n".join(rows) + "\n"

    def write_outputs(self, id_value: str, source_label: str, start_url: str, download_url: str | None) -> None:
        surface = {
            "service_worker_urls": sorted(self._service_worker_urls),
            "sourcemap_urls": sorted(self._sourcemap_urls),
            "detected_urls": sorted(self._detected_urls),
            "auth_hits": sorted(self._auth_hits),
            "captcha_hits": sorted(self._captcha_hits),
            "fingerprint_hits": sorted(self._fingerprint_hits),
            "encryption_hits": sorted(self._encryption_hits),
            "api_hits": sorted(self._api_hits),
        }
        metadata = {
            "id": id_value,
            "source_label": source_label,
            "start_url": start_url,
            "download_url": download_url,
            "max_pages": self.max_pages,
            "max_artifacts": self.max_artifacts,
            "max_depth": self.max_depth,
            "max_runtime_s": self.max_runtime_s,
        }
        (self.output_dir / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
        (self.output_dir / "surface.json").write_text(json.dumps(surface, indent=2) + "\n", encoding="utf-8")
        (self.output_dir / "records.json").write_text(
            json.dumps([asdict(record) for record in self._records], indent=2) + "\n",
            encoding="utf-8",
        )
        (self.output_dir / "records.tsv").write_text(self.build_records_tsv(), encoding="utf-8")
        (self.output_dir / "errors.json").write_text(json.dumps(self._errors, indent=2) + "\n", encoding="utf-8")
        (self.output_dir / "report.md").write_text(
            self.build_report(id_value, source_label, start_url, download_url),
            encoding="utf-8",
        )
        (self.output_dir / "recommendations.md").write_text(
            self.build_recommendations(start_url, download_url),
            encoding="utf-8",
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Bionic-safe source-page recon for web-backed payload intake.")
    parser.add_argument("--id", required=True)
    parser.add_argument("--url", required=True)
    parser.add_argument("--source-label", default="")
    parser.add_argument("--download-url", default="")
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--max-pages", type=int, default=DEFAULT_MAX_PAGES)
    parser.add_argument("--max-artifacts", type=int, default=DEFAULT_MAX_ARTIFACTS)
    parser.add_argument("--max-depth", type=int, default=DEFAULT_MAX_DEPTH)
    parser.add_argument("--max-runtime-s", type=int, default=60)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    recon = SourceIntakeRecon(
        max_pages=args.max_pages,
        max_artifacts=args.max_artifacts,
        max_depth=args.max_depth,
        max_runtime_s=args.max_runtime_s,
        output_dir=output_dir,
    )
    exit_code = 0
    try:
        recon.crawl(args.url)
    except Exception as exc:
        recon._errors.append({"url": args.url, "stage": "fatal", "error": str(exc)})
        exit_code = 1
    recon.write_outputs(args.id, args.source_label, args.url, args.download_url or None)
    print(str(output_dir))
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
