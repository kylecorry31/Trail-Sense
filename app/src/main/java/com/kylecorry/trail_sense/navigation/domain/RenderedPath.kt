package com.kylecorry.trail_sense.navigation.domain

import android.graphics.Path
import com.kylecorry.sol.units.Coordinate

data class RenderedPath(val origin: Coordinate, val path: Path)