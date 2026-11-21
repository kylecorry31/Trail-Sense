# Tools screen

Trail Sense is a set of tools designed for hiking, backpacking, camping, and geocaching. Use the Tools screen to search and open tools. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_tools.txt`](../../../../guides/en-US/guide_tool_tools.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)


## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Open a tool.** Tap Tools, search, tap the tile. Run `$T ui tap id=action_experimental_tools`, `$T ui type Beacons id=search_view_edit_text`, `$T ui tap 'text=/^Beacons$/,id=title'`. The Beacons tool opens (proven).
- **Pin.** Long-press a tool and pin it per Pin tools. Run `$T ui find Beacons` for the tile center, then `$T ui hold <x> <y>` with its center. It shows under Pinned.
- **Bottom navigation.** Customize the bottom navigation per the guide. Run via Settings. Tabs change; `$T pref list bottom` shows the stored list.
- **User guide.** Open a tool's user guide from the tool options. Run long-press the tile. The guide text opens (User Guide tool, id 35).
- **Proof.** Snap the Tools screen with the search applied and the opened tool.

## Gotchas

- Search matches tool names and keywords from guides/en-US/tool_keywords.txt.
- `text=Beacons` alone also matches the search box text after typing; add `id=title`.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
