package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2

// TODO: Specify the action
class SensorQualityDiagnostic(
    private val context: Context,
    lifecycleOwner: LifecycleOwner,
    private val sensor: ISensor,
    private val title: String
) : IDiagnostic {

    private val formatService = FormatServiceV2(context)

    init {
        sensor.asLiveData().observe(lifecycleOwner) {}
    }

    override fun getIssues(): List<DiagnosticIssue> {
        if (sensor.quality == Quality.Poor || sensor.quality == Quality.Moderate) {
            return listOf(
                DiagnosticIssue(
                    title,
                    context.getString(
                        R.string.quality,
                        formatService.formatQuality(sensor.quality)
                    ),
                    IssueSeverity.Warning
                )
            )
        }

        return listOf()
    }
}