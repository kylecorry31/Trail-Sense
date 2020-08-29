package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import android.os.Handler
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.sensors.AbstractSensor
import com.kylecorry.trail_sense.shared.sensors.IGPS

class OverrideGPS(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(), IGPS {

    override val location: Coordinate
        get() = userPrefs.locationOverride
    override val speed: Float
        get() = 0f
    override val verticalAccuracy: Float?
        get() = null
    override val horizontalAccuracy: Float?
        get() = null
    override val satellites: Int
        get() = 0
    override val hasValidReading: Boolean
        get() = true
    override val altitude: Float
        get() = userPrefs.altitudeOverride

    private val userPrefs = UserPreferences(context)
    private val handler = Handler()
    private lateinit var updateRunnable: Runnable

    override fun startImpl() {
        updateRunnable = Runnable {
            notifyListeners()
            handler.postDelayed(updateRunnable, updateFrequency)
        }

        handler.post(updateRunnable)
    }

    override fun stopImpl() {
        handler.removeCallbacks(updateRunnable)
    }
}