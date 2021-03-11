package com.kylecorry.trail_sense.shared

import com.kylecorry.trailsensecore.domain.specifications.Specification
import java.time.LocalTime

class IsMorningSpecification : Specification<LocalTime>() {
    override fun isSatisfiedBy(value: LocalTime): Boolean {
        val start = LocalTime.of(6, 0)
        val end = LocalTime.of(9, 0)
        return value >= start && value <= end
    }
}


class IsEveningSpecification : Specification<LocalTime>() {
    override fun isSatisfiedBy(value: LocalTime): Boolean {
        val start = LocalTime.of(18, 0)
        val end = LocalTime.of(21, 0)
        return value >= start && value <= end
    }
}
