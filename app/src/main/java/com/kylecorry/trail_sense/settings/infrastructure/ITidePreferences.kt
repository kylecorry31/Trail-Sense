package com.kylecorry.trail_sense.settings.infrastructure

interface ITidePreferences {
    var areTidesEnabled: Boolean
    val showNearestTide: Boolean
    var lastTide: Long?
}