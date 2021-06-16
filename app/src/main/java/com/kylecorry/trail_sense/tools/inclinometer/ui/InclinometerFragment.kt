package com.kylecorry.trail_sense.tools.inclinometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentInclinometerBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.inclinometer.AvalancheRisk
import com.kylecorry.trailsensecore.domain.inclinometer.InclinationService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.DeviceOrientation
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class InclinometerFragment : BoundFragment<FragmentInclinometerBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val inclinometer by lazy { sensorService.getInclinometer() }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val inclinationService = InclinationService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val throttle = Throttle(20)

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
            slopeAngle = if (slopeAngle == null && isOrientationValid()) {
                inclinometer.angle
            } else {
                null
            }
            binding.inclineLock.visibility =
                if (slopeAngle != null) View.VISIBLE else View.INVISIBLE
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

        if (!isOrientationValid() && slopeAngle == null) {
            // Display rotate icon / message
            binding.avalancheAlert.visibility = View.INVISIBLE
            binding.incline.text = getString(R.string.dash)
            binding.avalancheRisk.text = getString(R.string.inclinometer_rotate_device)
            return
        }

        val avalancheRisk = inclinationService.getAvalancheRisk(
            slopeAngle ?: inclinometer.angle
        )

        binding.avalancheAlert.visibility =
            if (avalancheRisk == AvalancheRisk.Low) View.INVISIBLE else View.VISIBLE
        binding.inclineLock.visibility = if (slopeAngle != null) View.VISIBLE else View.INVISIBLE

        binding.incline.text = getString(R.string.degree_format, slopeAngle ?: inclinometer.angle)
        binding.avalancheRisk.text = getAvalancheRiskString(avalancheRisk)

        updateObjectHeight()
    }

    private fun updateObjectHeight() {
        val incline = slopeAngle ?: inclinometer.angle

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
