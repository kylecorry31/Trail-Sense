package com.kylecorry.trail_sense.experimental.inclinometer.ui

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstroAltitude
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.domain.moon.MoonTruePhase
import com.kylecorry.trail_sense.astronomy.domain.moon.Tide
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode
import com.kylecorry.trail_sense.experimental.inclinometer.domain.AvalancheRisk
import com.kylecorry.trail_sense.experimental.inclinometer.domain.InclinationService
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.DeviceOrientation
import com.kylecorry.trail_sense.shared.sensors.IGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
import com.kylecorry.trail_sense.shared.system.UiUtils
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class InclinometerFragment : Fragment() {

    private lateinit var inclineContainer: ConstraintLayout
    private lateinit var inclineTxt: TextView
    private lateinit var avalancheRiskTxt: TextView
    private lateinit var lockImg: ImageView

    private val sensorService by lazy { SensorService(requireContext()) }
    private val inclinometer by lazy { sensorService.getInclinometer() }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientation() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val inclinationService = InclinationService()
    private val throttle = Throttle(20)

    private var slopeAngle: Float? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inclinometer, container, false)

        inclineTxt = view.findViewById(R.id.incline)
        avalancheRiskTxt = view.findViewById(R.id.avalanche_risk)
        inclineContainer = view.findViewById(R.id.incline_container)
        lockImg = view.findViewById(R.id.incline_lock)

        inclineContainer.setOnClickListener {
            slopeAngle = if (slopeAngle == null && isOrientationValid()) {
                inclinometer.angle
            } else {
                null
            }
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
            inclineTxt.text = getString(R.string.dash)
            avalancheRiskTxt.text = getString(R.string.inclinometer_rotate_device)
            return
        }

        val avalancheRisk = inclinationService.getAvalancheRisk(
            slopeAngle ?: inclinometer.angle
        )

        lockImg.visibility = if (slopeAngle != null) View.VISIBLE else View.INVISIBLE

        inclineTxt.text = getString(R.string.degree_format, slopeAngle ?: inclinometer.angle)
        avalancheRiskTxt.text = getAvalancheRiskString(avalancheRisk)

    }

    private fun getAvalancheRiskString(risk: AvalancheRisk): String {
        return when (risk) {
            AvalancheRisk.NotSteepEnough -> getString(R.string.avalanche_risk_low)
            AvalancheRisk.SlabsLessCommon -> getString(R.string.avalanche_risk_med)
            AvalancheRisk.MostAvalanches -> getString(R.string.avalanche_risk_high)
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
