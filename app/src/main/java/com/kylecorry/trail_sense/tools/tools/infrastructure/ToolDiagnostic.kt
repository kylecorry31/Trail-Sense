package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.AccelerometerDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.AlarmDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.AltimeterDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.BarometerDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.BatteryDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.CameraDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.FlashlightDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.GPSDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.LightSensorDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.MagnetometerDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.NotificationDiagnostic

// TODO: It shouldn't need the fragment
// TODO: It should have a single suspend function / flow to get the codes (takes in context)
data class ToolDiagnostic(
    val id: String,
    val create: (fragment: AndromedaFragment) -> IDiagnostic
) {
    companion object {
        val accelerometer = ToolDiagnostic("accelerometer-diagnostic") {
            AccelerometerDiagnostic(
                it.requireContext(),
                it
            )
        }
        val alarm = ToolDiagnostic("alarm-diagnostic") { AlarmDiagnostic(it.requireContext()) }
        val altimeterOverride =
            ToolDiagnostic("altimeter-override-diagnostic") { AltimeterDiagnostic(it.requireContext()) }
        val barometer =
            ToolDiagnostic("barometer-diagnostic") { BarometerDiagnostic(it.requireContext(), it) }
        val battery =
            ToolDiagnostic("battery-diagnostic") { BatteryDiagnostic(it.requireContext(), it) }
        val camera = ToolDiagnostic("camera-diagnostic") { CameraDiagnostic(it.requireContext()) }
        val flashlight =
            ToolDiagnostic("flashlight-diagnostic") { FlashlightDiagnostic(it.requireContext()) }
        val gps = ToolDiagnostic("gps-diagnostic") { GPSDiagnostic(it.requireContext(), it) }
        val light =
            ToolDiagnostic("light-diagnostic") { LightSensorDiagnostic(it.requireContext(), it) }
        val magnetometer = ToolDiagnostic("magnetometer-diagnostic") {
            MagnetometerDiagnostic(
                it.requireContext(),
                it
            )
        }

        // TODO: Add gyro
        val compass = arrayOf(accelerometer, magnetometer)
        val sightingCompass = arrayOf(*compass, camera)
        val altimeter = arrayOf(barometer, altimeterOverride, gps)

        // TODO: Add gyro
        val tilt = arrayOf(accelerometer)

        fun notification(channelId: String, code: DiagnosticCode): ToolDiagnostic {
            return ToolDiagnostic("notification-diagnostic-${channelId.slugify()}") {
                NotificationDiagnostic(
                    it.requireContext(),
                    channelId,
                    code
                )
            }
        }
    }
}