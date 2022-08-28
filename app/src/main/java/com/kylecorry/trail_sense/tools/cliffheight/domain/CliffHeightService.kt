package com.kylecorry.trail_sense.tools.cliffheight.domain

import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import java.time.Duration
import java.time.Instant

class CliffHeightService {

    private val physics = PhysicsService()

    private var cachedGravity = Geology.GRAVITY
    private var cachedLocation: Coordinate? = null

    fun getCliffHeight(start: Instant, end: Instant, location: Coordinate? = null): Distance {
        val time = Duration.between(start, end)

        // TODO: Have physics service support variable gravity
        return if (location == null) {
            physics.fallHeight(time)
        } else {
            val gravity = if (location == cachedLocation) {
                cachedGravity
            } else {
                val g = Geology.getGravity(location)
                cachedGravity = g
                cachedLocation = location
                g
            }
            val seconds = time.toMillis() / 1000f
            Distance(0.5f * gravity * seconds * seconds, DistanceUnits.Meters)
        }
    }

}