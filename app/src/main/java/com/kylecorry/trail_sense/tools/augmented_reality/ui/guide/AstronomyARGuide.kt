package com.kylecorry.trail_sense.tools.augmented_reality.ui.guide

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewArAstronomyGuideBinding
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARAstronomyLayer
import java.time.LocalDate
import java.time.ZonedDateTime

class AstronomyARGuide(
    private val astronomyLayer: ARAstronomyLayer,
    private val initialTimeOverride: ZonedDateTime?,
    private val onCancel: () -> Unit
) : ARGuide {

    private var objectToTrack = AstronomyObject.Sun
    private val astro = AstronomyService()
    private var arView: AugmentedRealityView? = null
    private var binding: ViewArAstronomyGuideBinding? = null

    private var timeOverride: ZonedDateTime? = null

    fun setDate(date: LocalDate) {
        binding?.arGuideDatePicker?.date = date
    }

    private val timer = CoroutineTimer {
        val arView = arView ?: return@CoroutineTimer
        val time = timeOverride ?: ZonedDateTime.now()

        // Clear the guide if the date is not today
        if (timeOverride != null && time.toLocalDate() != LocalDate.now()) {
            arView.clearGuide()
            return@CoroutineTimer
        }

        val destination = when (objectToTrack) {
            AstronomyObject.Sun -> {
                val azimuth = astro.getSunAzimuth(arView.location, time).value
                val altitude = astro.getSunAltitude(arView.location, time)
                SphericalARPoint(azimuth, altitude, angularDiameter = 2f)
            }

            AstronomyObject.Moon -> {
                val azimuth = astro.getMoonAzimuth(arView.location, time).value
                val altitude = astro.getMoonAltitude(arView.location, time)
                SphericalARPoint(azimuth, altitude, angularDiameter = 2f)
            }
        }

        arView.guideTo(destination) {
            // Do nothing when reached
        }
    }

    private val options = listOf(
        AstronomyObject.Sun,
        AstronomyObject.Moon
    )

    override fun start(arView: AugmentedRealityView, panel: FrameLayout) {
        panel.removeAllViews()
        // Inflate the binding into the panel
        val layoutInflater = LayoutInflater.from(panel.context)
        binding = ViewArAstronomyGuideBinding.inflate(layoutInflater, panel, true)
        this.arView = arView

        binding?.arGuideCancel?.setOnClickListener {
            onCancel()
        }

        // Configure the dropdown
        binding?.arGuideSpinner?.setHint(panel.context.getString(R.string.locate))
        binding?.arGuideSpinner?.setItems(options.map { getOptionName(panel.context, it) })
        binding?.arGuideSpinner?.setSelection(options.indexOf(objectToTrack))
        binding?.arGuideSpinner?.setOnItemSelectedListener { index ->
            objectToTrack = options[index ?: 0]
            updateTrackingIcon()
            startTimer()
        }

        // Configure the date picker
        val actualTimeOverride = initialTimeOverride?.let {
            if (it.toLocalDate() == LocalDate.now()) {
                null
            } else {
                it
            }
        }

        timeOverride = actualTimeOverride
        astronomyLayer.timeOverride = actualTimeOverride
        binding?.arGuideDatePicker?.date = actualTimeOverride?.toLocalDate() ?: LocalDate.now()
        binding?.arGuideDatePicker?.setOnDateChangeListener {
            timeOverride = if (it == LocalDate.now()) {
                null
            } else {
                it.atTime(12, 0).toZonedDateTime()
            }
            astronomyLayer.timeOverride = timeOverride
            startTimer()
        }

        updateTrackingIcon()


        startTimer()
    }

    private fun updateTrackingIcon() {
        binding?.arGuideIcon?.setImageResource(getOptionIcon(objectToTrack, ZonedDateTime.now()))
        binding?.arGuideIcon?.rotation = getOptionIconRotation(objectToTrack, ZonedDateTime.now())
    }

    override fun stop(arView: AugmentedRealityView, panel: FrameLayout) {
        this.arView = null
        timer.stop()
        panel.removeAllViews()
        binding = null
        arView.clearGuide()
        astronomyLayer.timeOverride = null
    }

    private fun startTimer() {
        timer.interval(1000)
    }

    private fun getOptionName(context: Context, option: AstronomyObject): String {
        return when (option) {
            AstronomyObject.Sun -> context.getString(R.string.sun)
            AstronomyObject.Moon -> context.getString(R.string.moon)
        }
    }

    private fun getOptionIcon(option: AstronomyObject, time: ZonedDateTime): Int {
        return when (option) {
            AstronomyObject.Sun -> R.drawable.ic_sun
            AstronomyObject.Moon -> {
                val phase = Astronomy.getMoonPhase(time).phase
                MoonPhaseImageMapper().getPhaseImage(phase)
            }
        }
    }

    private fun getOptionIconRotation(option: AstronomyObject, time: ZonedDateTime): Float {
        return when (option) {
            AstronomyObject.Sun -> 0f
            AstronomyObject.Moon -> {
                astro.getMoonTilt(arView?.location ?: return 0f, time)
            }
        }
    }


    private enum class AstronomyObject {
        Sun,
        Moon
    }

}