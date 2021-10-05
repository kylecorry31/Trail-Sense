package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Duration
import java.time.Instant

class IsValidBacktrackPointSpecification(private val backtrackHistoryDuration: Duration) :
    Specification<PathPoint>() {
    override fun isSatisfiedBy(value: PathPoint): Boolean {
        return value.time != null && value.time > Instant.now().minus(backtrackHistoryDuration)
    }
}