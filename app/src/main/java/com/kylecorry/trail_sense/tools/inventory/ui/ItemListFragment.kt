package com.kylecorry.trail_sense.tools.inventory.ui

import android.content.Context
import android.content.res.ColorStateList
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryColorMapper
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentItemListBinding
import com.kylecorry.trail_sense.databinding.ListItemInventoryBinding
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem
import com.kylecorry.trail_sense.tools.inventory.infrastructure.ItemRepo
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryIconMapper
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

    private lateinit var listView: ListView<InventoryItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.inventoryList, R.layout.list_item_inventory) { itemView, item ->
            val itemBinding = ListItemInventoryBinding.bind(itemView)
            itemBinding.name.text = item.name
            itemBinding.count.text = DecimalFormatter.format(item.amount, 4, false)

            val imgMapper = ItemCategoryIconMapper()
            itemBinding.itemCategoryImg.setImageResource(imgMapper.getIcon(item.category))

            val colorMapper = ItemCategoryColorMapper()
            itemBinding.itemCategoryImg.imageTintList = ColorStateList.valueOf(
                UiUtils.color(
                    requireContext(),
                    colorMapper.map(item.category)
                )
            )

            val textColor = if (item.amount == 0.0){
                UiUtils.color(requireContext(), R.color.negative)
            } else {
                UiUtils.androidTextColorPrimary(requireContext())
            }

            itemBinding.name.setTextColor(textColor)
            itemBinding.count.setTextColor(textColor)

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
            binding.inventoryEmptyText.visibility = if (items.isEmpty()){
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
                )
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
                    ){ cancelled ->
                        if (!cancelled){
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

    private fun deleteItem(item: InventoryItem){
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                itemRepo.deleteItem(item)
            }
        }
    }

    private fun editItem(item: InventoryItem) {
        val bundle = bundleOf("edit_item_id" to item.id)
        findNavController().navigate(R.id.action_action_inventory_to_createItemFragment, bundle)
    }

    private fun addAmount(item: InventoryItem, amount: Double) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    item.copy(amount = max(0.0, item.amount + amount)).apply { id = item.id })
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