package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.camera.view.PreviewView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.orientation.CustomRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.views.list.ListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCelestialNavigationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.createGrayscaleThresholdMatrix
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.formatEnumName
import com.kylecorry.trail_sense.shared.fromColorTemperature
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider.Companion.ACCELEROMETER_LOW_PASS
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider.Companion.MAGNETOMETER_LOW_PASS
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.StandardDeviationStarFinder
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime


class CelestialNavigationFragment : BoundFragment<FragmentCelestialNavigationBinding>() {

    private var stars by state(listOf<StarReading>())
    private var location by state<Coordinate?>(null)
    private var approximateLocation by state<Coordinate?>(null)
    private var calculating by state(false)
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val correctUsingCamera = true

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
    ): FragmentCelestialNavigationBinding {
        return FragmentCelestialNavigationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        binding.camera.setShowTorch(false)
        binding.camera.setExposureCompensation(1f)
        binding.camera.setFocus(1f)
        val threshold = Colors.createGrayscaleThresholdMatrix(170)
        val thresholdFilter = ColorMatrixColorFilter(threshold)
        binding.camera.setPreviewColorFilter(thresholdFilter)
        binding.arView.bind(binding.camera)
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        binding.arView.decimalPlaces = 2
        binding.arView.reticleDiameter = Resources.dp(requireContext(), 8f)
        binding.arView.setLayers(listOf(gridLayer))

        chooseApproximateLocation()

        binding.celestialNavigationTitle.rightButton.setOnClickListener {
            stars = emptyList()
            location = null
            chooseApproximateLocation()
        }

        binding.celestialNavigationTitle.leftButton.setOnClickListener {
            showStarList()
        }

        binding.celestialNavigationTitle.title.setOnLongClickListener {
            location?.let {
                Share.shareLocation(this, it)
            }
            true
        }

        binding.celestialNavigationTitle.subtitle.setOnClickListener {
            // TODO: Let the user delete readings
            CustomUiUtils.showList(
                requireContext(),
                getString(R.string.stars),
                stars.map {
                    ListItem(
                        it.star.ordinal.toLong(),
                        formatEnumName(it.star.name),
                        formatAzimuthAndAltitude(Bearing(it.azimuth ?: 0f), it.altitude),
                        icon = getStarIcon(it.star),
                    )
                }
            )
        }

        binding.recordBtn.setOnClickListener {
            recordStar()
        }
    }


    override fun onUpdate() {
        super.onUpdate()
        effect2(stars) {
            binding.celestialNavigationTitle.subtitle.text = resources.getQuantityString(
                R.plurals.star_count,
                stars.size,
                stars.size
            )
        }

        effect2(location, calculating) {
            binding.celestialNavigationTitle.title.text = if (calculating) {
                getString(R.string.loading)
            } else {
                location?.let {
                    formatter.formatLocation(it)
                } ?: getString(R.string.unknown)
            }
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

    private fun chooseApproximateLocation() {
        // TODO: Add a manual option
        Pickers.item(
            requireContext(),
            getString(R.string.approximate_location),
            listOf(getString(R.string.last_known_gps_location), getString(R.string.timezone)),
            cancelText = null,
            defaultSelectedIndex = 0,
        ) {
            approximateLocation = when (it) {
                1 -> {
                    null
                }

                else -> {
                    locationSubsystem.location
                }
            }
        }
    }

    private fun recordStar() {
        var inclination = binding.arView.inclination
        // TODO: Maybe set true north to false and calculate using a location suggested by the user
        var azimuth = Bearing.getBearing(binding.arView.azimuth)
        inBackground {
            val job = launch {
                if (!correctUsingCamera) {
                    return@launch
                }

                val image = binding.camera.previewImage

                if (image != null) {
                    val starPixels = onDefault { StandardDeviationStarFinder().findStars(image) }
                    if (starPixels.isEmpty()) {
                        return@launch
                    }

                    val nearestToCenter = starPixels.minByOrNull {
                        val x = it.x
                        val y = it.y
                        square(x - image.width / 2) + square(y - image.height / 2)
                    } ?: return@launch
                    val arPoint = binding.arView.toCoordinate(
                        nearestToCenter,
                        isClippedToScreen = true,
                        azimuthOverride = azimuth,
                        inclinationOverride = inclination
                    )
                    azimuth = arPoint.bearing
                    inclination = arPoint.elevation
                }
            }

            Alerts.withCancelableLoading(
                requireContext(),
                getString(R.string.loading),
                onCancel = { job.cancel() }) {
                job.join()
            }

            showStarList(true) { star ->
                toast(formatEnumName(star.name))
                stars = stars + StarReading(
                    star,
                    inclination,
                    azimuth,
                    ZonedDateTime.now()
                )
                inBackground {
                    calculating = true
                    location =
                        onDefault { Astronomy.getLocationFromStars(stars, approximateLocation) }
                    calculating = false
                }
            }
        }
    }

    private fun showStarList(onlyUnselected: Boolean = false, onClick: ((Star) -> Unit)? = null) {
        val allStars = getNearestStars().filter { (star, _, _) ->
            !onlyUnselected || stars.none { it.star == star }
        }

        var dialog: AlertDialog? = null

        val starItems = allStars.map { (star, starAltitude, starAzimuth) ->
            ListItem(
                star.ordinal.toLong(),
                formatEnumName(star.name),
                formatAzimuthAndAltitude(starAzimuth, starAltitude),
                icon = getStarIcon(star),
                action = {
                    dialog?.dismiss()
                    onClick?.invoke(star)
                }
            )
        }
        dialog = CustomUiUtils.showList(
            requireContext(),
            if (onClick != null) getString(R.string.select_star) else getString(R.string.nearest_stars),
            starItems,
            okText = if (onClick != null) {
                getString(android.R.string.cancel)
            } else {
                getString(android.R.string.ok)
            }
        )
    }

    private fun getNearestStars(): List<Triple<Star, Float, Bearing>> {
        val azimuth = binding.arView.azimuth
        val inclination = binding.arView.inclination

        val lookupLocation =
            approximateLocation ?: Time.getLocationFromTimeZone(ZoneId.systemDefault())

        return Star.entries.map {
            Triple(
                it,
                Astronomy.getStarAltitude(it, ZonedDateTime.now(), lookupLocation, true),
                Astronomy.getStarAzimuth(it, ZonedDateTime.now(), lookupLocation)
            )
        }.sortedBy {
            val addition = if (it.second < -5) 100000f else 0f
            square(inclination - it.second) + square(deltaAngle(azimuth, it.third.value)) + addition
        }
    }

    private fun formatAzimuthAndAltitude(azimuth: Bearing, altitude: Float): String {
        val azimuthText = formatter.formatDegrees(azimuth.value, replace360 = true)
            .padStart(4, ' ')
        val directionText = formatter.formatDirection(azimuth.direction)
            .padStart(2, ' ')
        return "$azimuthText   $directionText\n${formatter.formatDegrees(altitude)}"
    }

    private fun getStarIcon(star: Star): ListIcon {
        return ResourceListIcon(
            R.drawable.bubble,
            Colors.fromColorTemperature(Astronomy.getColorTemperature(star)),
            foregroundSize = map(
                -star.magnitude,
                -2f,
                1.5f,
                10f,
                24f,
                true
            )
        )
    }
}