package com.kylecorry.trail_sense.tools.cliffheight.domain

import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.sol.units.Distance
import java.time.Duration
import java.time.Instant

class CliffHeightService {

    private val physics = PhysicsService()

    fun getCliffHeight(start: Instant, end: Instant): Distance {
        val duration = Duration.between(start, end)
        return physics.fallHeight(duration)
    }

}