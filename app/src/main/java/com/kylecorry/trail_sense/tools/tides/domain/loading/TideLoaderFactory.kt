package com.kylecorry.trail_sense.tools.tides.domain.loading

import android.content.Context
import com.kylecorry.trail_sense.settings.infrastructure.TidePreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tides.domain.selection.DefaultTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.FallbackTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.LastTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.NearestTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo

class TideLoaderFactory {

    fun getTideLoader(context: Context): ITideLoader {
        val prefs = TidePreferences(context)
        val strategy = if (prefs.showNearestTide) {
            FallbackTideSelectionStrategy(
                LastTideSelectionStrategy(prefs, true),
                NearestTideSelectionStrategy(SensorService(context).getGPS(false))
            )
        } else {
            LastTideSelectionStrategy(prefs, false)
        }

        return TideLoaderImpl(
            TideTableRepo.getInstance(context),
            FallbackTideSelectionStrategy(strategy, DefaultTideSelectionStrategy())
        )
    }

}