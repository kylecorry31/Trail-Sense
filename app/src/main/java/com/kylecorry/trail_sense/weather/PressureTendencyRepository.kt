package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureTendency

class PressureTendencyRepository : IPressureTendencyRepository {
    override fun getDescription(tendency: PressureTendency): String {
        return when(tendency){
            PressureTendency.FALLING_SLOW -> "Weather may worsen"
            PressureTendency.RISING_SLOW -> "Weather may improve"
            PressureTendency.FALLING_FAST -> "Weather will worsen soon"
            PressureTendency.RISING_FAST -> "Weather will improve soon "
            PressureTendency.NO_CHANGE -> "Weather not changing"
        }
    }
}