package com.kylecorry.survival_aid.weather

import java.time.Duration
import java.time.Instant

data class PressureReading(val time: Instant, val reading: Float)

object PressureHistory {

    val readings = mutableListOf<PressureReading>()

    private val keepDuration: Duration = Duration.ofHours(48)
//
//    init {
////        readings.add(PressureReading(Instant.now().minus(Duration.ofDays(1)), 1000.1f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(160)), 1000.1f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(145)), 1000.5f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(130)), 1001.0f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(115)), 1001.1f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(60)), 1000.1f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(45)), 1000.5f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(30)), 1001.0f))
//        readings.add(PressureReading(Instant.now().minus(Duration.ofMinutes(15)), 1001.1f))
//    }

    fun addReading(reading: Float){
        readings.add(PressureReading(Instant.now(), reading))
        removeOldReadings()
    }

    fun removeOldReadings(){
        readings.removeIf { Duration.between(it.time, Instant.now()) > keepDuration }
    }
}