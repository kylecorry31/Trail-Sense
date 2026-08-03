package com.kylecorry.trail_sense.tools.tides.infrastructure

interface ITidePreferences {
    val showNearestTide: Boolean
    var lastTide: Long?
}
