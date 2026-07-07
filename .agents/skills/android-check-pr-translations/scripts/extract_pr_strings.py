#!/usr/bin/env python3
"""
Extract translated text changes from a PR diff and resolve each change against
the English source.

Usage:
    python3 extract_pr_strings.py <pr-number>
    python3 extract_pr_strings.py <pr-number> --output /tmp/pr-strings.json

Run from the repository root. Output: JSON with the structure:
{
  "pr_number": <int>,
  "locales": {
    "values-xx": {
      "added": [{"key": "...", "value": "...", "english": "..."}],
      "removed": [{"key": "...", "value": "...", "english": "..."}]
    }
  },
  "files": [
    {
      "path": "guides/xx/file.txt",
      "locale": "xx",
      "english_path": "guides/en-US/file.txt",
      "english": "...",
      "added": [{"hunk": "...", "lines": ["..."]}],
      "removed": [{"hunk": "...", "lines": ["..."]}]
    }
  ]
}
"""

import sys
import re
import json
import argparse
import subprocess
from pathlib import Path

ENGLISH_STRINGS_PATH = Path("app/src/main/res/values/strings.xml")

# Matches a complete <string name="key">value</string> on one line
STRING_RE = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.DOTALL)
# Matches the locale directory from a diff header line
LOCALE_RE = re.compile(r'diff --git .+ b/app/src/main/res/(values-[^/]+)/strings\.xml')
FILE_RE = re.compile(r'diff --git a/(.*?) b/(.*)')
GUIDE_LOCALE_RE = re.compile(r'^guides/([^/]+)/(.+\.txt)$')
SOURCE_GUIDE_LOCALE = "en-US"


def load_english_strings() -> dict[str, str]:
    """Parse the English strings.xml and return a {key: value} mapping."""
    if not ENGLISH_STRINGS_PATH.exists():
        print(f"Warning: English strings not found at {ENGLISH_STRINGS_PATH}", file=sys.stderr)
        return {}
    text = ENGLISH_STRINGS_PATH.read_text(encoding="utf-8")
    return {m.group(1): m.group(2) for m in STRING_RE.finditer(text)}


def parse_string_diff(diff_text: str, english: dict[str, str]) -> dict:
    """Parse a unified diff and return added/removed strings per locale."""
    locales: dict = {}
    current_locale: str | None = None
    # Buffer for accumulating potentially multi-line diff lines
    added_buf: list[str] = []
    removed_buf: list[str] = []

    def flush_buffer(buf: list[str], target_list: list):
        combined = "".join(buf).strip()
        for m in STRING_RE.finditer(combined):
            key = m.group(1)
            target_list.append({"key": key, "value": m.group(2), "english": english.get(key, "")})
        buf.clear()

    for raw_line in diff_text.splitlines(keepends=True):
        # New file header — flush buffers and switch locale
        locale_match = LOCALE_RE.search(raw_line)
        if locale_match:
            if current_locale:
                flush_buffer(added_buf, locales[current_locale]["added"])
                flush_buffer(removed_buf, locales[current_locale]["removed"])
            current_locale = locale_match.group(1)
            locales.setdefault(current_locale, {"added": [], "removed": []})
            added_buf.clear()
            removed_buf.clear()
            continue

        if current_locale is None:
            continue

        if raw_line.startswith("+++ ") or raw_line.startswith("--- "):
            continue

        if raw_line.startswith("+"):
            # Flush any pending removed buffer when we switch sides
            if removed_buf:
                flush_buffer(removed_buf, locales[current_locale]["removed"])
            content = raw_line[1:]
            added_buf.append(content)
            # Flush if the buffer now contains a complete string element
            if "</string>" in content:
                flush_buffer(added_buf, locales[current_locale]["added"])

        elif raw_line.startswith("-"):
            # Flush any pending added buffer when we switch sides
            if added_buf:
                flush_buffer(added_buf, locales[current_locale]["added"])
            content = raw_line[1:]
            removed_buf.append(content)
            if "</string>" in content:
                flush_buffer(removed_buf, locales[current_locale]["removed"])

        else:
            # Context line — flush both buffers
            if added_buf:
                flush_buffer(added_buf, locales[current_locale]["added"])
            if removed_buf:
                flush_buffer(removed_buf, locales[current_locale]["removed"])

    # Final flush
    if current_locale:
        flush_buffer(added_buf, locales[current_locale]["added"])
        flush_buffer(removed_buf, locales[current_locale]["removed"])

    # Drop locales with no changes
    return {k: v for k, v in locales.items() if v["added"] or v["removed"]}


