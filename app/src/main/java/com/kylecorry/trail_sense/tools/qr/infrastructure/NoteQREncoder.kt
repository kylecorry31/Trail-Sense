package com.kylecorry.trail_sense.tools.qr.infrastructure

import com.kylecorry.trail_sense.tools.notes.domain.Note
import java.time.Instant

class NoteQREncoder : IQREncoder<Note> {

    override fun encode(value: Note): String {
        val title = if (value.title != null) {
            "${value.title}\n\n\n"
        } else {
            ""
        }

        val contents = value.contents ?: ""

        return "$title$contents"
    }

    override fun decode(qr: String): Note {
        val split = qr.split("\n\n\n", limit = 2)
        return if (split.size > 1 && !split[0].contains("\n")) {
            val title = split.first()
            val body = split.takeLast(split.size - 1).joinToString("\n\n\n")
            Note(title, body, Instant.now().toEpochMilli())
        } else {
            Note(null, qr, Instant.now().toEpochMilli())
        }
    }
}