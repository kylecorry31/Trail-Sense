package com.kylecorry.trail_sense.tools.inventory.ui

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryColorMapper
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentItemListBinding
import com.kylecorry.trail_sense.databinding.ListItemPackItemBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItemDto
import com.kylecorry.trail_sense.tools.inventory.domain.Pack
import com.kylecorry.trail_sense.tools.inventory.domain.PackItem
import com.kylecorry.trail_sense.tools.inventory.infrastructure.InventoryItemMapper
import com.kylecorry.trail_sense.tools.inventory.infrastructure.ItemRepo
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryIconMapper
import com.kylecorry.trail_sense.tools.inventory.ui.mappers.ItemCategoryStringMapper
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.text.DecimalFormatter
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Double.max

class ItemListFragment : BoundFragment<FragmentItemListBinding>() {

    private val itemRepo by lazy { ItemRepo.getInstance(requireContext()) }
    private lateinit var itemsLiveData: LiveData<List<InventoryItemDto>>
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private lateinit var listView: ListView<PackItem>

    private var pack: Pack? = null
    private var packId: Long = 0L

    private val itemMapper = InventoryItemMapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packId = arguments?.getLong("pack_id") ?: 0L
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                loadPack(packId)
            }
        }
    }

    private suspend fun loadPack(packId: Long) {
        withContext(Dispatchers.IO) {
            pack = itemRepo.getPack(packId)
        }
        withContext(Dispatchers.Main) {
            itemsLiveData = itemRepo.getItemsFromPack(packId)
            setupUI()
        }
    }

    private fun setupUI() {
        binding.inventoryListTitle.text = pack?.name
        listView = ListView(binding.inventoryList, R.layout.list_item_pack_item) { itemView, item ->
            val itemBinding = ListItemPackItemBinding.bind(itemView)
            itemBinding.name.text = item.name

            val currentAmount = formatAmount(item.amount)
            itemBinding.count.text = if (item.desiredAmount != 0.0) {
                "$currentAmount / ${formatAmount(item.desiredAmount)}"
            } else {
                currentAmount
            }

            val categoryTextMapper = ItemCategoryStringMapper(requireContext())
            val imgMapper = ItemCategoryIconMapper()
            val colorMapper = ItemCategoryColorMapper()


            itemBinding.itemCategory.text = categoryTextMapper.getString(item.category)
            itemBinding.itemCategoryImg.setImageResource(imgMapper.getIcon(item.category))
            val categoryColor = colorMapper.map(item.category).color
            itemBinding.itemCategory.setTextColor(categoryColor)
            itemBinding.itemCategoryImg.colorFilter =
                PorterDuffColorFilter(categoryColor, PorterDuff.Mode.SRC_IN)

            if (item.weight != null) {
                itemBinding.weight.isVisible = true
                itemBinding.weightImg.isVisible = true
                itemBinding.weight.text = formatService.formatWeight(item.packedWeight!!)
            } else {
                itemBinding.weight.isVisible = false
                itemBinding.weightImg.isVisible = false
            }

            itemBinding.itemCheckbox.isChecked = item.isFullyPacked

            itemBinding.itemCheckbox.setOnClickListener {
                onItemCheckboxClicked(item)
            }

            itemBinding.itemMenuBtn.setOnClickListener {
                UiUtils.openMenu(itemBinding.itemMenuBtn, R.menu.inventory_item_menu) {
                    when (it) {
                        R.id.action_item_add -> {
                            CustomUiUtils.pickNumber(
                                requireContext(),
                                getString(R.string.dialog_item_add),
                                null,
                                null,
                                allowNegative = false,
                                hint = getString(R.string.dialog_item_amount)
                            ) {
                                if (it != null) {
                                    addAmount(item, it.toDouble())
                                }
                            }
                        }
                        R.id.action_item_subtract -> {
                            CustomUiUtils.pickNumber(
                                requireContext(),
                                getString(R.string.dialog_item_subtract),
                                null,
                                null,
                                allowNegative = false,
                                hint = getString(R.string.dialog_item_amount)
                            ) {
                                if (it != null) {
                                    addAmount(item, -it.toDouble())
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
            }
        }

        listView.addLineSeparator()

        itemsLiveData.observe(viewLifecycleOwner) { items ->
            binding.inventoryEmptyText.isVisible = items.isEmpty()
            // TODO: Update sort criteria
            listView.setData(
                items.sortedWith(
                    compareBy(
                        { it.category.id },
                        { it.name })
                ).map { itemMapper.mapToPackItem(it) }
            )
        }

        binding.addBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_action_inventory_to_createItemFragment,
                bundleOf("pack_id" to packId)
            )
        }

        binding.inventoryMenuButton.setOnClickListener {
            UiUtils.openMenu(binding.inventoryMenuButton, R.menu.inventory_menu) {
                when (it) {
                    R.id.action_pack_rename -> {
                        pack?.let {
                            renamePack(it)
                        }
                    }
                    R.id.action_pack_delete -> {
                        pack?.let {
                            deletePack(it)
                        }
                    }
                    R.id.action_pack_clear_packed -> {
                        UiUtils.alertWithCancel(
                            requireContext(),
                            getString(R.string.clear_amounts),
                            getString(R.string.action_inventory_clear_confirm),
                            getString(R.string.dialog_ok),
                            getString(R.string.dialog_cancel)
                        ) { cancelled ->
                            if (!cancelled) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        itemRepo.clearPackedAmounts(packId)
                                    }
                                }
                            }
                        }
                    }
                }
                true
            }
        }
    }

    private fun renamePack(pack: Pack) {
        CustomUiUtils.pickText(
            requireContext(),
            getString(R.string.rename),
            null,
            pack.name,
            hint = getString(R.string.name_hint)
        ) {
            if (it != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        itemRepo.addPack(pack.copy(name = it))
                    }
                    withContext(Dispatchers.Main) {
                        binding.inventoryListTitle.text = it
                    }
                }
            }
        }
    }

    private fun deletePack(pack: Pack) {
        UiUtils.alertWithCancel(
            requireContext(),
            getString(R.string.delete_pack),
            pack.name
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        itemRepo.deletePack(pack)
                    }
                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun onItemCheckboxClicked(item: PackItem) {
        if (!item.isFullyPacked) {
            if (item.desiredAmount == 0.0) {
                setAmount(item, 1.0)
            } else {
                setAmount(item, item.desiredAmount)
            }
        } else {
            setAmount(item, 0.0)
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
        val bundle = bundleOf("edit_item_id" to item.id, "pack_id" to packId)
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

    private fun setAmount(item: PackItem, amount: Double) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    itemMapper.mapToInventoryItem(item).copy(amount = amount)
                        .apply { id = item.id })
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return DecimalFormatter.format(amount, 4, false)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentItemListBinding {
        return FragmentItemListBinding.inflate(layoutInflater, container, false)
    }


}