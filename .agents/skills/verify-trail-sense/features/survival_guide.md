# Survival Guide

Use the Survival Guide tool to look up information about wilderness survival. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_survival_guide.txt`](../../../../guides/en-US/guide_tool_survival_guide.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Survival Guide` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open survival-guide`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Read.** Open the tool and tap a chapter (content from guides/en-US/guide_survival_chapter_*.txt). Run `$T open survival-guide`, dismiss the `Survival Guide` disclaimer (`id=button1`), then tap the chapter row. Use `text=/^Water$/` because plain `Water` also matches search results.
- **Search.** Search the guide. Run `$T ui type water id=search_view_edit_text`, wait ~1 s. Results such as `Hydration essentials > Dehydration` are listed.
- **Proof.** Snap the chapter and the search results.

## Gotchas

- Chapter content is long; use `$T ui scroll scrollable=true --until text=...` to reach a section.
