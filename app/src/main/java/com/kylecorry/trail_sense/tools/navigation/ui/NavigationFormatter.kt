package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService

class NavigationFormatter {

    private val formatter = getAppService<FormatService>()

    fun formatAzimuth(bearing: Float): String {
        val azimuthText = formatter.formatDegrees(bearing, replace360 = true).padStart(4, ' ')
        val directionText =
            formatter.formatDirection(CompassDirection.nearest(bearing)).padStart(2, ' ')
        return "$azimuthText   $directionText"
    }

}
