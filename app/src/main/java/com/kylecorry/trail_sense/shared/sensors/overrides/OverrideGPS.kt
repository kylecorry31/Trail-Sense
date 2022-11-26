package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.IntervalSensor
import java.time.Duration
import java.time.Instant

class OverrideGPS(context: Context, updateFrequency: Long = 20L) :
    IntervalSensor(Duration.ofMillis(updateFrequency)),
    IGPS {

    private val userPrefs by lazy { UserPreferences(context) }

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
    override val mslAltitude: Float
        get() = altitude
}