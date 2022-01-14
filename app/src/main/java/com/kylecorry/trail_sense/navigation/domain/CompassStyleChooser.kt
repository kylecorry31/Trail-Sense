package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.trail_sense.settings.infrastructure.ICompassStylePreferences

class CompassStyleChooser(private val prefs: ICompassStylePreferences) {

    fun getStyle(orientation: DeviceOrientation.Orientation): CompassStyle {
        return if (prefs.useLinearCompass && orientation == DeviceOrientation.Orientation.Portrait) {
            CompassStyle.Linear
        } else if (prefs.useRadarCompass) {
            CompassStyle.Radar
        } else {
            CompassStyle.Round
        }
    }

}