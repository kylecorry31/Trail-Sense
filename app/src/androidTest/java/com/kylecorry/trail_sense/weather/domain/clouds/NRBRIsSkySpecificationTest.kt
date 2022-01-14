package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import androidx.annotation.ColorInt
import org.junit.Assert.assertEquals
import org.junit.Test

internal class NRBRIsSkySpecificationTest {

    @Test
    fun testCanDetectSky(){
        val cases = listOf<List<Any>>(
            listOf(0.3f, Color.BLACK, false),
            listOf(0.6f, Color.BLACK, true),
            listOf(0.7f, Color.rgb(100, 0, 50), true),
            listOf(0.5f, Color.rgb(100, 0, 50), false),
            listOf(0.4f, Color.rgb(50, 0, 100), true),
            listOf(0.3f, Color.rgb(50, 0, 100), false),
        )

        for (case in cases){
            canDetectSky(case[0] as Float, case[1] as Int, case[2] as Boolean)
        }
    }


    private fun canDetectSky(threshold: Float, @ColorInt color: Int, expected: Boolean) {
        val spec = NRBRIsSkySpecification(threshold)
        assertEquals("$threshold, $color, $expected failed", expected, spec.isSatisfiedBy(color))
    }
}