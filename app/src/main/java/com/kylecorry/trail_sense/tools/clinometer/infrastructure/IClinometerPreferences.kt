package com.kylecorry.trail_sense.tools.clinometer.infrastructure

import com.kylecorry.sol.units.Distance

interface IClinometerPreferences {
    var lockWithVolumeButtons: Boolean
    var baselineDistance: Distance?
}
