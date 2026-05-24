package com.kylecorry.trail_sense.main.theme

import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService

class ThemeProvider {

    private val prefs = getAppService<UserPreferences>()
    private val astronomy = AstronomyService()
    private val sensors = getAppService<SensorSubsystem>()

    fun getColorTheme(): ColorTheme {
        return when (prefs.theme) {
            UserPreferences.Theme.Light -> ColorTheme.Light
            UserPreferences.Theme.Dark, UserPreferences.Theme.Black, UserPreferences.Theme.Night -> ColorTheme.Dark
            UserPreferences.Theme.System, UserPreferences.Theme.SystemBlack -> ColorTheme.System
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
    }

    private fun sunriseSunsetTheme(): ColorTheme {
        val location = sensors.lastKnownLocation
        if (location == Coordinate.zero) {
            return ColorTheme.System
        }
        val isSunUp = astronomy.isSunUp(location)
        return if (isSunUp) {
            ColorTheme.Light
        } else {
            ColorTheme.Dark
        }
    }
}
