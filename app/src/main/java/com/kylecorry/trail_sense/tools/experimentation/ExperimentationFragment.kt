package com.kylecorry.trail_sense.tools.experimentation

import android.graphics.Color
import android.os.Bundle
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.orientation.CustomRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.formatEnumName
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider.Companion.ACCELEROMETER_LOW_PASS
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider.Companion.MAGNETOMETER_LOW_PASS
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import java.time.ZonedDateTime

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private var stars by state(listOf<StarReading>())
    private var location by state<Coordinate?>(null)
    private var calculating by state(false)

    private val orientationSensor by lazy {
        val magnetometer =
            LowPassMagnetometer(
                requireContext(),
                SensorService.MOTION_SENSOR_DELAY,
                MAGNETOMETER_LOW_PASS
            )
        val accelerometer =
            LowPassAccelerometer(
                requireContext(),
                SensorService.MOTION_SENSOR_DELAY,
                ACCELEROMETER_LOW_PASS
            )
        val gyro = Gyroscope(requireContext(), SensorService.MOTION_SENSOR_DELAY)

        CustomRotationSensor(
            magnetometer, accelerometer, gyro,
            gyroWeight = 1f,
            validMagnetometerMagnitudes = Range(20f, 65f),
            validAccelerometerMagnitudes = Range(4f, 20f),
            onlyUseMagnetometerQuality = true
        )
    }

    private val gridLayer by lazy {
        ARGridLayer(
            30,
            northColor = Resources.getCardinalDirectionColor(requireContext()),
            horizonColor = Color.WHITE,
            labelColor = Color.WHITE,
            color = Color.WHITE.withAlpha(100),
            useTrueNorth = true
        )
    }

    private val locationSubsystem by lazy { LocationSubsystem.getInstance(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        binding.camera.setShowTorch(false)
        binding.camera.setExposureCompensation(1f)
        binding.arView.bind(binding.camera)
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        binding.arView.decimalPlaces = 2
        binding.arView.reticleDiameter = Resources.dp(requireContext(), 8f)
        binding.arView.setLayers(listOf(gridLayer))

        binding.recordBtn.setOnClickListener {
            val inclination = binding.arView.inclination
            // TODO: Maybe set true north to false and calculate using a location suggested by the user
            val azimuth = Bearing.getBearing(binding.arView.azimuth)
            // TODO: Let the user specify the last known location (choose source: last known GPS location, timezone, manual)
            val approximateLocation: Coordinate? = null //locationSubsystem.location
            // TODO: Get preview image and find the offset of the star from the center of the image to get an X (azimuth) and Y (inclination) offset
            // TODO: This will be the nearest cluster of white pixels from the center of the image
//            val image = binding.camera.previewImage
//            val fov = binding.camera.fov
            inBackground {
                val allStars = Star.entries.sortedBy { it.name }
                val starIdx = CoroutinePickers.item(
                    requireContext(),
                    "Star",
                    allStars.map { formatEnumName(it.name) })
                if (starIdx != null) {
                    stars = stars + StarReading(
                        allStars[starIdx],
                        inclination,
                        azimuth,
                        ZonedDateTime.now()
                    )
                    calculating = true
                    location =
                        onDefault { Astronomy.getLocationFromStars(stars, approximateLocation) }
                    calculating = false
                }
            }
        }
    }


    override fun onUpdate() {
        super.onUpdate()
        effect2(stars) {
            binding.text.text =
                stars.joinToString("\n") { "${it.star.name}: ${it.altitude}, ${it.azimuth}" }
        }

        effect2(location, calculating) {
            binding.location.text =
                if (calculating) getString(R.string.loading) else location?.toString() ?: getString(
                    R.string.unknown
                )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.arView.start(useGPS = false, customOrientationSensor = orientationSensor)
        binding.camera.start(
            readFrames = false,
            shouldStabilizePreview = false
        )
    }

    override fun onPause() {
        super.onPause()
        binding.arView.stop()
        binding.camera.stop()
    }

}