def is_translated_guide(path: str) -> bool:
    """Return True for localized guide text files that have an English source."""
    match = GUIDE_LOCALE_RE.match(path)
    return bool(match and match.group(1) != SOURCE_GUIDE_LOCALE)


def english_guide_path(path: str) -> Path:
    """Map guides/<locale>/<file>.txt to guides/en-US/<file>.txt."""
    match = GUIDE_LOCALE_RE.match(path)
    if not match:
        return Path()
    return Path("guides") / SOURCE_GUIDE_LOCALE / match.group(2)


def load_text(path: Path) -> str:
    """Read text if present, otherwise return an empty source marker."""
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def parse_guide_diff(diff_text: str) -> list[dict]:
    """Parse localized guide/tool keyword text-file changes from a unified diff."""
    files: list[dict] = []
    current: dict | None = None
    current_kind: str | None = None
    current_block: list[str] = []
    current_hunk: str = ""

    def flush_block():
        nonlocal current_kind, current_block
        if current is None or current_kind is None or not current_block:
            current_kind = None
            current_block = []
            return
        current[current_kind].append({"hunk": current_hunk, "lines": current_block})
        current_kind = None
        current_block = []

    def start_file(path: str):
        nonlocal current, current_kind, current_block, current_hunk
        flush_block()
        if current and (current["added"] or current["removed"]):
            files.append(current)

        current_kind = None
        current_block = []
        current_hunk = ""

        if not is_translated_guide(path):
            current = None
            return

        locale = GUIDE_LOCALE_RE.match(path).group(1)  # type: ignore[union-attr]
        english_path = english_guide_path(path)
        current = {
            "path": path,
            "locale": locale,
            "english_path": str(english_path),
            "english": load_text(english_path),
            "added": [],
            "removed": [],
        }

    for raw_line in diff_text.splitlines():
        file_match = FILE_RE.match(raw_line)
        if file_match:
            start_file(file_match.group(2))
            continue

        if current is None:
            continue

        if raw_line.startswith("@@"):
            flush_block()
            current_hunk = raw_line
            continue

        if raw_line.startswith("+++ ") or raw_line.startswith("--- "):
            continue

        if raw_line.startswith("+"):
            if current_kind != "added":
                flush_block()
                current_kind = "added"
            current_block.append(raw_line[1:])
        elif raw_line.startswith("-"):
            if current_kind != "removed":
                flush_block()
                current_kind = "removed"
            current_block.append(raw_line[1:])
        else:
            flush_block()

    flush_block()
    if current and (current["added"] or current["removed"]):
        files.append(current)

    return files


def main():
    parser = argparse.ArgumentParser(
        description="Extract translated text changes from a PR diff."
    )
    parser.add_argument("pr_number", type=int)
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Write JSON to this file instead of stdout.",
    )
    args = parser.parse_args()

    pr_number = args.pr_number

    result = subprocess.run(
        ["gh", "pr", "diff", str(pr_number)],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"Error fetching PR diff: {result.stderr}", file=sys.stderr)
        sys.exit(1)

    english = load_english_strings()
    locales = parse_string_diff(result.stdout, english)
    files = parse_guide_diff(result.stdout)
    output = {"pr_number": pr_number, "locales": locales, "files": files}
    output_text = json.dumps(output, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.write_text(output_text, encoding="utf-8")
    else:
        print(output_text, end="")


if __name__ == "__main__":
    main()
