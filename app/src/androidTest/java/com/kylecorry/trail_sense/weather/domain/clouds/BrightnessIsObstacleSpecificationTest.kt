package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import androidx.annotation.ColorInt
import org.junit.Assert
import org.junit.Test

class BrightnessIsObstacleSpecificationTest {

    @Test
    fun isSatisfiedBy() {
        val cases = listOf<List<Any>>(
            listOf(0f, Color.BLACK, false),
            listOf(0.1f, Color.BLACK, true),
            listOf(255f, Color.WHITE, false),
            listOf(101f, Color.rgb(200, 100, 0), true),
            listOf(100f, Color.rgb(200, 100, 0), false)
        )

        for (case in cases){
            canDetectObstacle(case[0] as Float, case[1] as Int, case[2] as Boolean)
        }
    }


    private fun canDetectObstacle(threshold: Float, @ColorInt color: Int, expected: Boolean) {
        val spec = BrightnessIsObstacleSpecification(threshold)
        Assert.assertEquals(
            "$threshold, $color, $expected failed",
            expected,
            spec.isSatisfiedBy(color)
        )
    }
}