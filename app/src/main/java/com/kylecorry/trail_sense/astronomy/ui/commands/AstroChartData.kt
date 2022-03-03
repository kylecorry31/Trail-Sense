package com.kylecorry.trail_sense.astronomy.ui.commands

import com.kylecorry.sol.units.Reading

data class AstroChartData(val sun: List<Reading<Float>>, val moon: List<Reading<Float>>)
