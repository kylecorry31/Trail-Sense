package com.kylecorry.trail_sense.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentDiagnosticsBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.navigation.NavControllerAppNavigation

class DiagnosticsFragment : BoundFragment<FragmentDiagnosticsBinding>() {

    private lateinit var diagnostics: List<IDiagnostic>
    private lateinit var diagnosticListView: ListView<DiagnosticCode>

    private val titleLookup by lazy { DiagnosticCodeTitleLookup(requireContext()) }
    private val descriptionLookup by lazy { DiagnosticCodeDescriptionLookup(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDiagnosticsBinding {
        return FragmentDiagnosticsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.diagnosticsTitle.rightButton.setOnClickListener {
            findNavController().navigate(R.id.action_diagnostics_to_sensor_details)
        }
        diagnosticListView =
            ListView(binding.diagnosticsList, R.layout.list_item_plain_icon) { itemView, code ->
                val itemBinding = ListItemPlainIconBinding.bind(itemView)
                itemBinding.title.text = titleLookup.getTitle(code)
                itemBinding.description.text = descriptionLookup.getDescription(code)
                itemBinding.icon.setImageResource(R.drawable.ic_alert)
                CustomUiUtils.setImageColor(itemBinding.icon, getStatusTint(code.severity))
                itemBinding.root.setOnClickListener {
                    val alerter = DiagnosticAlertService(
                        requireContext(),
                        NavControllerAppNavigation(findNavController())
                    )
                    alerter.alert(code)
                }
            }
        diagnosticListView.addLineSeparator()
        diagnostics = listOfNotNull(
            AccelerometerDiagnostic(requireContext(), this),
            MagnetometerDiagnostic(requireContext(), this),
            GPSDiagnostic(requireContext(), this),
            BarometerDiagnostic(requireContext(), this),
            AltimeterDiagnostic(requireContext()),
            BatteryDiagnostic(requireContext(), this),
            LightSensorDiagnostic(requireContext(), this),
            CameraDiagnostic(requireContext()),
            FlashlightDiagnostic(requireContext()),
            PedometerDiagnostic(requireContext()),
            NotificationDiagnostic(requireContext()),
            WeatherMonitorDiagnostic(requireContext()),
            AlarmDiagnostic(requireContext())
        )
        scheduleUpdates(INTERVAL_1_FPS)
    }

    override fun onUpdate() {
        super.onUpdate()
        val results = diagnostics.flatMap { it.scan() }.toSet().sortedBy { it.severity.ordinal }
        binding.emptyText.isVisible = results.isEmpty()
        diagnosticListView.setData(results)
    }

    @ColorInt
    private fun getStatusTint(status: Severity): Int {
        return when (status) {
            Severity.Error -> AppColor.Red.color
            Severity.Warning -> AppColor.Yellow.color
        }
    }

}