package com.kylecorry.trail_sense.tools.tides.domain.loading

import android.content.Context
import com.kylecorry.trail_sense.settings.infrastructure.TidePreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.selection.DefaultTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.FallbackTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.LastTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.NearestTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo

class TideLoaderFactory {

    fun getTideLoader(context: Context, useLastTide: Boolean = true): ITideLoader {
        val prefs = TidePreferences(context)
        val location = LocationSubsystem.getInstance(context)
        val strategy = if (prefs.showNearestTide && useLastTide) {
            FallbackTideSelectionStrategy(
                LastTideSelectionStrategy(prefs, true),
                NearestTideSelectionStrategy { location.location }
            )
        } else if (prefs.showNearestTide) {
            NearestTideSelectionStrategy { location.location }
        } else {
            LastTideSelectionStrategy(prefs, false)
        }

        return TideLoaderImpl(
            TideTableRepo.getInstance(context),
            FallbackTideSelectionStrategy(strategy, DefaultTideSelectionStrategy())
        )
    }

}