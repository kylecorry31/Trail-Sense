package com.kylecorry.trail_sense.tools.weather.ui.dialogs

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command

class ShowHistoricTemperatureDisclaimerCommand(private val context: Context) : Command {
    override fun execute() {
        val source = "NASA Global Modeling and Assimilation Office - MERRA-2"
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.historic_temperatures),
            context.getString(
                R.string.historic_temperatures_full_disclaimer,
                30
            ) + "\n\n" + context.getString(R.string.data_source, source),
            context.getString(R.string.pref_historic_temperatures_disclaimer_shown),
            considerShownIfCancelled = true,
            cancelText = null,
            shownValue = true
        )
    }
}