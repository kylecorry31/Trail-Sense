package com.kylecorry.trail_sense.shared.sensors2

interface IBarometer: ISensor, IAltimeter {
    val pressure: Float
}