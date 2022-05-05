package com.kylecorry.trail_sense.tools.notes.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.lists.ListItem
import com.kylecorry.trail_sense.shared.lists.ListItemMapper
import com.kylecorry.trail_sense.shared.lists.ListMenuItem
import com.kylecorry.trail_sense.tools.notes.domain.Note


enum class NoteAction {
    Edit,
    Delete,
    QR
}

class NoteListItemMapper(
    private val context: Context,
    private val actionHandler: (Note, NoteAction) -> Unit
) : ListItemMapper<Note> {
    override fun map(value: Note): ListItem {
        val title = if (value.title?.trim().isNullOrEmpty()) {
            context.getString(android.R.string.untitled)
        } else {
            value.title
        }!!

        val contents = value.contents ?: ""

        val menu = listOf(
            ListMenuItem(context.getString(R.string.qr_code)) {
                actionHandler(
                    value,
                    NoteAction.QR
                )
            },
            ListMenuItem(context.getString(R.string.delete)) {
                actionHandler(
                    value,
                    NoteAction.Delete
                )
            }
        )

        return ListItem(
            value.id,
            title,
            subtitle = contents,
            singleLineSubtitle = true,
            menu = menu
        ) {
            actionHandler(value, NoteAction.Edit)
        }
    }
}