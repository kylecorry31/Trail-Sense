package com.kylecorry.trail_sense.tools.experimentation

import android.graphics.Color
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.sense.clinometer.CameraClinometer
import com.kylecorry.andromeda.sense.clinometer.SideClinometer
import com.kylecorry.sol.time.Time
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private val sensors by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensors.getCompass() }
    private val inclinometer by lazy { CameraClinometer(requireContext()) }
    private val sideInclinometer by lazy { SideClinometer(requireContext()) }
    private val gps by lazy { sensors.getGPS() }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            userPrefs,
            gps
        )
    }

    private var lastSize: Size? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observe(compass) {
            binding.arView.azimuth = compass.rawBearing
        }

        observe(inclinometer) {
            binding.arView.inclination = inclinometer.incline
        }

        observe(sideInclinometer) {
            binding.arView.sideInclination = sideInclinometer.angle - 90
        }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)

        scheduleUpdates(INTERVAL_1_FPS)
    }

    override fun onResume() {
        super.onResume()
        binding.camera.start(
            readFrames = false,
            shouldStabilizePreview = false
        )

        inBackground {
            onDefault {
                val astro = AstronomyService()
                val location = LocationSubsystem.getInstance(requireContext()).location
                compass.declination = declinationProvider.getDeclination()

                val moonPositions = Time.getReadings(
                    LocalDate.now(),
                    ZoneId.systemDefault(),
                    Duration.ofMinutes(15)
                ) {
                    AugmentedRealityView.Point(
                        astro.getMoonAzimuth(location, it).value,
                        astro.getMoonAltitude(location, it),
                        1f,
                        Color.WHITE.withAlpha(127)
                    )
                }.map { it.value }

                val sunPositions = Time.getReadings(
                    LocalDate.now(),
                    ZoneId.systemDefault(),
                    Duration.ofMinutes(15)
                ) {
                    AugmentedRealityView.Point(
                        astro.getSunAzimuth(location, it).value,
                        astro.getSunAltitude(location, it),
                        1f,
                        AppColor.Yellow.color.withAlpha(127)
                    )
                }.map { it.value }

                val moonAltitude = astro.getMoonAltitude(location)
                val moonAzimuth = astro.getMoonAzimuth(location).value

                val sunAltitude = astro.getSunAltitude(location)
                val sunAzimuth = astro.getSunAzimuth(location).value

                binding.arView.points = moonPositions + sunPositions +
                        listOf(
                            AugmentedRealityView.Point(moonAzimuth, moonAltitude, 2f, Color.WHITE),
                            AugmentedRealityView.Point(
                                sunAzimuth,
                                sunAltitude,
                                2f,
                                AppColor.Yellow.color
                            )
                        )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.camera.stop()
    }

    override fun onUpdate() {
        super.onUpdate()
        val fov = binding.camera.fov
        binding.arView.fov = com.kylecorry.sol.math.geometry.Size(fov.first, fov.second)

        // Set the arView size to be the camera preview size
        val size = binding.camera.getPreviewSize()
        if (size != lastSize) {
            lastSize = size
            if (binding.arView.layoutParams == null) {
                binding.arView.layoutParams = FrameLayout.LayoutParams(size.width, size.height)
            } else {
                binding.arView.layoutParams = binding.arView.layoutParams.apply {
                    width = size.width
                    height = size.height
                }
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }
}