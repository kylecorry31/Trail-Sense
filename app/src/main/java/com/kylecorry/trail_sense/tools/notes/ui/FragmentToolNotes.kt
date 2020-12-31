package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolNotesBinding
import com.kylecorry.trail_sense.databinding.ListItemNoteBinding
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentToolNotes : Fragment() {

    private var _binding: FragmentToolNotesBinding? = null
    private val binding get() = _binding!!
    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }
    private lateinit var notesLiveData: LiveData<List<Note>>

    private lateinit var listView: ListView<Note>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.noteList, R.layout.list_item_note) { noteView, note ->
            val noteBinding = ListItemNoteBinding.bind(noteView)
            noteBinding.title.text = note.title ?: getString(R.string.untitled_note)
            noteBinding.contents.text = note.contents

            val menuListener = PopupMenu.OnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_note_delete -> {
                        deleteNote(note)
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
            // TODO: Navigate to create note fragment
//            findNavController().navigate(R.id.action_action_inventory_to_createItemFragment)
        }
    }

    private fun deleteNote(note: Note) {
        UiUtils.alertWithCancel(
            requireContext(),
            getString(R.string.delete_note_title),
            note.title ?: getString(R.string.untitled_note),
            getString(R.string.dialog_ok),
            getString(R.string.dialog_cancel)
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
        // TODO: Navigate to create note fragment
//        findNavController().navigate(R.id.action_action_inventory_to_createItemFragment, bundle)
    }
}