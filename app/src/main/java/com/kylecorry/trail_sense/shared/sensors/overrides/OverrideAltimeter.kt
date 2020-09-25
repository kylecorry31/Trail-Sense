package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class OverrideAltimeter(context: Context, private val updateFrequency: Long = 20L) :
    AbstractSensor(),
    IAltimeter {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = true

    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Intervalometer { notifyListeners() }

    override val altitude: Float
        get() = userPrefs.altitudeOverride

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}