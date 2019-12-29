package com.kylecorry.survival_aid.weather

import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.abs

data class PressureReading(val time: Instant, val reading: Float, val altitude: Double)

object PressureHistory: Observable() {

    val readings = mutableListOf<PressureReading>()

    private val keepDuration: Duration = Duration.ofHours(48)

    fun addReading(reading: Float, altitude: Double){
        val lastReading = readings.lastOrNull()
        var newReading = reading
        if (lastReading != null && abs(newReading - lastReading.reading) > 4){
            newReading = 0.5f * newReading + 0.5f * lastReading.reading
        }
        readings.add(PressureReading(Instant.now(), newReading, altitude))
        removeOldReadings()
        setChanged()
        notifyObservers()
    }

    fun setReadings(readings: List<PressureReading>){
        this.readings.clear()
        this.readings.addAll(readings)
        removeOldReadings()
        setChanged()
        notifyObservers()
    }

    fun removeOldReadings(){
        readings.removeIf { Duration.between(it.time, Instant.now()) > keepDuration }
    }
}