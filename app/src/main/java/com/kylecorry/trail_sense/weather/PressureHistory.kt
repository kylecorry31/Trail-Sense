package com.kylecorry.trail_sense.weather

import java.time.Duration
import java.time.Instant
import java.util.*

data class PressureReading(val time: Instant, val pressure: Float, val altitude: Double)

object PressureHistory: Observable() {

    val readings = mutableListOf<PressureReading>()

    private val keepDuration: Duration = Duration.ofHours(48)

    fun addReading(reading: Float, altitude: Double){
        readings.add(PressureReading(Instant.now(), reading, altitude))
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