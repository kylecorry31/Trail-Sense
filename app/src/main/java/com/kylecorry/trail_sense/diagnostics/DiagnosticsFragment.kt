package com.kylecorry.trail_sense.diagnostics

import android.Manifest
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentDiagnosticsBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.shared.AppColor

class DiagnosticsFragment : BoundFragment<FragmentDiagnosticsBinding>() {

    private lateinit var diagnostics: List<IDiagnostic>
    private lateinit var diagnosticListView: ListView<DiagnosticIssue>

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDiagnosticsBinding {
        return FragmentDiagnosticsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        diagnosticListView =
            ListView(binding.diagnosticsList, R.layout.list_item_plain_icon) { itemView, result ->
                val itemBinding = ListItemPlainIconBinding.bind(itemView)
                itemBinding.title.text = result.title
                itemBinding.description.text = result.message
                itemBinding.icon.setImageResource(getStatusIcon(result.severity))
                itemBinding.icon.colorFilter =
                    PorterDuffColorFilter(getStatusTint(result.severity), PorterDuff.Mode.SRC_IN)
                itemBinding.root.setOnClickListener {
                    result.fullMessage?.let {
                        Alerts.dialog(
                            requireContext(),
                            result.title,
                            it.message ?: result.message,
                            okText = it.actionTitle ?: getString(android.R.string.ok)
                        ) { cancelled ->
                            if (!cancelled) {
                                it.action.invoke()
                            }
                        }
                    }
                }
            }
        diagnosticListView.addLineSeparator()
        diagnostics = listOfNotNull(
            PermissionDiagnostic(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION,
                getString(R.string.gps_location)
            ),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                PermissionDiagnostic(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    getString(R.string.permission_background_location)
                ) else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                PermissionDiagnostic(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    getString(R.string.permission_activity_recognition)
                ) else null,
            PermissionDiagnostic(
                requireContext(),
                Manifest.permission.CAMERA,
                getString(R.string.camera)
            ),
            SensorDiagnostic(
                requireContext(),
                Sensor.TYPE_MAGNETIC_FIELD,
                getString(R.string.pref_compass_sensor_title)
            ),
            SensorDiagnostic(
                requireContext(),
                Sensor.TYPE_ACCELEROMETER,
                getString(R.string.gravity)
            ),
            SensorDiagnostic(
                requireContext(),
                Sensor.TYPE_GYROSCOPE,
                getString(R.string.sensor_gyroscope)
            ),
            SensorDiagnostic(
                requireContext(),
                Sensor.TYPE_RELATIVE_HUMIDITY,
                getString(R.string.hygrometer)
            ),
            SensorDiagnostic(
                requireContext(),
                Sensor.TYPE_PRESSURE,
                getString(R.string.barometer)
            ),
            SensorDiagnostic(
                requireContext(),
                Sensor.TYPE_STEP_COUNTER,
                getString(R.string.pedometer)
            ),
            GPSDiagnostic(requireContext(), findNavController()),
            AltimeterDiagnostic(requireContext(), findNavController()),
            CameraDiagnostic(requireContext()),
            FlashlightDiagnostic(requireContext()),
            BatteryDiagnostic(requireContext(), findNavController())
        )
        scheduleUpdates(1000)
    }

    override fun onUpdate() {
        super.onUpdate()
        val results = diagnostics.flatMap { it.getIssues() }
        diagnosticListView.setData(results)
    }

    @ColorInt
    private fun getStatusTint(status: IssueSeverity): Int {
        return when (status) {
            IssueSeverity.Error -> AppColor.Red.color
            IssueSeverity.Warning -> AppColor.Yellow.color
        }
    }

    @DrawableRes
    private fun getStatusIcon(status: IssueSeverity): Int {
        return android.R.drawable.stat_notify_error
    }

}