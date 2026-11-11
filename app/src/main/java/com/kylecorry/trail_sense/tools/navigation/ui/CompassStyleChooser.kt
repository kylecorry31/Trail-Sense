package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.trail_sense.tools.navigation.infrastructure.ICompassStylePreferences

class CompassStyleChooser(
    prefs: ICompassStylePreferences,
    private val isCompassAvailable: Boolean
) {

    private val useLinearCompass = prefs.useLinearCompass

    /**
     * @param incline the incline of the device in degrees, where 0 is upright and -90 is flat
     */
    fun getStyle(incline: Float): CompassStyle {
        val orientation = if (incline > FLAT_INCLINE_THRESHOLD) {
            DeviceOrientation.Orientation.Portrait
        } else {
            DeviceOrientation.Orientation.Flat
        }
        return getStyle(orientation)
    }

    fun getStyle(orientation: DeviceOrientation.Orientation): CompassStyle {
        if (!isCompassAvailable) {
            return CompassStyle.Radar
        }

        return if (useLinearCompass && orientation == DeviceOrientation.Orientation.Portrait) {
            CompassStyle.Linear
        } else {
            CompassStyle.Radar
        }
    }

    private companion object {
        const val FLAT_INCLINE_THRESHOLD = -30f
    }

}
