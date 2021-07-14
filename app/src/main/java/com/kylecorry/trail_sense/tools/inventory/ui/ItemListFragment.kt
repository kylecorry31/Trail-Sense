package com.kylecorry.trail_sense.tools.inventory.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryColorMapper
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentItemListBinding
import com.kylecorry.trail_sense.databinding.ListItemPackItemBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem
import com.kylecorry.trail_sense.tools.inventory.domain.PackItem
import com.kylecorry.trail_sense.tools.inventory.infrastructure.InventoryItemMapper
import com.kylecorry.trail_sense.tools.inventory.infrastructure.ItemRepo
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryIconMapper
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryStringMapper
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.system.tryOrNothing
import com.kylecorry.trailsensecore.infrastructure.text.DecimalFormatter
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Double.max

class ItemListFragment : BoundFragment<FragmentItemListBinding>() {

    private val itemRepo by lazy { ItemRepo.getInstance(requireContext()) }
    private lateinit var itemsLiveData: LiveData<List<InventoryItem>>
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private lateinit var listView: ListView<PackItem>

    private val itemMapper = InventoryItemMapper()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.inventoryList, R.layout.list_item_pack_item) { itemView, item ->
            val itemBinding = ListItemPackItemBinding.bind(itemView)
            itemBinding.name.text = item.name

            // TODO: Support desired amount
            val currentAmount = DecimalFormatter.format(item.amount, 4, false)
            itemBinding.count.text = if (item.desiredAmount != 0.0) {
                "$currentAmount / ${DecimalFormatter.format(item.desiredAmount, 4, false)}"
            } else {
                currentAmount
            }

            val categoryTextMapper = ItemCategoryStringMapper(requireContext())
            itemBinding.itemCategory.text = categoryTextMapper.getString(item.category)
            val imgMapper = ItemCategoryIconMapper()
            itemBinding.itemCategoryImg.setImageResource(imgMapper.getIcon(item.category))

            val colorMapper = ItemCategoryColorMapper()
            val categoryColor = colorMapper.map(item.category).color
            itemBinding.itemCategory.setTextColor(categoryColor)
            itemBinding.itemCategoryImg.colorFilter =
                PorterDuffColorFilter(categoryColor, PorterDuff.Mode.SRC_IN)

            if (item.weight != null) {
                itemBinding.weight.isVisible = true
                itemBinding.weightImg.isVisible = true
                itemBinding.weight.text =
                    formatService.formatWeight(item.weight.copy(weight = item.weight.weight * item.amount.toFloat()))
            } else {
                itemBinding.weight.isVisible = false
                itemBinding.weightImg.isVisible = false
            }

            itemBinding.itemCheckbox.isChecked = item.amount >= item.desiredAmount && item.amount != 0.0

            itemBinding.itemCheckbox.setOnCheckedChangeListener { _, isChecked ->
                itemBinding.itemCheckbox.setOnCheckedChangeListener(null)
                if (isChecked) {
                    if (item.desiredAmount == 0.0) {
                        addAmount(item, 1.0)
                    } else {
                        addAmount(item, max(0.0, item.desiredAmount - item.amount))
                    }
                } else {
                    addAmount(item, -item.amount)
                }
            }

            val menuListener = PopupMenu.OnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_item_add -> {
                        editTextDialog(
                            requireContext(),
                            getString(R.string.dialog_item_add),
                            getString(R.string.dialog_item_amount),
                            null,
                            null,
                            getString(R.string.dialog_item_add),
                            getString(R.string.dialog_cancel)
                        ) { canceled: Boolean, text: String? ->
                            if (!canceled) {
                                val value = text?.toDoubleOrNull()
                                if (value != null) {
                                    itemBinding.itemCheckbox.setOnCheckedChangeListener(null)
                                    addAmount(item, value)
                                }
                            }

                        }
                    }
                    R.id.action_item_subtract -> {
                        editTextDialog(
                            requireContext(),
                            getString(R.string.dialog_item_subtract),
                            getString(R.string.dialog_item_amount),
                            null,
                            null,
                            getString(R.string.dialog_item_subtract),
                            getString(R.string.dialog_cancel)
                        ) { canceled: Boolean, text: String? ->
                            if (!canceled) {
                                val value = text?.toDoubleOrNull()
                                if (value != null) {
                                    itemBinding.itemCheckbox.setOnCheckedChangeListener(null)
                                    addAmount(item, -value)
                                }
                            }

                        }
                    }
                    R.id.action_item_edit -> {
                        editItem(item)
                    }
                    R.id.action_item_delete -> {
                        deleteItem(item)
                    }
                }
                true
            }

            itemBinding.itemMenuBtn.setOnClickListener {
                val popup = PopupMenu(it.context, it)
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.inventory_item_menu, popup.menu)
                popup.setOnMenuItemClickListener(menuListener)
                popup.show()
            }

            itemView.setOnClickListener {
                tryOrNothing {
                    editItem(item)
                }
            }

        }

        listView.addLineSeparator()

        itemsLiveData = itemRepo.getItems()
        itemsLiveData.observe(viewLifecycleOwner) { items ->
            binding.inventoryEmptyText.visibility = if (items.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            listView.setData(
                items.sortedWith(
                    compareBy(
                        { it.category.id },
                        { it.name },
                        { it.amount })
                ).map { itemMapper.mapToPackItem(it) }
            )
        }

        binding.addBtn.setOnClickListener {
            findNavController().navigate(R.id.action_action_inventory_to_createItemFragment)
        }

        val inventoryMenuListener = PopupMenu.OnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_inventory_delete_all -> {
                    UiUtils.alertWithCancel(
                        requireContext(),
                        getString(R.string.action_inventory_delete_all),
                        getString(R.string.action_inventory_delete_all_confirm),
                        getString(R.string.dialog_ok),
                        getString(R.string.dialog_cancel)
                    ) { cancelled ->
                        if (!cancelled) {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    itemRepo.deleteAll()
                                }
                            }
                        }
                    }
                }
            }
            true
        }

        binding.inventoryMenuButton.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.inventory_menu, popup.menu)
            popup.setOnMenuItemClickListener(inventoryMenuListener)
            popup.show()
        }
    }

    private fun deleteItem(item: PackItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                itemRepo.deleteItem(itemMapper.mapToInventoryItem(item))
            }
        }
    }

    private fun editItem(item: PackItem) {
        val bundle = bundleOf("edit_item_id" to item.id)
        findNavController().navigate(R.id.action_action_inventory_to_createItemFragment, bundle)
    }

    private fun addAmount(item: PackItem, amount: Double) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    itemMapper.mapToInventoryItem(item)
                        .copy(amount = max(0.0, item.amount + amount)).apply { id = item.id })
            }
        }
    }

    private fun editTextDialog(
        context: Context,
        title: String,
        hint: String?,
        description: String?,
        initialInputText: String?,
        okButton: String,
        cancelButton: String,
        onClose: (cancelled: Boolean, text: String?) -> Unit
    ): AlertDialog {
        val layout = FrameLayout(context)
        val editTextView = EditText(context)
        editTextView.setText(initialInputText)
        editTextView.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
        editTextView.hint = hint
        layout.setPadding(64, 0, 64, 0)
        layout.addView(editTextView)

        val builder = AlertDialog.Builder(context)
        builder.apply {
            setTitle(title)
            if (description != null) {
                setMessage(description)
            }
            setView(layout)
            setPositiveButton(okButton) { dialog, _ ->
                onClose(false, editTextView.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(cancelButton) { dialog, _ ->
                onClose(true, null)
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
        return dialog
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentItemListBinding {
        return FragmentItemListBinding.inflate(layoutInflater, container, false)
    }


}