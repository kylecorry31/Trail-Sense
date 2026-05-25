# AI Assistant Tool Knowledge

This is the app-facing companion to `.agents/skills/trail-sense-ai-tool-finder`.

Use the skill reference at `.agents/skills/trail-sense-ai-tool-finder/references/tool-index.md` as the canonical source for mapping user needs to Trail Sense tools, usage steps, and value explanations.

Recommended integration pattern for an in-app AI assistant:

1. Retrieve the most relevant tool entries from the tool index using keyword search.
2. Prefer a single best tool and include related tools only when helpful.
3. Answer with: recommended tool, where to find it, how to use it, what values mean, and any caveat.
4. If live values are needed, use app context/tool providers rather than guessing.
5. For safety-sensitive tools, include the existing guide caveat.

Primary sources:

- `app/src/main/java/com/kylecorry/trail_sense/tools/tools/infrastructure/Tools.kt`
- `app/src/main/java/com/kylecorry/trail_sense/tools/**/<Tool>ToolRegistration.kt`
- `app/src/main/java/com/kylecorry/trail_sense/settings/SettingsToolRegistration.kt`
- `app/src/main/res/raw/guide_tool_*.md`
