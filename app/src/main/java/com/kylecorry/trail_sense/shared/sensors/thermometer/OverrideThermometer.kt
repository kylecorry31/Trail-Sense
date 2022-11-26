package com.kylecorry.trail_sense.shared.sensors.thermometer

import android.content.Context
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.IntervalSensor
import java.time.Duration

class OverrideThermometer(context: Context, frequency: Duration = Duration.ofMillis(20)) :
    IntervalSensor(frequency), IThermometer {

    private val preferences = UserPreferences(context).thermometer

    override val temperature: Float
        get() = preferences.temperatureOverride
}