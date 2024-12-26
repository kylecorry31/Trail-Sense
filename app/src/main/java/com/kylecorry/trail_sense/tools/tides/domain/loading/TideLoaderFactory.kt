package com.kylecorry.trail_sense.tools.tides.domain.loading

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.TidePreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.selection.DefaultTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.EstimateTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.FallbackTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.LastTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.domain.selection.NearestTideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo

class TideLoaderFactory {

    fun getTideLoader(
        context: Context,
        useLastTide: Boolean = true,
        locationOverride: Coordinate? = null
    ): ITideLoader {
        val prefs = TidePreferences(context)
        val location = LocationSubsystem.getInstance(context)
        val strategy = if (prefs.showNearestTide && useLastTide) {
            FallbackTideSelectionStrategy(
                LastTideSelectionStrategy(prefs, true),
                NearestTideSelectionStrategy { locationOverride ?: location.location }
            )
        } else if (prefs.showNearestTide || !useLastTide) {
            NearestTideSelectionStrategy { locationOverride ?: location.location }
        } else {
            LastTideSelectionStrategy(prefs, false)
        }

        return TideLoaderImpl(
            TideTableRepo.getInstance(context),
            FallbackTideSelectionStrategy(
                strategy,
                EstimateTideSelectionStrategy(),
                LastTideSelectionStrategy(prefs, false),
                DefaultTideSelectionStrategy()
            )
        )
    }

}