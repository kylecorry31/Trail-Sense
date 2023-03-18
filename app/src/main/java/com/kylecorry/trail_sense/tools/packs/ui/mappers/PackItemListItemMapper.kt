package com.kylecorry.trail_sense.tools.packs.ui.mappers

import android.content.Context
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.list.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.packs.domain.PackItem

enum class PackItemAction {
    Check,
    Add,
    Subtract,
    Edit,
    Delete
}


class PackItemListItemMapper(
    private val context: Context,
    private val actionHandler: (PackItem, PackItemAction) -> Unit
) : ListItemMapper<PackItem> {

    private val categoryTextMapper = ItemCategoryStringMapper(context)
    private val imgMapper = ItemCategoryIconMapper()
    private val colorMapper = ItemCategoryColorMapper()
    private val formatService = FormatService.getInstance(context)

    override fun map(value: PackItem): ListItem {
        val currentAmount = formatAmount(value.amount)
        val count = if (value.desiredAmount != 0.0) {
            "$currentAmount / ${formatAmount(value.desiredAmount)}"
        } else {
            currentAmount
        }
        val tag = ListItemTag(
            categoryTextMapper.getString(value.category),
            ResourceListIcon(imgMapper.getIcon(value.category), size = 16f),
            colorMapper.map(value.category).color
        )

        val weight = value.weight?.let { formatService.formatWeight(value.packedWeight!!) }

        val menu = listOf(
            ListMenuItem(context.getString(R.string.add)) {
                actionHandler(
                    value,
                    PackItemAction.Add
                )
            },
            ListMenuItem(context.getString(R.string.subtract)) {
                actionHandler(
                    value,
                    PackItemAction.Subtract
                )
            },
            ListMenuItem(context.getString(R.string.edit)) {
                actionHandler(
                    value,
                    PackItemAction.Edit
                )
            },
            ListMenuItem(context.getString(R.string.delete)) {
                actionHandler(
                    value,
                    PackItemAction.Delete
                )
            },
        )

        return ListItem(
            value.id,
            value.name,
            data = listOfNotNull(
                ListItemData(count, null),
                weight?.let {
                    ListItemData(
                        it,
                        ResourceListIcon(
                            R.drawable.ic_weight,
                            Resources.androidTextColorSecondary(context)
                        )
                    )
                },
            ),
            tags = listOf(tag),
            checkbox = ListItemCheckbox(value.isFullyPacked) {
                actionHandler(
                    value,
                    PackItemAction.Check
                )
            },
            menu = menu
        ) {
            actionHandler(value, PackItemAction.Edit)
        }
    }

    private fun formatAmount(amount: Double): String {
        return DecimalFormatter.format(amount, 4, false)
    }
}