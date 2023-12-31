package com.kylecorry.trail_sense.tools.augmented_reality.guide

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.position.SphericalARPoint
import java.time.ZonedDateTime

class AstronomyARGuide : ARGuide {

    private var objectToTrack = AstronomyObject.Sun
    private val astro = AstronomyService()
    private var arView: AugmentedRealityView? = null
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

    override fun start(arView: AugmentedRealityView) {
        this.arView = arView
        timer.interval(1000)
    }

    override fun stop(arView: AugmentedRealityView) {
        this.arView = null
        timer.stop()
        arView.clearGuide()
    }


    private enum class AstronomyObject {
        Sun,
        Moon
    }

}