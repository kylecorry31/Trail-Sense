package com.kylecorry.trail_sense.shared.sensors

interface ISensor {

    fun start(listener: SensorListener)

    fun stop(listener: SensorListener?)

}