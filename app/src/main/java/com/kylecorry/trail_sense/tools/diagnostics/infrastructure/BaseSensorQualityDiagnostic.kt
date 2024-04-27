package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.topics.asLiveData
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic

abstract class BaseSensorQualityDiagnostic<T : ISensor>(
    protected val context: Context,
    lifecycleOwner: LifecycleOwner? = null,
    protected val sensor: T? = null
) : IDiagnostic {

    protected val canRun = lifecycleOwner != null && sensor != null

    init {
        lifecycleOwner?.let {
            sensor?.asLiveData()?.observe(it) {}
        }
    }
}