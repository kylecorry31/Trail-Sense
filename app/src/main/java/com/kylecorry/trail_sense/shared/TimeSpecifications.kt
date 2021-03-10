package com.kylecorry.trail_sense.shared

import com.kylecorry.trailsensecore.domain.specifications.Specification
import java.time.LocalTime

class IsMorningSpecification : Specification<LocalTime>() {
    override fun isSatisfiedBy(value: LocalTime): Boolean {
        val start = LocalTime.of(6, 0)
        val end = LocalTime.of(10, 0)
        return value.isAfter(start) && value.isBefore(end)
    }
}
