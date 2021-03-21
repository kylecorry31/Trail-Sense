package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolNotesCreateBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentToolNotesCreate : Fragment() {

    private var _binding: FragmentToolNotesCreateBinding? = null
    private val binding get() = _binding!!
    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }

    private var editingNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val noteId = arguments?.getLong("edit_note_id") ?: 0L
        if (noteId != 0L) {
            loadEditingNote(noteId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolNotesCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        return title != editingNote?.title || content != editingNote?.contents
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

}