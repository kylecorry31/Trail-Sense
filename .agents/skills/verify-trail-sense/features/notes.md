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

- **Create.** Tap create, type a title and content, save. Run `$T ui tap id=add_btn`, `$T ui type "Verify note" 'id=/:id\/title_edit$/'` (a plain `id=title_edit` is ambiguous), `$T ui type body id=content_edit`, then `$T ui tap id=note_create_btn` (the save button is a bottom-right FAB, not `toolbar_right_button`). The list row shows the content (`id=description`); a note saved without a title has an empty title.
- **Side effect.** Read the stored note. Run `$T db query "select _id, title, contents from notes"`. A row with the title.
- **Proof.** Snap the list; save the db output. Precondition seeding via `$T db exec "insert into notes(title, contents, created) values ('Seeded note','x',0)"` was proven to show in the list after relaunch.

## Gotchas

- QR sharing renders a QR code; scanning it back needs a second camera.
- Delete: the row menu (`id=menu_btn`) offers `QR Code` and `Delete`; Delete asks for `text=/^OK$/`. Driven end to end on 2026-11-21.
