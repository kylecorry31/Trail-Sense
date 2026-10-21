---
name: trail-sense-user-guide
description: Write or edit Trail Sense tool user guides (guides/en-US/guide_tool_*.txt) in the established house style - task-based headings, numbered procedures, warning callouts, and consistent UI wording. Use when adding a user guide, documenting a new or changed feature, or reviewing guide wording.
---

# Trail Sense User Guide

Keep every tool guide in `guides/en-US/guide_tool_*.txt` in the same style, based on the Google developer style guide and Microsoft Writing Style Guide procedure rules. The guides contain no images or videos, except the existing YouTube video-guide links.

## Scope

- Edit only `guides/en-US/guide_tool_*.txt`. All other guides (other locales, and the site and app copies) are updated only by `scripts/weekly-update.sh`, which the user runs manually. Never edit them or the scripts.
- A new tool guide is `guides/en-US/guide_tool_<tool>.txt`. Reference it from the tool's registration as `guideId = R.raw.guide_tool_<tool>`.
- Document only behavior you have verified. Check labels, defaults and settings paths against `app/src/main/res/values/strings.xml` and the tool's code. Never invent UI placement or behavior to make a sentence read better. Ask if it can't be verified.

## Rendering rules

The app splits a guide into sections in `TextUtils.getSections`.

- Any line that starts with `#` is a heading. No other line may start with `#`.
- `##` headings become collapsible sections, and `###` and `####` headings nest inside them. Don't skip levels.
- Text before the first heading is the intro. It renders as plain text.
- Keep URLs exactly as they are. The translation checks (`scripts/update_guides.py`) compare URLs, line counts and markdown element counts against the en-US source.

## Structure

1. **Purpose line.** The first line is one sentence: "Use the X tool to…". It says what the tool is for and doesn't repeat the title.
2. **Warnings and notes.** Anything that affects safety or accuracy comes right after the purpose line, before the content it applies to, as `**Warning:**` or `**Note:**`. Don't put a disclaimer at the end. Use `**Warning:**` for risk of injury, data loss or unrecoverable actions, and `**Note:**` for everything else. Put a warning directly before the destructive step, such as "Deleting a group also deletes all of the beacons in it."
3. **Sections in order:** tasks (what the reader does), then reference (what the screen shows), then settings, then `## Quick action`, then `## Widgets`, then `## Video guide`. Omit any that don't apply.
4. **One purpose per `##` section.** Split a section that does two jobs, such as "Rename a path" and "Delete a path".
5. **No troubleshooting section** unless the facts are known and verified.

## Headings

- Sentence case, no trailing punctuation, 60 characters or fewer, no duplicates in a guide.
- **Task headings** start with an imperative verb and an object: "Create a beacon", "Set a sunset alert", "Show or hide beacons on maps". Don't use "How to…" or gerunds ("Creating…").
- **Reference headings** are noun phrases: "Beacon settings", "Item fields", "Estimation methods".
- Use the same heading for the same task in every guide: "Delete a X", "Rename a X", "Export X", "Organize X", "Search and sort X", "Show or hide X on maps", "Create a group", "Move a X to another group".

## Procedures

Use a lead-in sentence that ends in a colon, then a numbered list.

```
To create a group, do the following:

1. Tap the **+** button in the bottom-right corner of the beacon list.
2. Select **Group**.
3. Enter a name for the group.
4. Tap **OK**.
```

- Lead-ins are "To <goal>, do the following:" for steps and "To <goal>, do one of the following:" for alternative ways.
- Each step is one action that starts with an imperative verb. Keep steps to two sentences or fewer. Put the location before the action: "In the list of cloud types, use the checkboxes…".
- A step may start with a condition ("If the **Coordinates** tab isn't open, swipe to it.") or a time ("When you finish, tap…").
- Mark optional steps with `Optional:` at the start: "4. Optional: Tap the time at the top of the screen to change the observation time."
- Put the result after the list, not as a step: "When the timer expires, the water is likely safe to drink."
- Don't nest lists inside steps. Put the options, examples or explanation after the list under its own lead-in ("The import options are:", "For example, enter the scale as follows:").
- Alternative ways to do something go in a bullet list with a lead-in, not in numbered steps.
- Refer to earlier steps by number ("Repeat steps 2 and 3…"). Renumber when you add or remove a step.
- Give a task that has a different path its own `##` or `###` heading, such as "Calibrate from the map grid".

## Lists

- Introduce every list with a sentence that ends in a colon. A heading is not a lead-in. Add a line such as "An item has the following fields:".
- Use `- ` bullets for options, fields, settings and widgets. Format a reference item as `- **Label**: Description.` and end each item with a period. Bullets in a list are all phrases or all sentences.
- Reference lists such as theme options may nest one level of definitions. Avoid nesting elsewhere.
- Use a numbered list only for ordered steps or an ordered cycle, never for options.

