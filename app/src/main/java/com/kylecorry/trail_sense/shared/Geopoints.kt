package com.kylecorry.trail_sense.shared

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.time.Duration
import java.time.Instant

interface NavigationIndicator

// Used to represent a track
data class TrackIndicator(val id: Long, val waypoints: List<TrackWaypointIndicator>, @ColorInt val color: Int, val dotted: Boolean = false, val fadeDuration: Duration? = null): NavigationIndicator
data class TrackWaypointIndicator(val location: Coordinate, val time: Instant? = null)

// Used to represent a waypoint (or beacon)
data class WaypointIndicator(val id: Long, val location: Coordinate, @ColorInt val color: Int? = null, @DrawableRes val icon: Int? = null): NavigationIndicator

// Used to represent a bearing
data class BearingIndicator(val bearing: Bearing, @ColorInt val color: Int? = null, @DrawableRes val icon: Int? = null): NavigationIndicator