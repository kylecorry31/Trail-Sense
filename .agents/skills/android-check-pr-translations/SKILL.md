---
name: android-check-pr-translations
description: Audit translation accuracy for strings changed in a GitHub PR. Use when asked to check, review, audit, or verify translations in a pull request. Identifies inaccurate translated strings by comparing PR changes against the English source.
---

# Check PR Translations

Audit the translation accuracy of Android string resources changed in a GitHub pull request.

## Workflow

1. Run the extraction script from the skill's folder to get the changed strings from the PR:
   ```
   python scripts/extract_pr_strings.py <pr-number>
   ```
   The output includes both the translated value and the English source for each changed key — no repo search needed.

2. For each entry in `added`, compare `value` (translation) against `english` (source) using the accuracy criteria below.

3. Report findings as JSON (see Output Format).

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

The script parses the output of `gh pr diff` and extracts `<string>` elements from `res/values-*/strings.xml` patch hunks.

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
  }
}
```

- `added` — strings newly introduced or whose value was changed (new value)
- `removed` — strings that were deleted or whose value was changed (old value)
- `english` — the corresponding value from `app/src/main/res/values/strings.xml`; empty string if the key is not found
- Only `res/values-*/strings.xml` files are included; the English `res/values/strings.xml` is excluded

## Output Format

Output ONLY a JSON blob:

```json
{
  "pr_number": 1234,
  "inaccurate_translations": [
    {
      "key": "key_name",
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
