package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolNotesCreateBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.andromeda.fragments.BoundFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentToolNotesCreate : BoundFragment<FragmentToolNotesCreateBinding>() {

    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }

    private var editingNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val noteId = arguments?.getLong("edit_note_id") ?: 0L
        if (noteId != 0L) {
            loadEditingNote(noteId)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.noteCreateBtn.setOnClickListener {
            val existingNote = editingNote
            val title = binding.titleEdit.text.toString()
            val content = binding.contentEdit.text.toString()

            val note = existingNote?.copy(title = title, contents = content)
                ?.apply { id = existingNote.id }
                ?: Note(title, content, Instant.now().toEpochMilli())
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    notesRepo.addNote(note)
                }

                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_fragmentToolNotesCreate_to_fragmentToolNotes)
                }
            }
        }

        CustomUiUtils.promptIfUnsavedChanges(requireActivity(), this, this::hasChanges)
    }

    private fun hasChanges(): Boolean {
        val title = binding.titleEdit.text.toString()
        val content = binding.contentEdit.text.toString()
        return !nothingEntered() && (title != editingNote?.title || content != editingNote?.contents)
    }


    private fun nothingEntered(): Boolean {
        if (editingNote != null) {
            return false
        }

        val title = binding.titleEdit.text.toString()
        val content = binding.contentEdit.text.toString()

        return title.isBlank() && content.isBlank()
    }


    private fun loadEditingNote(id: Long) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                editingNote = notesRepo.getNote(id)
            }

            withContext(Dispatchers.Main) {
                editingNote?.let {
                    binding.titleEdit.setText(it.title ?: "")
                    binding.contentEdit.setText(it.contents ?: "")
                }
            }

        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolNotesCreateBinding {
        return FragmentToolNotesCreateBinding.inflate(layoutInflater, container, false)
    }

}