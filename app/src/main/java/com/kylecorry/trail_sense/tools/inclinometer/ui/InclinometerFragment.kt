package com.kylecorry.trail_sense.tools.inclinometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.inclinometer.domain.AvalancheRisk
import com.kylecorry.trail_sense.tools.inclinometer.domain.InclinationService
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.DeviceOrientation
import com.kylecorry.trail_sense.shared.sensors.SensorService

class InclinometerFragment : Fragment() {

    private lateinit var inclineContainer: ConstraintLayout
    private lateinit var heightTxt: TextView
    private lateinit var distanceEdit: EditText
    private lateinit var phoneHeightEdit: EditText

    private lateinit var inclineTxt: TextView
    private lateinit var avalancheRiskTxt: TextView
    private lateinit var lockImg: ImageView
    private lateinit var avalancheImg: ImageView

    private val sensorService by lazy { SensorService(requireContext()) }
    private val inclinometer by lazy { sensorService.getInclinometer() }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientation() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val inclinationService = InclinationService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private var slopeAngle: Float? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inclinometer, container, false)

        heightTxt = view.findViewById(R.id.estimated_height)
        distanceEdit = view.findViewById(R.id.object_distance)
        phoneHeightEdit = view.findViewById(R.id.phone_height)
        avalancheImg = view.findViewById(R.id.avalanche_alert)
        inclineTxt = view.findViewById(R.id.incline)
        avalancheRiskTxt = view.findViewById(R.id.avalanche_risk)
        inclineContainer = view.findViewById(R.id.incline_container)
        lockImg = view.findViewById(R.id.incline_lock)

        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
            distanceEdit.hint = getString(R.string.object_distance_ft)
            phoneHeightEdit.hint = getString(R.string.your_height_ft)
        }

        inclineContainer.setOnClickListener {
            slopeAngle = if (slopeAngle == null && isOrientationValid()) {
                inclinometer.angle
            } else {
                null
            }
            lockImg.visibility = if (slopeAngle != null) View.VISIBLE else View.INVISIBLE
        }

        return view
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
            avalancheImg.visibility = View.INVISIBLE
            inclineTxt.text = getString(R.string.dash)
            avalancheRiskTxt.text = getString(R.string.inclinometer_rotate_device)
            return
        }

        val avalancheRisk = inclinationService.getAvalancheRisk(
            slopeAngle ?: inclinometer.angle
        )

        avalancheImg.visibility =
            if (avalancheRisk == AvalancheRisk.Low) View.INVISIBLE else View.VISIBLE
        lockImg.visibility = if (slopeAngle != null) View.VISIBLE else View.INVISIBLE

        inclineTxt.text = getString(R.string.degree_format, slopeAngle ?: inclinometer.angle)
        avalancheRiskTxt.text = getAvalancheRiskString(avalancheRisk)

        updateObjectHeight()
    }

    private fun updateObjectHeight() {
        val incline = slopeAngle ?: inclinometer.angle

        val units = prefs.distanceUnits

        val distance = distanceEdit.text.toString().toFloatOrNull()
        val phoneHeight =
            phoneHeightEdit.text.toString().toFloatOrNull() ?: LocationMath.convertToBaseUnit(
                1.5f,
                units
            )

        if (distance == null) {
            heightTxt.text = getString(R.string.dash)
        } else {
            val distMeters = LocationMath.convertToMeters(distance, units)
            val heightMeters = LocationMath.convertToMeters(phoneHeight, units)
            heightTxt.text = formatService.formatSmallDistance(
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
