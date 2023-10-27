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
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.sense.clinometer.CameraClinometer
import com.kylecorry.andromeda.sense.clinometer.SideClinometer
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.time.Time
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.atan2

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private val sensors by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensors.getCompass() }
    private val inclinometer by lazy { CameraClinometer(requireContext()) }
    private val sideInclinometer by lazy { SideClinometer(requireContext()) }
    private val gps by lazy { sensors.getGPS(frequency = Duration.ofMillis(200)) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            userPrefs,
            gps
        )
    }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(requireContext())
    }

    private var lastSize: Size? = null

    private var beacons = listOf<Beacon>()
    private var astroPoints = listOf<AugmentedRealityView.Point>()
    private var beaconPoints = listOf<AugmentedRealityView.Point>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observe(compass) {
            binding.linearCompass.azimuth = compass.bearing
            binding.arView.azimuth = compass.rawBearing
        }

        observe(inclinometer) {
            binding.arView.inclination = inclinometer.incline
        }

        observe(sideInclinometer) {
            binding.arView.sideInclination = sideInclinometer.angle - 90
        }

        observe(gps) {
            updateNearbyBeacons()
        }

        observeFlow(beaconRepo.getBeacons()) {
            beacons = it
            updateNearbyBeacons()
        }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)

        binding.linearCompass.showAzimuthArrow = false

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
                    val alpha = if (it.isBefore(ZonedDateTime.now())) {
                        20
                    } else {
                        127
                    }
                    AugmentedRealityView.Point(
                        AugmentedRealityView.HorizonCoordinate(
                            astro.getMoonAzimuth(location, it).value,
                            astro.getMoonAltitude(location, it)
                        ),
                        1f,
                        Color.WHITE.withAlpha(alpha)
                    )
                }.map { it.value }

                val sunPositions = Time.getReadings(
                    LocalDate.now(),
                    ZoneId.systemDefault(),
                    Duration.ofMinutes(15)
                ) {
                    val alpha = if (it.isBefore(ZonedDateTime.now())) {
                        20
                    } else {
                        127
                    }
                    AugmentedRealityView.Point(
                        AugmentedRealityView.HorizonCoordinate(
                            astro.getSunAzimuth(location, it).value,
                            astro.getSunAltitude(location, it)
                        ),
                        1f,
                        AppColor.Yellow.color.withAlpha(alpha)
                    )
                }.map { it.value }

                val moonAltitude = astro.getMoonAltitude(location)
                val moonAzimuth = astro.getMoonAzimuth(location).value

                val sunAltitude = astro.getSunAltitude(location)
                val sunAzimuth = astro.getSunAzimuth(location).value

                astroPoints = moonPositions + sunPositions +
                        listOf(
                            AugmentedRealityView.Point(
                                AugmentedRealityView.HorizonCoordinate(
                                    moonAzimuth,
                                    moonAltitude
                                ), 2f, Color.WHITE
                            ),
                            AugmentedRealityView.Point(
                                AugmentedRealityView.HorizonCoordinate(
                                    sunAzimuth,
                                    sunAltitude
                                ),
                                2f,
                                AppColor.Yellow.color
                            )
                        )

                updatePoints()
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
        binding.linearCompass.range = fov.first

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

    private fun updateNearbyBeacons() {
        inBackground {
            onIO {
                val navigationService = NavigationService()
                val nearbyCount = userPrefs.navigation.numberOfVisibleBeacons
                val nearbyDistance = userPrefs.navigation.maxBeaconDistance
                val nearby = navigationService.getNearbyBeacons(
                    gps.location,
                    beacons,
                    nearbyCount,
                    8f,
                    nearbyDistance
                )

                beaconPoints = nearby.map {
                    val bearing = gps.location.bearingTo(it.coordinate).value
                    val distance = gps.location.distanceTo(it.coordinate)
                    // Calculate the elevation angle without calling a method
                    val elevation = if (it.elevation == null) {
                        0f
                    } else {
                        atan2((it.elevation - gps.altitude), distance).toDegrees()
                    }
                    val scaledSize = (360f / distance).coerceIn(1f, 5f)
                    AugmentedRealityView.Point(
                        AugmentedRealityView.HorizonCoordinate(
                            bearing,
                            elevation
                        ),
                        scaledSize,
                        it.color
                    )
                }.sortedBy { it.size }

                updatePoints()
            }
        }
    }

    private fun updatePoints() {
        binding.arView.points = astroPoints + beaconPoints
    }
}