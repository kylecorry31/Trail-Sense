package com.kylecorry.trail_sense.settings.infrastructure

interface ITidePreferences {
    val showNearestTide: Boolean
    var lastTide: Long?
}