## Wording

- Second person ("you"), active voice, present tense. No "please", "simply", "just", "easily", "very", "etc.", "e.g.", "i.e.", "via", "in order to", "will", or "should" when you mean "must".
- Use "must" for a requirement and "can" for a choice. Say "Make sure…" when you mean it.
- Write "home screen", not "homescreen", except for the app's own label "Add to homescreen". Write "Internet", "GPS", "GPX", "QR code", "DEM".
- Prefer "tap" for touch. Use "long-press" to open a secondary action, and "tap and hold" for holding down continuously, as in the whistle and clinometer.
- Use these controls and phrases the same way everywhere:

| Thing | Write |
|---|---|
| Button, icon or label you tap | `Tap **Label**.`, `Tap the camera icon in the top-right corner.` |
| Toolbar button position | `in the top-right corner` (not "at the top-right") |
| Position on a list row | `on the right side of the item's row`, `on the beacon's row` |
| Row actions | `Tap the menu button on the beacon's row, and select **Delete**.` Menu items are "select", never "tap" |
| Menu button inside an open item | `Open the beacon, tap the menu button in the top-right corner, and select **Delete**.` |
| Add button | `Tap the **+** button in the bottom-right corner.` |
| Confirm | `Tap **OK**.` |
| Switch | `Turn on the **X** switch.` |
| Setting | `Enable **Settings > Weather > Storm alerts**.` or `Disable …`. A setting that isn't a switch is "Set **Settings > X > Y**." |
| Turn something off | `To turn this off, disable **Settings > X > Y**.` |
| Background feature state | "Backtrack is on", "the weather monitor is on" (not "enabled" or "running") |
| Permission | `the Location permission`, `the Camera permission` (capitalized, not bold) |
| Menu path or setting | Bold, with ` > `: `**Settings > Pedometer > Step history**` |
| UI labels and values | Bold, never in quotes |
| Mode or value you enter | Bold the label, not the value: `enter 1 for **From**` |
| Cross-reference in the same guide | `See the Quick action section.` |
| Cross-reference to another guide | `For more information, see the Paths guide.` or `see the Thermometer settings section of the Settings guide.` |
| Link to a video | `For a video guide to photo maps, see this video by a Trail Sense user: [YouTube: …](url)` |
| Screen areas | `at the top of the screen`, `at the bottom of the screen` for areas, and corners for buttons |

## Reusable sections

Use these exact forms.

**Alarm audio channel** (any alert tool):

```
To play the alert on the alarm audio channel, which sounds even if media and notifications are muted, enable **Settings > Turn Back > Use alarm for turn back alert**.
```

**Quick action** (one action):

```
## Quick action
To use the Weather Monitor quick action, enable it in the settings for the tab you want it on. Tap the quick action button to turn the weather monitor on or off.
```

With several quick actions or gestures, change the lead-in to "To use the following quick actions, enable them in the settings for the tab you want them on:" or "…it on. The quick action has the following gestures:" and list `- **Tap**: …` and `- **Long-press**: …`.

**Widgets**:

```
## Widgets
You can place the following widgets on your device's home screen or view them in the app:

- **Pressure**: Shows the current pressure and tendency arrow.
```

Start each widget description with a verb such as "Shows" or "Turns".

**Group, search and sort** (lists of beacons, paths, maps): copy the "Organize", "Search and sort" and "Show or hide" sections from `guide_tool_beacons.txt` or `guide_tool_paths.txt` and change the noun. Keep the "Deleting a group also deletes all of the X in it." warning.

## Workflow

1. Read `guide_tool_clouds.txt` (a short task guide) and `guide_tool_beacons.txt` (a list-management guide) to see the style. Read the guide you are changing first.
2. Verify each fact in the code and in `app/src/main/res/values/strings.xml` before writing it.
3. Write or edit using the rules above. Match the neighboring text, and change only what the request needs. Keep the original wording of unrelated lines.
4. If a new heading, setting, or procedure repeats something another guide already documents, copy that guide's wording.
5. Run the checks below. This step is complete when none of them print anything.

   ```
   cd guides/en-US
   grep -n "^#" guide_tool_<tool>.txt | grep -v "^[0-9]*:#\{2,4\} "
   grep -n " $" guide_tool_<tool>.txt
   grep -n -i "please\|simply\|\be\.g\.\|homescreen\|\bclick\b\|menu icon\|\(backtrack\|monitor\|pedometer\) is \(enabled\|running\)" guide_tool_<tool>.txt
   tail -c1 guide_tool_<tool>.txt | xxd -p | grep -v 0a
   ```

   Then confirm by reading that every list has a lead-in with a colon, every numbered list is sequential, no step has nested bullets, and every "see the X section" matches a heading.
6. Report which sections changed. Don't edit the translations, the site copies or the app copies.
