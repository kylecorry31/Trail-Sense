package com.kylecorry.trail_sense.tools.pedometer.infrastructure.odometer

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Instant
import java.time.LocalDate

class Odometer(private val context: Context): AbstractSensor(), IOdometer {

    private val cache by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }

    override val distance: Distance
        get() = Distance.meters(cache.getFloat(CACHE_KEY) ?: 0f)

    val lastReset: Instant
        get() = cache.getInstant(LAST_RESET) ?: Instant.now()

    override val hasValidReading: Boolean
        get() = cache.contains(CACHE_KEY)

    private val intervalometer = Timer {
        notifyListeners()
    }

    override fun startImpl() {
        intervalometer.interval(1000)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }

    fun addDistance(distance: Distance){
        synchronized(this) {
            val lastReset = cache.getInstant(LAST_RESET)
            if (lastReset != null && lastReset.toZonedDateTime().toLocalDate() != LocalDate.now() && prefs.resetOdometerDaily){
                // Reset it daily
                cache.putFloat(CACHE_KEY, 0f)
                cache.remove(LAST_LOCATION)
                cache.putInstant(LAST_RESET, Instant.now())
            } else if (lastReset == null){
                cache.putInstant(LAST_RESET, Instant.now())
            }
            val meters = distance.meters().distance + this.distance.meters().distance
            cache.putFloat(CACHE_KEY, meters)
            notifyListeners()
        }
    }

    fun reset(){
        synchronized(this) {
            cache.putFloat(CACHE_KEY, 0f)
            cache.remove(LAST_LOCATION)
            cache.putInstant(LAST_RESET, Instant.now())
            notifyListeners()
        }
    }

    companion object {
        private const val CACHE_KEY = "odometer_distance"
        private const val LAST_LOCATION = "last_odometer_location"
        private const val LAST_RESET = "last_odometer_reset"
    }

}