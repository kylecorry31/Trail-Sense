package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.trail_sense.settings.infrastructure.ICompassStylePreferences

class CompassStyleChooser(prefs: ICompassStylePreferences, private val isCompassAvailable: Boolean) {

    private val useLinearCompass = prefs.useLinearCompass
    private val useRadarCompass = prefs.useRadarCompass

    fun getStyle(orientation: DeviceOrientation.Orientation): CompassStyle {
        if (!isCompassAvailable){
            return CompassStyle.Radar
        }

        return if (useLinearCompass && orientation == DeviceOrientation.Orientation.Portrait) {
            CompassStyle.Linear
        } else if (useRadarCompass) {
            CompassStyle.Radar
        } else {
            CompassStyle.Round
        }
    }

}