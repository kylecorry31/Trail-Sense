#!/usr/bin/env python3
"""
Extract added and removed <string> entries from strings.xml files in a PR diff,
also resolving each key against the English source strings.xml.

Usage:
    python3 extract_pr_strings.py <pr-number>

Run from the repository root. Output: JSON with the structure:
{
  "pr_number": <int>,
  "locales": {
    "values-xx": {
      "added": [{"key": "...", "value": "...", "english": "..."}],
      "removed": [{"key": "...", "value": "...", "english": "..."}]
    }
  }
}
"""

import sys
import re
import json
import subprocess
from pathlib import Path

ENGLISH_STRINGS_PATH = Path("app/src/main/res/values/strings.xml")

# Matches a complete <string name="key">value</string> on one line
STRING_RE = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.DOTALL)
# Matches the locale directory from a diff header line
LOCALE_RE = re.compile(r'diff --git .+ b/app/src/main/res/(values-[^/]+)/strings\.xml')


def load_english_strings() -> dict[str, str]:
    """Parse the English strings.xml and return a {key: value} mapping."""
    if not ENGLISH_STRINGS_PATH.exists():
        print(f"Warning: English strings not found at {ENGLISH_STRINGS_PATH}", file=sys.stderr)
        return {}
    text = ENGLISH_STRINGS_PATH.read_text(encoding="utf-8")
    return {m.group(1): m.group(2) for m in STRING_RE.finditer(text)}


def parse_diff(diff_text: str, english: dict[str, str]) -> dict:
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


def main():
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <pr-number>", file=sys.stderr)
        sys.exit(1)

    pr_number = int(sys.argv[1])

    result = subprocess.run(
        ["gh", "pr", "diff", str(pr_number)],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"Error fetching PR diff: {result.stderr}", file=sys.stderr)
        sys.exit(1)

    english = load_english_strings()
    locales = parse_diff(result.stdout, english)
    output = {"pr_number": pr_number, "locales": locales}
    print(json.dumps(output, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
