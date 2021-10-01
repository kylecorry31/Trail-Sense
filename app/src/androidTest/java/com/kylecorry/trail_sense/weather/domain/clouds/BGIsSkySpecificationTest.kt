package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import androidx.annotation.ColorInt
import org.junit.Assert.assertEquals
import org.junit.Test

internal class BGIsSkySpecificationTest {

    @Test
    fun testCanDetectSky(){
        val cases = listOf<List<Any>>(
            listOf(30, Color.rgb(50, 70, 90), false),
            listOf(30, Color.rgb(50, 70, 100), true),
            listOf(30, Color.rgb(50, 70, 120), true),
            listOf(40, Color.rgb(50, 70, 100), false),
            listOf(30, Color.rgb(50, 70, 50), false),
            listOf(10, Color.rgb(50, 255, 255), false),
        )

        for (case in cases){
            canDetectSky(case[0] as Int, case[1] as Int, case[2] as Boolean)
        }

    }


    private fun canDetectSky(threshold: Int, @ColorInt color: Int, expected: Boolean) {
        val spec = BGIsSkySpecification(threshold)
        assertEquals("$threshold, $color, $expected failed", expected, spec.isSatisfiedBy(color))
    }
}