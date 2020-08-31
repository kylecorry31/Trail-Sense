package com.kylecorry.trail_sense.shared.sensors.declination

import android.content.Context
import android.os.Handler
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.AbstractSensor

class OverrideDeclination(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(), IDeclinationProvider {
    override val declination: Float
        get() = userPrefs.declinationOverride

    override val hasValidReading: Boolean
        get() = true

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