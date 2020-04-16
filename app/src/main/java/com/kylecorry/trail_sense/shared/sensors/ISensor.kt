package com.kylecorry.trail_sense.shared.sensors2

interface ISensor {

    fun start(listener: SensorListener)

    fun stop(listener: SensorListener?)

}