package com.kylecorry.trail_sense.tools.inventory.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryStringMapper
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateItemBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItemDto
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory
import com.kylecorry.trail_sense.tools.inventory.infrastructure.ItemRepo
import com.kylecorry.trailsensecore.domain.math.toDoubleCompat
import com.kylecorry.trailsensecore.infrastructure.text.DecimalFormatter
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlinx.coroutines.*

class CreateItemFragment : BoundFragment<FragmentCreateItemBinding>() {

    private val itemRepo by lazy { ItemRepo.getInstance(requireContext()) }

    private var editingItem: InventoryItemDto? = null

    private var packId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = arguments?.getLong("edit_item_id") ?: 0L
        packId = arguments?.getLong("pack_id") ?: 0L
        if (itemId != 0L) {
            loadEditingItem(itemId)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createBtn.setOnClickListener {
            val name = binding.nameEdit.text?.toString()
            val amount = binding.countEdit.text?.toString()?.toDoubleCompat() ?: 0.0
            val desiredAmount = binding.desiredAmountEdit.text?.toString()?.toDoubleCompat() ?: 0.0
            val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]

            if (name != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        itemRepo.addItem(
                            InventoryItemDto(
                                name,
                                packId,
                                category,
                                amount,
                                desiredAmount
                            ).apply {
                                editingItem?.let {
                                    id = it.id
                                }
                            })
                    }

                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
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

        CustomUiUtils.promptIfUnsavedChanges(requireActivity(), this, this::hasChanges)
    }

    private fun loadEditingItem(id: Long) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                editingItem = itemRepo.getItem(id)
            }

            withContext(Dispatchers.Main) {
                editingItem?.let {
                    binding.createItemTitle.text = getString(R.string.edit_item_title)
                    binding.nameEdit.setText(it.name)
                    binding.countEdit.setText(DecimalFormatter.format(it.amount, 4, false))
                    binding.desiredAmountEdit.setText(
                        DecimalFormatter.format(
                            it.desiredAmount,
                            4,
                            false
                        )
                    )
                    binding.categorySelectSpinner.setSelection(it.category.ordinal)
                }
            }

        }
    }

    private fun hasChanges(): Boolean {
        val name = binding.nameEdit.text?.toString()
        val amount = binding.countEdit.text?.toString()?.toDoubleCompat() ?: 0.0
        val desiredAmount = binding.desiredAmountEdit.text?.toString()?.toDoubleCompat() ?: 0.0
        val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]

        return !nothingEntered() && (name != editingItem?.name || amount != editingItem?.amount || desiredAmount != editingItem?.desiredAmount || category != editingItem?.category)
    }

    private fun nothingEntered(): Boolean {
        if (editingItem != null) {
            return false
        }

        val name = binding.nameEdit.text?.toString()
        val amount = binding.countEdit.text?.toString()
        val desiredAmount = binding.desiredAmountEdit.text?.toString()
        val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]

        return name.isNullOrBlank() && amount.isNullOrBlank() && desiredAmount.isNullOrBlank() && category == ItemCategory.Other
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateItemBinding {
        return FragmentCreateItemBinding.inflate(layoutInflater, container, false)
    }

}