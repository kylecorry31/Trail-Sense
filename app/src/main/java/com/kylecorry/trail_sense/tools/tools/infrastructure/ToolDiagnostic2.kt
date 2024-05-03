package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class ToolDiagnostic2(
    val id: String,
    val name: String,
    val quickScan: (fragment: AndromedaFragment) -> List<ToolDiagnosticResult>,
    val fullScan: (fragment: AndromedaFragment) -> Flow<List<ToolDiagnosticResult>> = {
        flowOf(quickScan(it))
    }
)

data class ToolDiagnosticResult(
    val id: String,
    val severity: Severity,
    val name: String,
    val description: String,
    val resolution: String? = null,
    val action: ToolDiagnosticAction? = null
)

data class ToolDiagnosticAction(
    val name: String,
    val action: (fragment: AndromedaFragment) -> Unit
) {
    companion object {
        fun navigate(
            @IdRes to: Int,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) {
                it.findNavController().navigateWithAnimation(to)
            }
        }

        fun command(
            command: Command,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) {
                command.execute()
            }
        }

        fun intent(
            intent: Intent,
            title: String
        ): ToolDiagnosticAction {
            return ToolDiagnosticAction(title) {
                it.startActivity(intent)
            }
        }

        fun permissions(context: Context): ToolDiagnosticAction {
            return intent(Intents.appSettings(context), context.getString(R.string.settings))
        }

        fun notification(context: Context, channel: String?): ToolDiagnosticAction {
            return intent(
                Intents.notificationSettings(context, channel),
                context.getString(R.string.settings)
            )
        }
    }
}

// An example diagnostic that checks for the presence of a flashlight
//val flashlightDiagnostic = ToolDiagnostic2(
//    "flashlight-diagnostic",
//    "Flashlight",
//    quickScan = {
//        if (!FlashlightSubsystem.getInstance(it.requireContext()).isAvailable()) {
//            listOf(
//                ToolDiagnosticResult(
//                    "flashlight-diagnostic",
//                    Severity.Warning,
//                    "Flashlight not available",
//                    "The flashlight is not available on this device"
//                )
//            )
//        } else {
//            emptyList()
//        }
//    })
//
//val gpsQualityDiagnostic = ToolDiagnostic2(
//    "gps-quality-diagnostic",
//    "GPS Quality",
//    quickScan = {
//        emptyList()
//    },
//    fullScan = {
//        val gps = SensorService(it.requireContext()).getGPS()
//        gps.flow.map {
//            if (gps.quality == Quality.Poor) {
//                listOf(
//                    ToolDiagnosticResult(
//                        "gps-quality-diagnostic",
//                        Severity.Warning,
//                        "GPS Quality",
//                        "The GPS signal quality is poor",
//                        "Move to an area with a better view of the sky"
//                    )
//                )
//            } else {
//                emptyList()
//            }
//        }
//            .distinctUntilChanged()
//    }
//)