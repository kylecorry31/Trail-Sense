package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime

data class ARGuidanceRefreshRequest(val context: Context, val location: Coordinate, val time: ZonedDateTime)
