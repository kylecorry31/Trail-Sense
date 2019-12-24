package com.kylecorry.survival_aid.weather

import java.time.Duration
import java.time.Instant

data class PressureReading(val time: Instant, val reading: Float)

object PressureHistory {

    val readings = mutableListOf<PressureReading>()

    private val keepDuration: Duration = Duration.ofHours(3)

    fun addReading(reading: Float){
        readings.add(PressureReading(Instant.now(), reading))
        removeOldReadings()
    }

    fun removeOldReadings(){
        readings.removeIf { Duration.between(it.time, Instant.now()) > keepDuration }
    }
}