package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.sol.units.Distance

interface IPedometerPreferences {
    var isEnabled: Boolean
    val resetDaily: Boolean
    val showNotification: Boolean
    var strideLength: Distance
    var alertDistance: Distance?
}