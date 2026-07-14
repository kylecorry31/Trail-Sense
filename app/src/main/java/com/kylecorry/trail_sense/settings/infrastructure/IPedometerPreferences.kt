package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import java.time.Duration

interface IPedometerPreferences {
    var isEnabled: Boolean
    val resetDaily: Boolean
    var strideLength: Distance
    var alertDistance: Distance?
    val useAlarmForDistanceAlert: Boolean
    var stepHistory: Duration
    val averagePaceTimeMode: AveragePaceTimeMode
}
