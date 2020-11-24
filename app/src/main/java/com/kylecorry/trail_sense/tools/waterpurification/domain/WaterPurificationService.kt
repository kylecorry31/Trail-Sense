package com.kylecorry.trail_sense.tools.waterpurification.domain

import java.time.Duration

class WaterPurificationService {

    fun getPurificationTime(altitude: Float?): Duration {
        if (altitude == null || altitude >= 1000f){
            return Duration.ofMinutes(3)
        }

        return Duration.ofMinutes(1)
    }

}