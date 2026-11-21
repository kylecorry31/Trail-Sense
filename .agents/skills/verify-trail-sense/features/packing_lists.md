# Packing Lists

Use the Packing Lists tool to plan a trip and make sure you pack everything you need. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_packing_lists.txt`](../../../../guides/en-US/guide_tool_packing_lists.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Packing Lists` → tap the tile.
- Precondition shortcut only: `$T open packing-lists`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Create list.** Create a list per Create a packing list. Run `$T ui tap id=add_btn`, then `$T ui tap 'text=New packing list'` (the speed dial also offers Import packing list) and enter a name. `id=create_btn` exists only on the item form, where it saves the item.
- **Add item.** Add an item with a weight and mark it packed. Run follow Add an item. Item shows packed amount and pack weight.
- **Side effect.** Stored data. Run `$T db query "select * from packs"`, `$T db query "select * from items"`. Rows for the list and its item.
- **Proof.** Snap the list detail with the pack weight; save the db output.

## Gotchas

- Weight units follow `@pref_weight_units` (seed sets lbs).
