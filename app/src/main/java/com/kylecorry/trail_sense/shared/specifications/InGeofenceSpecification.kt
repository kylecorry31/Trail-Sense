package com.kylecorry.trail_sense.shared.specifications

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

class InGeofenceSpecification(private val center: Coordinate, private val radius: Distance) :
    Specification<Coordinate>() {
    override fun isSatisfiedBy(value: Coordinate): Boolean {
        val distance = center.distanceTo(value)
        return distance <= radius.meters().value
    }
}
