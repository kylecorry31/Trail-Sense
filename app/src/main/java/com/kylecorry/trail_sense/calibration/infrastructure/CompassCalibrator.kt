package com.kylecorry.trail_sense.calibration.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Coordinate

class CompassCalibrator(context: Context) {

    private val prefs = UserPreferences(context)
    private val declinationCalculator = DeclinationCalculator()

    fun setDeclinationManual(declination: Float){
        prefs.useAutoDeclination = false
        prefs.declinationOverride = declination
    }

    fun setDeclinationManual(coordinate: Coordinate, altitude: Float){
        setDeclinationManual(getDeclinationAuto(coordinate, altitude))
    }

    fun getDeclinationManual(): Float {
        return prefs.declinationOverride
    }

    fun getDeclinationAuto(coordinate: Coordinate, altitude: Float): Float {
        return declinationCalculator.calculate(coordinate, altitude)
    }

}