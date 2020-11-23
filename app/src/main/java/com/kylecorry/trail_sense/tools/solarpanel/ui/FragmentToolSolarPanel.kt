package com.kylecorry.trail_sense.tools.solarpanel.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolSolarPanelBinding
import com.kylecorry.trail_sense.databinding.FragmentToolWhistleBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.math.deltaAngle
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.astronomy.AstronomyService
import com.kylecorry.trailsensecore.domain.astronomy.SolarPanelPosition
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

class FragmentToolSolarPanel : Fragment() {

    private val astronomyService = AstronomyService()
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val compass by lazy { sensorService.getCompass() }
    private val orientation by lazy { sensorService.getOrientationSensor() }
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private var position: SolarPanelPosition? = null

    private var _binding: FragmentToolSolarPanelBinding? = null
    private val binding get() = _binding!!

    private var useToday = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolSolarPanelBinding.inflate(inflater, container, false)
        updateButtonState()
        binding.solarTodayBtn.setOnClickListener {
            useToday = true
            updatePosition()
            updateButtonState()
        }
        binding.solarNowBtn.setOnClickListener {
            useToday = false
            updatePosition()
            updateButtonState()
        }
        UiUtils.alert(
            requireContext(),
            getString(R.string.tool_solar_panel_title),
            getString(R.string.solar_panel_instructions),
            getString(R.string.dialog_ok)
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (position == null) {
            gps.start(this::onGPSUpdate)
        }
        compass.start(this::update)
        orientation.start(this::update)
    }

    override fun onPause() {
        super.onPause()
        gps.stop(this::onGPSUpdate)
        compass.stop(this::update)
        orientation.stop(this::update)
    }

    private fun onGPSUpdate(): Boolean {
        updatePosition()
        return false
    }

    private fun updatePosition() {
        if (!gps.hasValidReading) {
            return
        }

        position = if (useToday) {
            astronomyService.getBestSolarPanelPositionForDay(ZonedDateTime.now(), gps.location)
        } else {
            astronomyService.getBestSolarPanelPositionForTime(ZonedDateTime.now(), gps.location)
        }
    }

    private fun updateButtonState() {
        setButtonState(
            binding.solarTodayBtn,
            useToday,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
        setButtonState(
            binding.solarNowBtn,
            !useToday,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
    }

    private fun update(): Boolean {
        if (throttle.isThrottled()) {
            return true
        }

        val solarPosition = position ?: return true

        binding.solarContent.visibility = View.VISIBLE
        binding.solarLoading.visibility = View.GONE
        val desiredAzimuth = solarPosition.bearing.inverse()
        val azimuthDiff = deltaAngle(desiredAzimuth.value, compass.bearing.value)
        val azimuthAligned = azimuthDiff.absoluteValue < AZIMUTH_THRESHOLD
        binding.azimuthComplete.visibility = if (azimuthAligned) View.VISIBLE else View.INVISIBLE
        binding.currentAzimuth.text = formatService.formatDegrees(compass.bearing.value)
        binding.desiredAzimuth.text = formatService.formatDegrees(desiredAzimuth.value)
        binding.arrowLeft.visibility =
            if (!azimuthAligned && azimuthDiff < 0) View.VISIBLE else View.INVISIBLE
        binding.arrowRight.visibility =
            if (!azimuthAligned && azimuthDiff > 0) View.VISIBLE else View.INVISIBLE

        val altitudeDiff = solarPosition.angle + orientation.orientation.y
        val altitudeAligned = altitudeDiff.absoluteValue < ALTITUDE_THRESHOLD
        binding.altitudeComplete.visibility = if (altitudeAligned) View.VISIBLE else View.INVISIBLE
        binding.currentAltitude.text = formatService.formatDegrees(-orientation.orientation.y)
        binding.desiredAltitude.text = formatService.formatDegrees(solarPosition.angle)
        binding.arrowUp.visibility =
            if (!altitudeAligned && altitudeDiff > 0) View.VISIBLE else View.INVISIBLE
        binding.arrowDown.visibility =
            if (!altitudeAligned && altitudeDiff < 0) View.VISIBLE else View.INVISIBLE

        return true
    }

    private fun setButtonState(
        button: Button,
        isOn: Boolean,
        @ColorInt primaryColor: Int,
        @ColorInt secondaryColor: Int
    ) {
        if (isOn) {
            button.setTextColor(secondaryColor)
            button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        } else {
            button.setTextColor(UiUtils.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(button.context))
        }
    }


    companion object {
        private const val AZIMUTH_THRESHOLD = 5
        private const val ALTITUDE_THRESHOLD = 5
    }

}