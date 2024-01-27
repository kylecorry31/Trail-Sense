package com.kylecorry.trail_sense.tools.astronomy.ui.commands

import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime

interface IAstroChartDataProvider {
    fun get(location: Coordinate, time: ZonedDateTime): AstroChartData
}