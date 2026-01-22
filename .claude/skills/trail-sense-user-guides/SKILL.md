---
name: trail-sense-user-guides
description: Write or update user guide documentation for Trail-Sense tools. Use when asked to create, write, update, or modify a tool's user guide, help documentation, or user-facing documentation. Covers guide structure, formatting conventions, and common sections.
---

# Trail Sense User Guides

Write user guide documentation for Trail Sense tools.

## File Location

`guides/en-US/guide_tool_<toolname>.txt`

The `<toolname>` should match the tool's directory name in `app/src/main/java/com/kylecorry/trail_sense/tools/`.

## Structure

```
The [Tool Name] tool can be used to [brief description of purpose].

## Main Feature
Explanation of the main feature and how to use it.

## Creating a [item]
1. Tap the '+' button in the bottom-right corner of the screen.
2. Click '[option]'.
3. Enter [details].
4. Click the checkmark button in the top-right corner of the screen.

### Optional fields
- **Field name**: Description of the field.
- **Another field**: Description.

## Viewing a [item]
To view a [item], click it in the list. The following information will be shown:
- **Name**: The name of the item.
- **Detail**: Description.

## Delete a [item]
To delete a [item], click the menu button on the row you want to remove, then select 'Delete'.

## Quick action
You can enable the [Tool] quick action in the settings for the desired tab.

To use the quick action, tap the [Tool] quick action button to [action].

## Widgets
The following widgets can be placed on your device's homescreen or viewed in-app:

- **Widget name**: Description of what it shows.
```

## Formatting Rules

### Headers
- `##` for main sections
- `###` for subsections

### Text Style
- **Bold** for UI element names, field names, button labels
- Second person: "You can...", "To create..."
- Present tense throughout

### Lists
- Numbered lists for step-by-step instructions
- Bullet points for options, fields, or non-sequential items

### Settings References
Format: `Settings > Category > Setting name`

Example: `Settings > Weather > Storm alert`

### Cross-References
Reference other guides by name in single quotes.

Example: "See the 'Navigation' guide for more information."

## Common Sections

Include sections as applicable to the tool:

| Section | When to Include |
|---------|-----------------|
| Main feature | Always - explain core functionality |
| Creating items | Tools that create/save data |
| Viewing items | Tools with detail views |
| Editing items | Tools with editable data |
| Deleting items | Tools with deletable data |
| Exporting | Tools that export data |
| Organizing (groups) | Tools with group/folder support |
| Searching | Tools with search functionality |
| Quick action | Tools with quick actions |
| Widgets | Tools with home screen widgets |
| Accuracy/Disclaimer | Tools making estimates or predictions |

## Writing Style

- **Be concise**: Focus on user actions, not implementation details
- **Be direct**: Start instructions with action verbs
- **Be consistent**: Use same terminology as the app UI
- **No jargon**: Write for users unfamiliar with the app

## Examples

### Step-by-step instruction
```
## Creating a beacon
1. Tap the '+' button in the bottom-right corner of the screen.
2. Click 'Beacon'.
3. Enter a name for the beacon.
4. Enter a location for the beacon. You can tap the GPS icon next to the location field to use your current location.
5. Fill out any of the optional fields you want to record.
6. Click the checkmark button in the top-right corner of the screen.
```

### Settings reference
```
You can adjust prediction sensitivity in Settings > Weather > Forecast sensitivity. Higher sensitivity may detect more patterns but might yield more false predictions.
```

### Quick action section
```
## Quick action
You can enable the Weather Monitor quick action in the settings for the tab you want it on.

To use the quick action, tap the Weather Monitor quick action button to toggle it on or off.
```

### Disclaimer
```
## Accuracy
The weather prediction is a best guess using available sensor data and may not be accurate. If Trail Sense says it is going to be clear but you see what appears to be storm clouds rolling in, trust your instincts.
```

## Source Files

For existing guide examples, see:
- `guides/en-US/guide_tool_*.txt`
