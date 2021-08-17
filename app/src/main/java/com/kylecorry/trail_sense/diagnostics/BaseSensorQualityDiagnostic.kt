package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.asLiveData

abstract class BaseSensorQualityDiagnostic<T : ISensor>(
    protected val context: Context,
    lifecycleOwner: LifecycleOwner,
    protected val sensor: T
) : IDiagnostic {

    init {
        sensor.asLiveData().observe(lifecycleOwner) {}
    }
}