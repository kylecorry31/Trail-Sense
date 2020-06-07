package com.kylecorry.trail_sense.weather.ui

import java.time.Duration

interface IPressureChartSelectedListener {

    fun onNothingSelected()

    fun onValueSelected(timeAgo: Duration, pressure: Float)

}