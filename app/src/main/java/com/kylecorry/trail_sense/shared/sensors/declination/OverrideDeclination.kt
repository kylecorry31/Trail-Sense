package com.kylecorry.trail_sense.shared.sensors.declination

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.declination.IDeclinationProvider
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class OverrideDeclination(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(),
    IDeclinationProvider {
    override val declination: Float
        get() = userPrefs.declinationOverride

    override val hasValidReading: Boolean
        get() = true

    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Intervalometer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}