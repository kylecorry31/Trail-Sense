package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.math.toDoubleCompat
import com.kylecorry.andromeda.core.units.WeightUnits
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateItemBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryStringMapper
import com.kylecorry.trailsensecore.domain.packs.ItemCategory
import com.kylecorry.trailsensecore.domain.packs.PackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateItemFragment : BoundFragment<FragmentCreateItemBinding>() {

    private val itemRepo by lazy { PackRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }

    private var editingItem: PackItem? = null

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

        binding.itemWeightInput.units = formatService.sortWeightUnits(WeightUnits.values().toList())

        binding.createBtn.setOnClickListener {
            val name = binding.nameEdit.text?.toString()
            val amount = binding.countEdit.text?.toString()?.toDoubleCompat() ?: 0.0
            val desiredAmount = binding.desiredAmountEdit.text?.toString()?.toDoubleCompat() ?: 0.0
            val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]
            val weight = binding.itemWeightInput.value

            if (name != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        itemRepo.addItem(
                            PackItem(
                                editingItem?.id ?: 0,
                                packId,
                                name,
                                category,
                                amount,
                                desiredAmount,
                                weight
                            )
                        )
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
                    binding.itemWeightInput.value = it.weight
                    if (it.weight == null) {
                        binding.itemWeightInput.unit = binding.itemWeightInput.units.firstOrNull()
                    }
                }
            }

        }
    }

    private fun hasChanges(): Boolean {
        val name = binding.nameEdit.text?.toString()
        val amount = binding.countEdit.text?.toString()?.toDoubleCompat() ?: 0.0
        val desiredAmount = binding.desiredAmountEdit.text?.toString()?.toDoubleCompat() ?: 0.0
        val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]
        val weight = binding.itemWeightInput.value

        return !nothingEntered() && (name != editingItem?.name || amount != editingItem?.amount || desiredAmount != editingItem?.desiredAmount || category != editingItem?.category || weight != editingItem?.weight)
    }

    private fun nothingEntered(): Boolean {
        if (editingItem != null) {
            return false
        }

        val name = binding.nameEdit.text?.toString()
        val amount = binding.countEdit.text?.toString()
        val desiredAmount = binding.desiredAmountEdit.text?.toString()
        val category = ItemCategory.values()[binding.categorySelectSpinner.selectedItemPosition]
        val weight = binding.itemWeightInput.value

        return name.isNullOrBlank() && amount.isNullOrBlank() && desiredAmount.isNullOrBlank() && category == ItemCategory.Other && weight == null
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateItemBinding {
        return FragmentCreateItemBinding.inflate(layoutInflater, container, false)
    }

}