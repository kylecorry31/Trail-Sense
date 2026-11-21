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

- **Create table.** Create a tide table per Create a tide table. Run `$T ui tap id=create_btn` (verify with `$T ui find`). The table appears in the list.
- **View.** Open the table. Run tap it. Tide chart with high/low times.
- **Side effect.** Stored tables. Run `$T db query "select * from tide_tables"`. A row; entered tides in `tide_table_rows`.
- **Proof.** Snap the list and the chart; save the db output.

## Gotchas

- Estimation without a table uses location; set `$T geo` near a coast.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
