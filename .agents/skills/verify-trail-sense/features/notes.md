# Notes

Use the Notes tool to write down notes that you can access offline. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_notes.txt`](../../../../guides/en-US/guide_tool_notes.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Notes` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open notes`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Create.** Tap create, type a title and content, save. Run `$T ui tap id=create_btn`, `$T ui find Title`, `$T ui type "Verify note" id=<reported id>`, `$T ui tap id=toolbar_right_button` (verify handles with `$T ui find`). The note title appears in the list.
- **Side effect.** Read the stored note. Run `$T db query "select _id, title, contents from notes"`. A row with the title.
- **Proof.** Snap the list; save the db output. Precondition seeding via `$T db exec "insert into notes(title, contents, created) values ('Seeded note','x',0)"` was proven to show in the list after relaunch.

## Gotchas

- QR sharing renders a QR code; scanning it back needs a second camera.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
