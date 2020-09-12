package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import android.os.Handler
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter

class CachedAltimeter(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(),
    IAltimeter {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = true

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val userPrefs = UserPreferences(context)
    private val handler = Handler()
    private lateinit var updateRunnable: Runnable

    override val altitude: Float
        get() = prefs.getFloat(GPS.LAST_ALTITUDE, userPrefs.altitudeOverride)

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