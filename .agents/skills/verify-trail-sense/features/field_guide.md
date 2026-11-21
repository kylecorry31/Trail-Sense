# Field Guide

Use the Field Guide tool to identify plants, animals, and other objects in the wild, and to record where you see them. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_field_guide.txt`](../../../../guides/en-US/guide_tool_field_guide.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Field Guide` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open field-guide`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Browse.** Open the tool and open a built-in page (content from guides/en-US/field_guide_*.txt). Run `$T open field-guide`, dismiss the `Disclaimer` dialog (`text=/^OK$/`), then tap a category row (`Plant`, `Fungus`, `Animal`) and tap the page (`Dandelion`). The top level lists only categories until a category is tapped or a search is typed.
- **Search.** Search for a page. Run `$T ui type morel id=search_view_edit_text`. Morel listed.
- **Create page.** Create a page per the guide. Run `$T ui tap id=add_btn` (toolbar `Create page`). `id=create_btn` is the sightings list button, not this one. The new page is in the list.
- **Side effect.** Read the stored pages and sightings. Run `$T db query "select * from field_guide_pages"`. A row for the new page; sightings go to `field_guide_sightings`.
- **Proof.** Snap the list and the page; save the db query output.

## Gotchas

- Built-in pages aren't rows in the database; only user pages and sightings are.
- Search handle `id=search_view_edit_text`, category rows (`Plant`, `Fungus`, `Animal`) and the `add_btn` to `Create page` screen were driven on 2026-11-21.
