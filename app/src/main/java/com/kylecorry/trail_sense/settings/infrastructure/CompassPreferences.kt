package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R

class CompassPreferences(context: Context) : PreferenceRepo(context) {

    var compassSmoothing by IntPreference(
        cache,
        context.getString(R.string.pref_compass_filter_amt),
        1
    )

    var useTrueNorth by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_true_north),
        true
    )

    var source by StringEnumPreference(
        cache,
        context.getString(R.string.pref_compass_source),
        CompassSource.values().associateBy { it.id },
        CompassSource.RotationVector
    )

    /**
     * Returns the available compass sources in order of quality
     */
    fun getAvailableSources(): List<CompassSource> {
        val sources = mutableListOf<CompassSource>()

        if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
            sources.add(CompassSource.RotationVector)
        }

        if (Sensors.hasSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)) {
            sources.add(CompassSource.GeomagneticRotationVector)
        }

        if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
            sources.add(CompassSource.CustomMagnetometer)
        }

        @Suppress("DEPRECATION")
        if (Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)) {
            sources.add(CompassSource.Orientation)
        }

        return sources
    }

    enum class CompassSource(val id: String) {
        RotationVector("rotation_vector"),
        GeomagneticRotationVector("geomagnetic_rotation_vector"),
        CustomMagnetometer("custom_magnetometer"),
        Orientation("orientation")
    }
}