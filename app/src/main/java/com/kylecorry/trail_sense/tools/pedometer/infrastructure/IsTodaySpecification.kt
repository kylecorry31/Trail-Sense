package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.time.Time.toZonedDateTime
import java.time.Instant
import java.time.LocalDate

class IsTodaySpecification: Specification<Instant>() {
    override fun isSatisfiedBy(value: Instant): Boolean {
        return value.toZonedDateTime().toLocalDate() == LocalDate.now()
    }
}