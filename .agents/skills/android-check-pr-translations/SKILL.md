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
   python scripts/extract_pr_strings.py <pr-number>
   ```
   This step is complete when the output includes:
   - `locales`: changed translated `<string>` resources with the English source for each key.
   - `files`: changed localized guide `.txt` files, including guide pages, field guides, and `tool_keywords.txt`, with the matching `guides/en-US/...` source text.

2. For every `locales.*.added` entry, compare `value` (translation) against `english` (source) using the accuracy criteria below. This step is complete when every added string resource has been classified as accurate or inaccurate.

3. For every `files[]` entry, compare the added translated lines against the `english` source text. Use removed lines and hunk context to understand what changed, but report only inaccuracies present in the added translated content. This step is complete when every added localized guide line has been classified as accurate or inaccurate.

4. Report findings as JSON (see Output Format). The audit is complete only when the JSON accounts for every inaccurate added translation and contains no accurate translations.

## Accuracy Criteria

**Counts as inaccurate:**
- The meaning is changed or distorted
- Content is added that isn't in the source (extra phrases, opinions, embellishments)
- Content is omitted that IS in the source
- Tone is significantly altered (e.g., formal → casual or vice versa) in a way that changes user-facing meaning
- Placeholders/variables (e.g., `%s`, `%1$d`, `%2$s`) are missing, reordered incorrectly, or altered
- HTML tags or formatting present in source are removed or changed in a way that affects meaning
- The translation introduces region-specific idioms or cultural spin not implied by the source

**Does NOT count as inaccurate:**
- Natural grammatical restructuring required by the target language
- Pluralization differences due to language rules
- Articles/pronouns added because the target language requires them
- Word order changes due to target language syntax rules

## Extraction Script: `scripts/extract_pr_strings.py` (skill-local)

The script parses the output of `gh pr diff` and extracts:
- Added and removed `<string>` elements from `res/values-*/strings.xml` patch hunks.
- Added and removed lines from localized guide text files under `guides/<locale>/*.txt`, excluding `guides/en-US`.

**Usage:**
```
python scripts/extract_pr_strings.py <pr-number>
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

## Output Format

Output ONLY a JSON blob:

```json
{
  "pr_number": 1234,
  "inaccurate_translations": [
    {
      "key": "key_name or null",
      "file": "path/to/file or null",
      "locale": "values-xx",
      "reason": "Why it is inaccurate"
    }
  ]
}
```

If all translations are accurate:
```json
{
  "pr_number": 1234,
  "inaccurate_translations": []
}
```

Do not output any explanation, commentary, or text outside the JSON blob.

- `key`: use the string resource key for `strings.xml` findings; use `null` for guide file findings.
- `file`: use `null` for `strings.xml` findings; use the localized file path for guide file findings.
- `locale`: use the Android resource directory (`values-xx`) or guide locale (`xx`, `xx-rYY`).
