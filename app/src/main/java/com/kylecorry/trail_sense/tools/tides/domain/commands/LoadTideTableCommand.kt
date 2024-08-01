package com.kylecorry.trail_sense.tools.tides.domain.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

class LoadTideTableCommand(private val context: Context) {

    suspend fun execute(): TideTable? = onIO {
        val loader = TideLoaderFactory().getTideLoader(context)
        val prefs = UserPreferences(context).tides
        loader.getTideTable()
            ?.copy(estimator = if (prefs.useLunitidalInterval) TideEstimator.LunitidalInterval else TideEstimator.Clock)
    }

}