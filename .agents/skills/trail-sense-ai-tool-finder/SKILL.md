---
name: trail-sense-ai-tool-finder
description: Answer Trail Sense user questions about which in-app tool handles a need, where to find it, how to use it, and what displayed values mean. Use when asked to route a user need to Trail Sense tools, explain tool usage, interpret measurements, or prepare AI assistant knowledge for the app.
---

# Trail Sense AI Tool Finder

Use this skill to answer user-facing questions such as:

- "Where in Trail Sense can I measure slope?"
- "How do I find a saved location later?"
- "What does pressure tendency mean?"
- "Which tool should I use to estimate tide/current weather/declination?"

## Required Source

Read `references/tool-index.md` first. It is the canonical AI assistant knowledge index for registered Trail Sense tools.

If the answer needs more detail than the index contains, open the matching user guide:

- In-app guide source: `app/src/main/res/raw/guide_tool_<tool>.md`
- Website guide source: `site/src/user-guide/guide_tool_<tool>.md`
- Tool registration source: `app/src/main/java/com/kylecorry/trail_sense/tools/**/<Tool>ToolRegistration.kt`
- Settings registration source: `app/src/main/java/com/kylecorry/trail_sense/settings/SettingsToolRegistration.kt`

## Answer Contract

When responding to an end user, include:

1. The recommended tool name.
2. Where to find it in the app: usually `Tools` tab/list, search, pinned tool, quick action, widget, or a settings path.
3. How to use the feature in a few direct steps.
4. What the important values or results mean.
5. Sensor, availability, and accuracy caveats when relevant.

Prefer a single best tool. Mention related tools only when they materially help the user's task.

## Important Rules

- Do not invent readings. If the app would need live sensor data, say the user must open the tool or enable the monitor.
- If a tool depends on hardware, mention that it may be unavailable without the sensor.
- If a tool is experimental, debug-only, plugin-only, or has no user guide, say so plainly.
- For safety-sensitive areas such as weather, navigation, water safety, ballistics, tide, or emergency signaling, remind the user to verify with real-world conditions and backup tools.
- Use the same UI names as the app and user guides.
