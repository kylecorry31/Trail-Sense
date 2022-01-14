package com.kylecorry.trail_sense.tools.tides.ui.tidelistitem

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.R

class EstimatedTideListItemFactory(context: Context) : DefaultTideListItemFactory(context) {

    override fun onClick(tide: Tide) {
        Alerts.dialog(
            context,
            context.getString(R.string.disclaimer_estimated_tide_title),
            context.getString(R.string.disclaimer_estimated_tide),
            cancelText = null
        )
    }

    override fun formatTideHeight(tide: Tide): String {
        return context.getString(R.string.estimated)
    }
}