package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trail_sense.shared.domain.Accuracy

interface ISensor {

    val accuracy: Accuracy

    fun start(listener: SensorListener)

    fun stop(listener: SensorListener?)

}