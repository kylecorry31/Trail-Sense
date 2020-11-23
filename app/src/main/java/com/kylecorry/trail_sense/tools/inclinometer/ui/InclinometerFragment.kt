package com.kylecorry.trail_sense.tools.inclinometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentInclinometerBinding
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.inclinometer.AvalancheRisk
import com.kylecorry.trailsensecore.domain.inclinometer.InclinationService
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.DeviceOrientation
import com.kylecorry.trailsensecore.infrastructure.time.Throttle

class InclinometerFragment : Fragment() {

    private lateinit var binding: FragmentInclinometerBinding

    private val sensorService by lazy { SensorService(requireContext()) }
    private val inclinometer by lazy { sensorService.getInclinometer() }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientation() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val inclinationService = InclinationService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private var slopeAngle: Float? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
            binding.objectDistance.hint = getString(R.string.object_distance_ft)
            binding.phoneHeight.hint = getString(R.string.your_height_ft)
        }

        binding.root.setOnClickListener {
            slopeAngle = if (slopeAngle == null && isOrientationValid()) {
                inclinometer.angle
            } else {
                null
            }
            binding.inclineLock.visibility = if (slopeAngle != null) View.VISIBLE else View.INVISIBLE
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInclinometerBinding.inflate(layoutInflater, container, false)
        return binding.root
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

        val units = prefs.distanceUnits

        val distance = binding.objectDistance.text.toString().toFloatOrNull()
        val phoneHeight =
            binding.phoneHeight.text.toString().toFloatOrNull() ?: LocationMath.convertToBaseUnit(
                1.5f,
                units
            )

        if (distance == null) {
            binding.estimatedHeight.text = getString(R.string.dash)
        } else {
            val distMeters = LocationMath.convertToMeters(distance, units)
            val heightMeters = LocationMath.convertToMeters(phoneHeight, units)
            binding.estimatedHeight.text = formatService.formatSmallDistance(
                inclinationService.estimateHeight(
                    distMeters,
                    incline,
                    heightMeters
                )
            )
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

}
