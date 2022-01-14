package com.kylecorry.trail_sense.tools.tides.ui.tidelistitem

import android.content.Context
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences

open class DefaultTideListItemFactory(protected val context: Context): ITideListItemFactory {
    protected val formatter = FormatService(context)
    protected val units by lazy { UserPreferences(context).baseDistanceUnits }

    override fun create(tide: Tide): TideListItem {
        return TideListItem(
            tide,
            this::formatTideType,
            this::formatTideTime,
            this::formatTideHeight
        )
    }

    protected open fun formatTideType(tide: Tide): String {
        val tideTypeMap = mapOf(
            true to context.getString(R.string.high_tide_letter),
            false to context.getString(R.string.low_tide_letter)
        )
        return tideTypeMap[tide.isHigh]!!
    }

    protected open fun formatTideTime(tide: Tide): String {
        return formatter.formatTime(tide.time.toLocalTime(), false)
    }

    protected open fun formatTideHeight(tide: Tide): String {
        return if (tide.height == null) {
            context.getString(R.string.dash)
        } else {
            formatter.formatDistance(Distance.meters(tide.height!!).convertTo(units), 2, true)
        }
    }

    protected open fun onClick(tide: Tide){

    }

}