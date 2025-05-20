package com.kylecorry.trail_sense.tools.cliffheight.domain

import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import java.time.Duration
import java.time.Instant

class CliffHeightService {

    private val hooks = Hooks()

    fun getCliffHeight(start: Instant, end: Instant, location: Coordinate? = null): Distance {
        val time = Duration.between(start, end)

        val gravity = hooks.memo(
            "gravity",
            location
        ) {
            location?.let { Geology.getGravity(it) } ?: Geology.GRAVITY
        }

        return Physics.fallHeight(time, gravity)
    }

}