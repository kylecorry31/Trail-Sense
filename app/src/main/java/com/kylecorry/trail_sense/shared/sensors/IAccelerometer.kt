package com.kylecorry.trail_sense.shared.sensors

interface IAccelerometer: ISensor {
    val acceleration: FloatArray
}