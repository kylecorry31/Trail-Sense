package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolNotesBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FragmentToolNotes : BoundFragment<FragmentToolNotesBinding>() {

    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }
    private lateinit var notesLiveData: LiveData<List<Note>>
    private val listMapper by lazy { NoteListItemMapper(requireContext(), this::handleAction) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.noteList.emptyView = binding.notesEmptyText
        notesLiveData = notesRepo.getNotes()
        observe(notesLiveData) { items ->
            binding.noteList.setItems(items.sortedByDescending { it.createdOn }, listMapper)
        }

        binding.addBtn.setOnClickListener {
            findNavController().navigate(R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate)
        }
    }

    private fun handleAction(note: Note, action: NoteAction) {
        when (action) {
            NoteAction.Edit -> editNote(note)
            NoteAction.Delete -> deleteNote(note)
            NoteAction.QR -> showQR(note)
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
                inBackground {
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