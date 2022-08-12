package com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem

import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.FeatureState

interface IPedometerSubsystem {
    val steps: ITopic<Long>
    val distance: ITopic<Distance>
    val pace: ITopic<Speed>
    val state: ITopic<FeatureState>

    fun enable()
    fun disable()
}