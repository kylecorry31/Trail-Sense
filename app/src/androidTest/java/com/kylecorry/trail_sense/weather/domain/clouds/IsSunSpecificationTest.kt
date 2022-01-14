package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import androidx.annotation.ColorInt
import org.junit.Assert
import org.junit.Test

class IsSunSpecificationTest {

    @Test
    fun isSatisfiedBy() {
        val cases = listOf<List<Any>>(
            listOf(Color.BLACK, false),
            listOf(Color.WHITE, true),
            listOf(Color.rgb(255, 255, 255), true),
            listOf(Color.argb(255, 255, 255, 255), true),
            listOf(Color.rgb(200, 100, 0), false),
            listOf(Color.rgb(255, 255, 0), false)
        )

        for (case in cases){
            canDetectSun(case[0] as Int, case[1] as Boolean)
        }
    }


    private fun canDetectSun(@ColorInt color: Int, expected: Boolean) {
        val spec = IsSunSpecification()
        Assert.assertEquals(
            "$color, $expected failed",
            expected,
            spec.isSatisfiedBy(color)
        )
    }
}