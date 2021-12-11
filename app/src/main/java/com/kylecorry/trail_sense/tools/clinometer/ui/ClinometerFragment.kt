package com.kylecorry.trail_sense.tools.clinometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.AvalancheRisk
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentClinometerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService

class ClinometerFragment : BoundFragment<FragmentClinometerBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val clinometer by lazy { sensorService.getClinometer() }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val geology = GeologyService()
    private val formatter by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private var slopeIncline: Float? = null
    private var slopeAngle: Float? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.setButtonState(binding.clinometerLeftQuickAction, false)
        CustomUiUtils.setButtonState(binding.clinometerRightQuickAction, false)

        binding.root.setOnClickListener {
            if (slopeIncline == null && isOrientationValid()) {
                slopeAngle = clinometer.angle
                slopeIncline = clinometer.incline
            } else {
                slopeAngle = null
                slopeIncline = null
            }
        }

        clinometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        deviceOrientation.asLiveData().observe(viewLifecycleOwner, { updateUI() })

    }

    private fun updateUI() {

        if (throttle.isThrottled()) {
            return
        }

        if (!isOrientationValid() && slopeIncline == null) {
            binding.clinometerInstructions.text = getString(R.string.clinometer_rotate_device)
            return
        }

        binding.clinometerInstructions.text = getString(R.string.set_inclination_instructions)

        val avalancheRisk = geology.getAvalancheRisk(
            slopeIncline ?: clinometer.incline
        )

        val angle = 270 - (slopeAngle ?: clinometer.angle)
        val incline = slopeIncline ?: clinometer.incline

        binding.clinometer.angle = angle
        binding.clinometer.locked = slopeAngle != null

        binding.inclination.text = formatter.formatDegrees(slopeIncline ?: clinometer.incline)
        binding.avalancheRisk.title = getAvalancheRiskString(avalancheRisk)

        binding.inclinationDescription.text =
            getString(R.string.slope_amount, formatter.formatPercentage(getSlopePercent(incline)))

    }

    private fun getSlopePercent(incline: Float): Float {
        return SolMath.tanDegrees(incline) * 100
    }

    private fun getAvalancheRiskString(risk: AvalancheRisk): String {
        return when (risk) {
            AvalancheRisk.Low -> getString(R.string.low)
            AvalancheRisk.Moderate -> getString(R.string.moderate)
            AvalancheRisk.High -> getString(R.string.high)
        }
    }

    private fun isOrientationValid(): Boolean {
        return deviceOrientation.orientation != DeviceOrientation.Orientation.Flat && deviceOrientation.orientation != DeviceOrientation.Orientation.FlatInverse
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentClinometerBinding {
        return FragmentClinometerBinding.inflate(layoutInflater, container, false)
    }

}
