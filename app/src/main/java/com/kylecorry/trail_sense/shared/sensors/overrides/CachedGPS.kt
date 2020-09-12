package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import android.os.Handler
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS

class CachedGPS(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(), IGPS {
    override val location: Coordinate
        get(){
            // TODO: Losing some precision here
            val lat = prefs.getFloat(GPS.LAST_LATITUDE, userPrefs.locationOverride.latitude.toFloat())
            val lng = prefs.getFloat(GPS.LAST_LONGITUDE, userPrefs.locationOverride.longitude.toFloat())
            return Coordinate(lat.toDouble(), lng.toDouble())
        }
    override val speed: Float
        get() = prefs.getFloat(GPS.LAST_SPEED, 0.0f)
    override val verticalAccuracy: Float?
        get() = null
    override val horizontalAccuracy: Float?
        get() = null
    override val satellites: Int
        get() = 0
    override val hasValidReading: Boolean
        get() = true
    override val altitude: Float
        get() = prefs.getFloat(GPS.LAST_ALTITUDE, userPrefs.altitudeOverride)

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
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