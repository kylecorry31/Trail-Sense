package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolNotesBinding
import com.kylecorry.trail_sense.databinding.ListItemNoteBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentToolNotes : BoundFragment<FragmentToolNotesBinding>() {

    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }
    private lateinit var notesLiveData: LiveData<List<Note>>

    private lateinit var listView: ListView<Note>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.noteList, R.layout.list_item_note) { noteView, note ->
            val noteBinding = ListItemNoteBinding.bind(noteView)
            noteBinding.title.text = if (note.title?.trim().isNullOrEmpty()) {
                getString(android.R.string.untitled)
            } else {
                note.title
            }
            noteBinding.contents.text = note.contents ?: ""

            val menuListener = PopupMenu.OnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_note_delete -> {
                        deleteNote(note)
                    }
                    R.id.action_note_qr -> {
                        showQR(note)
                    }
                }
                true
            }

            noteBinding.noteMenuBtn.setOnClickListener {
                val popup = PopupMenu(it.context, it)
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.note_menu, popup.menu)
                popup.setOnMenuItemClickListener(menuListener)
                popup.show()
            }

            noteView.setOnClickListener {
                editNote(note)
            }

        }

        listView.addLineSeparator()

        notesLiveData = notesRepo.getNotes()
        notesLiveData.observe(viewLifecycleOwner) { items ->
            binding.notesEmptyText.visibility = if (items.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            listView.setData(items.sortedByDescending { it.createdOn })
        }

        binding.addBtn.setOnClickListener {
            findNavController().navigate(R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate)
        }
    }

    private fun showQR(note: Note) {
        CustomUiUtils.showQR(
            this,
            note.title ?: getString(android.R.string.untitled),
            NoteQREncoder().encode(note)
        )
    }

    private fun deleteNote(note: Note) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_note_title),
            if (note.title?.trim().isNullOrEmpty()) {
                getString(android.R.string.untitled)
            } else {
                note.title!!
            }
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        notesRepo.deleteNote(note)
                    }
                }
            }
        }
    }

    private fun editNote(note: Note) {
        val bundle = bundleOf("edit_note_id" to note.id)
        findNavController().navigate(
            R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate,
            bundle
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolNotesBinding {
        return FragmentToolNotesBinding.inflate(layoutInflater, container, false)
    }
}