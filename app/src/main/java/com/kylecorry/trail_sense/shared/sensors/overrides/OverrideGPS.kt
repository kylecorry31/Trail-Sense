package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.core.units.Speed
import com.kylecorry.andromeda.core.units.TimeUnits
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Instant

class OverrideGPS(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(),
    IGPS {

    override val location: Coordinate
        get() = userPrefs.locationOverride
    override val speed: Speed
        get() = Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    override val time: Instant
        get() = Instant.now()
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
    override val mslAltitude: Float?
        get() = altitude

    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Timer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}