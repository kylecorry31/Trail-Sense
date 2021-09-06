package com.kylecorry.trail_sense.tools.solarpanel.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.orientation.GravityOrientationSensor
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.science.astronomy.SolarPanelPosition
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolSolarPanelBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.solarpanel.domain.SolarPanelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.math.absoluteValue

class FragmentToolSolarPanel : BoundFragment<FragmentToolSolarPanelBinding>() {

    private val solarPanelService = SolarPanelService()
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val compass by lazy { sensorService.getCompass() }
    private val orientation by lazy { GravityOrientationSensor(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var position: SolarPanelPosition? = null
    private var nowDuration = Duration.ofHours(2)
    private var alignToRestOfDay = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateButtonState()
        binding.solarTodayBtn.setOnClickListener {
            position = null
            alignToRestOfDay = true
            updatePosition()
            updateButtonState()
        }
        binding.solarNowBtn.setOnClickListener {
            position = null
            alignToRestOfDay = false
            CustomUiUtils.pickDuration(requireContext(), nowDuration, getString(R.string.duration_of_charge)){
                if (it != null){
                    nowDuration = it
                    updatePosition()
                    updateButtonState()
                }
            }
        }
        Alerts.dialog(
            requireContext(),
            getString(R.string.tool_solar_panel_title),
            getString(R.string.solar_panel_instructions),
            cancelText = null
        )

        compass.asLiveData().observe(viewLifecycleOwner, {})
        orientation.asLiveData().observe(viewLifecycleOwner, {})

        scheduleUpdates(INTERVAL_30_FPS)
        throttleUpdates(16)
    }

    override fun onResume() {
        super.onResume()
        if (position == null) {
            gps.start(this::onGPSUpdate)
        }
    }

    override fun onPause() {
        super.onPause()
        gps.stop(this::onGPSUpdate)
    }

    private fun getDeclination(): Float {
        return declination.getDeclination()
    }

    private fun onGPSUpdate(): Boolean {
        updatePosition()
        return false
    }

    private fun updatePosition() {
        runInBackground {
            withContext(Dispatchers.IO) {
                position = solarPanelService.getBestPosition(
                    gps.location,
                    if (alignToRestOfDay) Duration.ofDays(1) else nowDuration
                )
            }
        }
    }

    private fun updateButtonState() {
        setButtonState(
            binding.solarTodayBtn,
            alignToRestOfDay,
            Resources.color(requireContext(), R.color.colorPrimary),
            Resources.color(requireContext(), R.color.colorSecondary)
        )
        setButtonState(
            binding.solarNowBtn,
            !alignToRestOfDay,
            Resources.color(requireContext(), R.color.colorPrimary),
            Resources.color(requireContext(), R.color.colorSecondary)
        )
    }

    override fun onUpdate() {
        binding.solarNowBtn.text = formatService.formatDuration(nowDuration)

        if (position == null) {
            binding.solarContent.isVisible = false
            binding.solarLoading.isVisible = true
        }

        val solarPosition = position ?: return

        if (prefs.navigation.useTrueNorth) {
            compass.declination = getDeclination()
        } else {
            compass.declination = 0f
        }

        binding.solarContent.isVisible = true
        binding.solarLoading.isVisible = false
        val declinationOffset = if (prefs.navigation.useTrueNorth) {
            0f
        } else {
            -getDeclination()
        }
        val desiredAzimuth = solarPosition.bearing.withDeclination(declinationOffset).inverse()
        val azimuthDiff = deltaAngle(desiredAzimuth.value, compass.bearing.value)
        val azimuthAligned = azimuthDiff.absoluteValue < AZIMUTH_THRESHOLD
        binding.azimuthComplete.visibility = if (azimuthAligned) View.VISIBLE else View.INVISIBLE
        binding.currentAzimuth.text =
            formatService.formatDegrees(compass.bearing.value, replace360 = true)
        binding.desiredAzimuth.text =
            formatService.formatDegrees(desiredAzimuth.value, replace360 = true)
        binding.arrowLeft.visibility =
            if (!azimuthAligned && azimuthDiff < 0) View.VISIBLE else View.INVISIBLE
        binding.arrowRight.visibility =
            if (!azimuthAligned && azimuthDiff > 0) View.VISIBLE else View.INVISIBLE

        val euler = orientation.orientation.toEuler()
        val altitudeDiff = solarPosition.tilt + euler.pitch
        val altitudeAligned = altitudeDiff.absoluteValue < ALTITUDE_THRESHOLD
        binding.altitudeComplete.visibility = if (altitudeAligned) View.VISIBLE else View.INVISIBLE
        binding.currentAltitude.text = formatService.formatDegrees(-euler.pitch)
        binding.desiredAltitude.text = formatService.formatDegrees(solarPosition.tilt)
        binding.arrowUp.visibility =
            if (!altitudeAligned && altitudeDiff > 0) View.VISIBLE else View.INVISIBLE
        binding.arrowDown.visibility =
            if (!altitudeAligned && altitudeDiff < 0) View.VISIBLE else View.INVISIBLE

        val energy = solarPanelService.getSolarEnergy(
            gps.location,
            SolarPanelPosition(-euler.pitch, compass.bearing.inverse())
        )
        binding.energy.text =
            getString(R.string.up_to_amount, formatService.formatSolarEnergy(energy))
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
            button.setTextColor(Resources.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(Resources.androidBackgroundColorSecondary(button.context))
        }
    }


    companion object {
        private const val AZIMUTH_THRESHOLD = 5
        private const val ALTITUDE_THRESHOLD = 5
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolSolarPanelBinding {
        return FragmentToolSolarPanelBinding.inflate(layoutInflater, container, false)
    }

}