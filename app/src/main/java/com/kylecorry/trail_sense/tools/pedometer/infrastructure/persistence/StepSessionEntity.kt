package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import com.kylecorry.sol.math.Range
import com.kylecorry.trail_sense.shared.database.Identifiable
import com.kylecorry.trail_sense.tools.pedometer.domain.StepSession
import java.time.Instant

data class StepSessionEntity(
    override val id: Long,
    val steps: Int,
    val start: Instant,
    val end: Instant
) : Identifiable {

    fun toStepSession(): StepSession {
        return StepSession(steps, Range(start, end))
    }

}
