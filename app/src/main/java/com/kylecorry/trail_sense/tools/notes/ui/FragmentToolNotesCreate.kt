package com.kylecorry.trail_sense.tools.notes.ui

import androidx.core.widget.addTextChangedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useUnsavedChangesPrompt
import com.kylecorry.trail_sense.shared.views.Notepad
import com.kylecorry.trail_sense.shared.views.TextInputView
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import java.time.Instant

class FragmentToolNotesCreate : TrailSenseReactiveFragment(R.layout.fragment_tool_notes_create) {

    override fun update() {
        // Views
        val titleView = useView<TextInputView>(R.id.title_edit)
        val contentView = useView<Notepad>(R.id.content_edit)
        val createButtonView = useView<FloatingActionButton>(R.id.note_create_btn)

        // Services
        val context = useAndroidContext()
        val navController = useNavController()
        val notesRepo = useMemo(context) { NoteRepo.getInstance(context) }

        // Arguments
        val noteId = useArgument<Long>("edit_note_id") ?: 0L

        // State
        val (title, setTitle) = useState(titleView.text.toString())
        val (content, setContent) = useState(contentView.text?.toString() ?: "")
        val editingNote = useBackgroundMemo(notesRepo, noteId) {
            if (noteId != 0L) notesRepo.getNote(noteId) else null
        }

        val hasChanges = useMemo(editingNote, title, content) {
            val nothingEntered = editingNote == null && title.isBlank() && content.isBlank()
            !nothingEntered && (title != editingNote?.title || content != editingNote.contents)
        }

        // Effects
        useEffect(titleView) {
            setTitle(titleView.text.toString())
            titleView.setOnTextChangeListener {
                setTitle(it?.toString() ?: "")
            }
        }

        useEffect(contentView) {
            setContent(contentView.text?.toString() ?: "")
            contentView.addTextChangedListener {
                setContent(it?.toString() ?: "")
            }
        }

        useEffect(titleView, contentView, editingNote) {
            editingNote?.let {
                titleView.text = it.title ?: ""
                contentView.setText(it.contents ?: "")
            }
        }

        useEffect(createButtonView, navController, notesRepo, editingNote, title, content) {
            createButtonView.setOnClickListener {
                val note = editingNote?.copy(title = title, contents = content)
                    ?.apply { id = editingNote.id }
                    ?: Note(title, content, Instant.now().toEpochMilli())
                inBackground {
                    onIO {
                        notesRepo.addNote(note)
                    }

                    onMain {
                        navController.navigateUp()
                    }
                }
            }
        }

        useUnsavedChangesPrompt(hasChanges)
    }
}
