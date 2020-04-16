package com.kylecorry.trail_sense.shared.sensors

interface IBarometer: ISensor, IAltimeter {
    val pressure: Float
}