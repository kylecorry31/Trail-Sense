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

- **Read.** Open the tool and tap a chapter (content from guides/en-US/guide_survival_chapter_*.txt). Run `$T open survival-guide`, `$T ui tap text=Water`. The chapter opens.
- **Search.** Search the guide. Run `$T ui find Search`, type a term. Matching sections listed.
- **Proof.** Snap the chapter and the search results.

## Gotchas

- Chapter content is long; use `$T ui scroll scrollable=true --until text=...` to reach a section.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
