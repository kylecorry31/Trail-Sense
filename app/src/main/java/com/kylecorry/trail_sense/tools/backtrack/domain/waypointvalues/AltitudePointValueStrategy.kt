package com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues

import android.content.Context
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class AltitudePointValueStrategy(private val context: Context) : IPointValueStrategy {
    override fun getValue(point: PathPoint): String {
        val elevation = point.elevation ?: return ""
        val units = UserPreferences(context).baseDistanceUnits
        val distance = Distance.meters(elevation).convertTo(units)
        return FormatService(context).formatDistance(distance, Units.getDecimalPlaces(units), false)
    }
}