package com.kylecorry.trail_sense.tools.inclinometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.sol.math.InclinationService
import com.kylecorry.sol.science.geology.AvalancheRisk
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentInclinometerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.SensorService

class InclinometerFragment : BoundFragment<FragmentInclinometerBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val inclinometer by lazy { sensorService.getInclinometer() }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val inclinationService = InclinationService()
    private val geoService = GeologyService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private var slopeIncline: Float? = null
    private var slopeAngle: Float? = null

    private var objectDistance: Distance? = null
    private var userHeight: Distance? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.setButtonState(binding.selectDistance, objectDistance != null)
        CustomUiUtils.setButtonState(binding.selectHeight, userHeight != null)

        val units = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            listOf(DistanceUnits.Meters, DistanceUnits.Feet)
        } else {
            listOf(DistanceUnits.Feet, DistanceUnits.Meters)
        }

        binding.selectDistance.text =
            "${getString(R.string.distance_away)}\n${getString(R.string.dash)}"
        binding.selectHeight.text =
            "${getString(R.string.your_height)}\n${getString(R.string.dash)}"


        binding.selectDistance.setOnClickListener {
            CustomUiUtils.pickDistance(
                requireContext(), units, objectDistance, getString(
                    R.string.distance_away
                )
            ) {
                if (it != null) {
                    objectDistance = it
                    CustomUiUtils.setButtonState(binding.selectDistance, true)
                    binding.selectDistance.text = "${getString(R.string.distance_away)}\n${
                        formatService.formatDistance(
                            it,
                            1
                        )
                    }"
                }
            }
        }

        binding.selectHeight.setOnClickListener {
            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                userHeight,
                getString(R.string.your_height)
            ) {
                if (it != null) {
                    userHeight = it
                    CustomUiUtils.setButtonState(binding.selectHeight, true)
                    binding.selectHeight.text =
                        "${getString(R.string.your_height)}\n${formatService.formatDistance(it, 1)}"
                }
            }
        }

        binding.root.setOnClickListener {
            if (slopeIncline == null && isOrientationValid()) {
                slopeAngle = inclinometer.angle
                slopeIncline = inclinometer.incline
            } else {
                slopeAngle = null
                slopeIncline = null
            }
        }

    }

    override fun onResume() {
        super.onResume()
        inclinometer.start(this::onInclinometerUpdate)
        deviceOrientation.start(this::onDeviceOrientationUpdate)
    }

    override fun onPause() {
        super.onPause()
        inclinometer.stop(this::onInclinometerUpdate)
        deviceOrientation.stop(this::onDeviceOrientationUpdate)
    }

    private fun updateUI() {

        if (throttle.isThrottled()) {
            return
        }

        if (!isOrientationValid() && slopeIncline == null) {
            binding.sideInclinometer.reset()
            binding.sideInclinometer.message = getString(R.string.inclinometer_rotate_device)
            return
        }

        binding.sideInclinometer.angle = slopeAngle ?: inclinometer.angle
        binding.sideInclinometer.incline = slopeIncline ?: inclinometer.incline

        val avalancheRisk = geoService.getAvalancheRisk(
            slopeIncline ?: inclinometer.incline
        )

        binding.sideInclinometer.color = when(avalancheRisk){
            AvalancheRisk.Low -> AppColor.Gray.color
            AvalancheRisk.High -> AppColor.Red.color
            AvalancheRisk.Moderate -> AppColor.Yellow.color
        }

        binding.sideInclinometer.locked = slopeAngle != null
        binding.sideInclinometer.message = getAvalancheRiskString(avalancheRisk)

        updateObjectHeight()
    }

    private fun updateObjectHeight() {
        val incline = slopeIncline ?: inclinometer.incline

        if (objectDistance == null) {
            binding.estimatedHeight.text = getString(R.string.dash)
            binding.estimatedHeightLbl.text = getString(R.string.distance_away_not_set)
        } else {
            val distMeters = objectDistance!!.meters().distance
            val heightMeters = userHeight?.meters()?.distance ?: 1.5f
            binding.estimatedHeight.text = formatService.formatDistance(
                Distance.meters(
                    inclinationService.estimateHeight(
                        distMeters,
                        incline,
                        heightMeters
                    )
                ).convertTo(prefs.baseDistanceUnits)
            )
            binding.estimatedHeightLbl.text = getString(R.string.estimated_height)
        }
    }

    private fun getAvalancheRiskString(risk: AvalancheRisk): String {
        return when (risk) {
            AvalancheRisk.Low -> getString(R.string.avalanche_risk_low)
            AvalancheRisk.Moderate -> getString(R.string.avalanche_risk_med)
            AvalancheRisk.High -> getString(R.string.avalanche_risk_high)
        }
    }

    private fun isOrientationValid(): Boolean {
        return deviceOrientation.orientation != DeviceOrientation.Orientation.Flat && deviceOrientation.orientation != DeviceOrientation.Orientation.FlatInverse
    }

    private fun onInclinometerUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onDeviceOrientationUpdate(): Boolean {
        updateUI()
        return true
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInclinometerBinding {
        return FragmentInclinometerBinding.inflate(layoutInflater, container, false)
    }

}
