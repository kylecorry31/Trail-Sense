# Tides

Use the Tides tool to keep track of the tides at a specific location. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_tides.txt`](../../../../guides/en-US/guide_tool_tides.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Tides` → tap the tile.
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open tides`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Create table.** Create a tide table per Create a tide table. Dismiss the `Tides` disclaimer on first open (`id=button1`, OK; the other button is `User Guide`). The tide list is behind the toolbar button, so run `$T ui tap id=toolbar_right_button`, then `$T ui tap id=add_btn`. On the create screen, `id=tide_times` is the entry table; tapping a row's time opens a date picker and then a time picker, each confirmed with `text=/^OK$/`. Save with `$T ui tap id=toolbar_right_button` (it appears once the form is valid). The table appears in the list. Driven on 2026-11-21: a saved table gave one `tide_tables` row and one `tide_table_rows` row (the name is an `id=/:id\/name$/` field and may be left empty).
- **View.** Open the table. Run tap it. Tide chart with high/low times.
- **Side effect.** Stored tables. Run `$T db query "select * from tide_tables"`. A row; entered tides in `tide_table_rows`.
- **Proof.** Snap the list and the chart; save the db output.

## Gotchas

- Estimation without a table uses location; set `$T geo` near a coast.
- A saved table is cleaned up with `$T db exec "delete from tide_table_rows; delete from tide_tables"`.
