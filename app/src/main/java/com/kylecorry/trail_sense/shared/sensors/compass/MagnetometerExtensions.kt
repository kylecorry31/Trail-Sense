package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector3Utils

fun IMagnetometer.getQualityFromFieldStrength(): Quality {
    // https://geomag.bgs.ac.uk/education/earthmag.html
    val normalStrengthRange = Range(22f, 67f)
    val warningStrengthRange =
        Range(normalStrengthRange.start * 0.9f, normalStrengthRange.end * 1.1f)

    val strength = Vector3Utils.magnitude(rawMagneticField)
    return when {
        normalStrengthRange.contains(strength) -> Quality.Good
        warningStrengthRange.contains(strength) -> Quality.Moderate
        SolMath.isZero(strength) -> Quality.Unknown
        else -> Quality.Poor
    }
}