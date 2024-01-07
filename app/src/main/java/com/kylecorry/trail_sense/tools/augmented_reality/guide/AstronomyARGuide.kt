package com.kylecorry.trail_sense.tools.augmented_reality.guide

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.LayoutInflaterCompat
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.databinding.ViewArAstronomyGuideBinding
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.position.SphericalARPoint
import java.time.ZonedDateTime

class AstronomyARGuide(private val onCancel: () -> Unit) : ARGuide {

    private var objectToTrack = AstronomyObject.Sun
    private val astro = AstronomyService()
    private var arView: AugmentedRealityView? = null
    private var binding: ViewArAstronomyGuideBinding? = null
    private val timer = CoroutineTimer {
        val arView = arView ?: return@CoroutineTimer
        // TODO: Make time configurable
        val time = ZonedDateTime.now()

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

        updateTrackingIcon()


        startTimer()
    }

    private fun updateTrackingIcon() {
        binding?.arGuideIcon?.setImageResource(getOptionIcon(objectToTrack, ZonedDateTime.now()))
    }

    override fun stop(arView: AugmentedRealityView, panel: FrameLayout) {
        this.arView = null
        timer.stop()
        panel.removeAllViews()
        binding = null
        arView.clearGuide()
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


    private enum class AstronomyObject {
        Sun,
        Moon
    }

}