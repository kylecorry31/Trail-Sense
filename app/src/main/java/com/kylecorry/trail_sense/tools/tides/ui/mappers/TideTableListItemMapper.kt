package com.kylecorry.trail_sense.tools.tides.ui.mappers

import android.content.Context
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.lists.*
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class TideTableListItemMapper(
    private val context: Context,
    private val actionHandler: (tide: TideTable, action: TideTableAction) -> Unit
) : ListItemMapper<Pair<TideTable, TideType?>> {

    private val formatter by lazy { FormatService(context) }

    override fun map(value: Pair<TideTable, TideType?>): ListItem {
        val table = value.first
        return ListItem(
            table.id,
            getTideTitle(table),
            getDescription(table),
            icon = getIcon(table, value.second),
            menu = listOf(
                ListMenuItem(context.getString(R.string.edit)) {
                    actionHandler(
                        table,
                        TideTableAction.Edit
                    )
                },
                ListMenuItem(
                    if (table.isVisible) context.getString(R.string.hide) else context.getString(
                        R.string.show
                    )
                ) { actionHandler(table, TideTableAction.ToggleVisibility) },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(
                        table,
                        TideTableAction.Delete
                    )
                },
            )
        ) {
            actionHandler(table, TideTableAction.Select)
        }

    }

    private fun getIcon(tide: TideTable, type: TideType?): ListIcon {
        return ResourceListIcon(
            if (tide.isVisible) getTideIcon(type) else R.drawable.ic_not_visible,
            if (tide.isVisible) null else Resources.androidTextColorSecondary(context)
        )
    }

    @DrawableRes
    private fun getTideIcon(tide: TideType?): Int {
        return when(tide){
            TideType.High -> R.drawable.ic_tide_high
            TideType.Low -> R.drawable.ic_tide_low
            null -> R.drawable.ic_tide_half
        }
    }

    private fun getDescription(tide: TideTable): String {
        return context.resources.getQuantityString(
            R.plurals.tides_entered_count,
            tide.tides.size,
            tide.tides.size
        )
    }

    private fun getTideTitle(tide: TideTable): String {
        return tide.name
            ?: if (tide.location != null) formatter.formatLocation(tide.location) else context.getString(
                android.R.string.untitled
            )
    }

}