package com.kylecorry.trail_sense.tools.inventory.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryStringMapper
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateItemBinding
import com.kylecorry.trail_sense.shared.DecimalFormatter
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory
import com.kylecorry.trail_sense.tools.inventory.infrastructure.ItemRepo
import kotlinx.coroutines.*

class CreateItemFragment : Fragment() {
    private var _binding: FragmentCreateItemBinding? = null
    private val binding get() = _binding!!

    private val itemRepo by lazy { ItemRepo.getInstance(requireContext()) }

    private var editingItem: InventoryItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = arguments?.getLong("edit_item_id") ?: 0L
        if (itemId != 0L) {
            loadEditingBeacon(itemId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createBtn.setOnClickListener {
            val name = binding.nameEdit.text?.toString()
            val amount = binding.countEdit.text?.toString()?.toDoubleOrNull() ?: 0.0
            val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]

            if (name != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        itemRepo.addItem(InventoryItem(name, category, amount).apply {
                            editingItem?.let {
                                id = it.id
                            }
                        })
                    }

                    withContext(Dispatchers.Main) {
                        findNavController().navigate(R.id.action_createItemFragment_to_action_inventory)
                    }
                }
            }
        }

        val nameMapper = ItemCategoryStringMapper(requireContext())

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_plain,
            R.id.item_name,
            ItemCategory.values().map { nameMapper.getString(it) })
        binding.categorySelectSpinner.prompt = getString(R.string.spinner_category_prompt)
        binding.categorySelectSpinner.adapter = adapter

        if (editingItem == null) {
            binding.categorySelectSpinner.setSelection(0)
        }
    }

    private fun loadEditingBeacon(id: Long) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                editingItem = itemRepo.getItem(id)
            }

            withContext(Dispatchers.Main) {
                editingItem?.let {
                    binding.createItemTitle.text = getString(R.string.edit_item_title)
                    binding.nameEdit.setText(it.name)
                    binding.countEdit.setText(DecimalFormatter.format(it.amount))
                    binding.categorySelectSpinner.setSelection(it.category.ordinal)
                }
            }

        }
    }

}