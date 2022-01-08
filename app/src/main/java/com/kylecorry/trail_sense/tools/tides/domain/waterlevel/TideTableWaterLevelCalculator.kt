package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.tools.tides.domain.SineWave
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.min

class TideTableWaterLevelCalculator(private val table: TideTable) : IWaterLevelCalculator {

    override fun calculate(time: ZonedDateTime): Float {
        if (table.tides.isEmpty()) {
            return 0f
        }

        if (table.tides.size == 1) {
            val tide = table.tides[0]
            return TideClockWaterLevelCalculator(tide).calculate(time)
        }


        val sortedTides = table.tides.sortedBy { it.time }
        var idx = -1

        for (i in 0 until sortedTides.lastIndex) {
            if (sortedTides[i].time <= time && sortedTides[i + 1].time >= time) {
                idx = i
                break
            }
        }

        val t = Duration.between(sortedTides[0].time, time).seconds / 3600f

        if (idx == -1) {
            // TODO: Maybe fall back to the tide clock
            idx = if (time <= sortedTides[0].time) {
                0
            } else {
                sortedTides.lastIndex - 1
            }
        }
        return getSineWaveBetween(
            sortedTides[idx],
            sortedTides[idx + 1],
            sortedTides[0].time
        ).cosine(t)
    }

    private fun getSineWaveBetween(tide1: Tide, tide2: Tide, reference: ZonedDateTime): SineWave {
        val period = Duration.between(tide1.time, tide2.time).seconds / 3600f
        val deltaHeight = abs(tide1.height - tide2.height)
        val verticalShift = deltaHeight / 2 + min(tide1.height, tide2.height)
        val frequency = (360 / (2 * period)).toRadians()
        val amplitude = (if (tide1.type == TideType.High) 1 else -1) * deltaHeight / 2
        val t = Duration.between(reference, tide1.time).seconds / 3600f
        return SineWave(amplitude, frequency, t, verticalShift)
    }
}