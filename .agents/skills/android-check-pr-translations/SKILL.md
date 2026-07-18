---
name: android-check-pr-translations
description: Audit changed Android translations in a GitHub PR.
disable-model-invocation: true
---

# Check PR Translations

Audit the translation accuracy of Android string resources and localized guide files changed in a GitHub pull request.

## Workflow

1. Run the extraction script from the skill's folder to get the changed translated content from the PR:
   ```
   python3 scripts/extract_pr_strings.py <pr-number> --output /tmp/pr-strings.json
   ```
   Read the extracted JSON from `/tmp/pr-strings.json`.
   This step is complete when the output includes:
   - `locales`: changed translated `<string>` resources with the English source for each key.
   - `files`: changed localized guide `.txt` files, including guide pages, field guides, and `tool_keywords.txt`, with the matching `guides/en-US/...` source text.

2. Use the /translation-review skill to review each of the locales and files translations. Include the string key and locale name in your finding so I can identify which language has the inaccurate translation.

## Extraction Script: `scripts/extract_pr_strings.py` (skill-local)

The script parses the output of `gh pr diff` and extracts:
- Added and removed `<string>` elements from `res/values-*/strings.xml` patch hunks.
- Added and removed lines from localized guide text files under `guides/<locale>/*.txt`, excluding `guides/en-US`.

**Usage:**
```
python3 scripts/extract_pr_strings.py <pr-number> --output /tmp/pr-strings.json
```

**Output schema:**
```json
{
  "pr_number": 1234,
  "locales": {
    "values-pl": {
      "added":   [{"key": "some_key", "value": "translated text", "english": "English source text"}],
      "removed": [{"key": "old_key",  "value": "old translated text", "english": "English source text"}]
    }
  },
  "files": [
    {
      "path": "guides/pl-rPL/tool_keywords.txt",
      "locale": "pl-rPL",
      "english_path": "guides/en-US/tool_keywords.txt",
      "english": "English source text...",
      "added": [{"hunk": "@@ ...", "lines": ["translated text"]}],
      "removed": [{"hunk": "@@ ...", "lines": ["old translated text"]}]
    }
  ]
}
```

- `locales.*.added` — strings newly introduced or whose value was changed (new value)
- `locales.*.removed` — strings that were deleted or whose value was changed (old value)
- `locales.*.english` — the corresponding value from `app/src/main/res/values/strings.xml`; empty string if the key is not found
- Only `res/values-*/strings.xml` files are included; the English `res/values/strings.xml` is excluded
- `files[].added` — added or changed translated guide lines grouped into contiguous diff blocks
- `files[].removed` — removed translated guide lines grouped into contiguous diff blocks
- `files[].english` — the complete matching English source guide file; empty string if the source file is not found