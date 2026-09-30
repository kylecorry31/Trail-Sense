package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useLiveData
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder

class FragmentToolNotes : TrailSenseReactiveFragment(R.layout.fragment_tool_notes) {

    override fun update() {
        // Views
        val listView = useView<AndromedaListView>(R.id.note_list)
        val emptyTextView = useView<TextView>(R.id.notes_empty_text)
        val addButtonView = useView<FloatingActionButton>(R.id.add_btn)

        // Services
        val context = useAndroidContext()
        val navController = useNavController()
        val notesRepo = useMemo(context) { NoteRepo.getInstance(context) }

        // State
        val notesLiveData = useMemo(notesRepo) { notesRepo.getNotes() }
        val notes = useLiveData(notesLiveData, emptyList()) { items ->
            items.sortedByDescending { it.createdOn }
        }

        val listMapper = useMemo(context, navController, notesRepo) {
            NoteListItemMapper(context) { note, action ->
                when (action) {
                    NoteAction.Edit -> navController.navigate(
                        R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate,
                        Bundle().apply { putLong("edit_note_id", note.id) }
                    )

                    NoteAction.Delete -> deleteNote(note, notesRepo)
                    NoteAction.QR -> showQR(note)
                }
            }
        }

        // Effects
        useEffect(listView, emptyTextView) {
            listView.emptyView = emptyTextView
        }

        useEffect(listView, notes, listMapper, resetOnResume) {
            listView.setItems(notes, listMapper)
        }

        useEffect(addButtonView, navController) {
            addButtonView.setOnClickListener {
                navController.navigate(R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate)
            }
        }
    }

    private fun showQR(note: Note) {
        CustomUiUtils.showQR(
            this,
            note.title ?: getString(android.R.string.untitled),
            NoteQREncoder().encode(note)
        )
    }

    private fun deleteNote(note: Note, notesRepo: NoteRepo) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_note_title),
            if (note.title?.trim().isNullOrEmpty()) {
                getString(android.R.string.untitled)
            } else {
                note.title
            }
        ) { cancelled ->
            if (!cancelled) {
                inBackground {
                    onIO {
                        notesRepo.deleteNote(note)
                    }
                }
            }
        }
    }
}
