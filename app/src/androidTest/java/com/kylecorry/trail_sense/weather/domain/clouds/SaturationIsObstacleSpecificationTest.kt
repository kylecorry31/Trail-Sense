package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import androidx.annotation.ColorInt
import org.junit.Assert
import org.junit.Test

class SaturationIsObstacleSpecificationTest {

    @Test
    fun isSatisfiedBy() {
        val cases = listOf<List<Any>>(
            listOf(0f, Color.BLACK, false),
            listOf(0f, Color.WHITE, false),
            listOf(0.9f, Color.RED, true),
            listOf(1f, Color.RED, false),
            listOf(0.3f, Color.rgb(200, 100, 127), true),
            listOf(0.6f, Color.rgb(200, 100, 127), false),
        )

        for (case in cases){
            canDetectObstacle(case[0] as Float, case[1] as Int, case[2] as Boolean)
        }
    }


    private fun canDetectObstacle(threshold: Float, @ColorInt color: Int, expected: Boolean) {
        val spec = SaturationIsObstacleSpecification(threshold)
        Assert.assertEquals(
            "$threshold, $color, $expected failed",
            expected,
            spec.isSatisfiedBy(color)
        )
    }
}