package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureTendency

interface IPressureTendencyRepository {
    fun getDescription(tendency: PressureTendency): String
}