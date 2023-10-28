package com.kylecorry.trail_sense.tools.augmented_reality

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
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.sense.clinometer.CameraClinometer
import com.kylecorry.andromeda.sense.clinometer.SideClinometer
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.databinding.FragmentAugmentedRealityBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.atan2

// TODO: Support arguments for default layer visibility (ex. coming from astronomy, enable only sun/moon)
class AugmentedRealityFragment : BoundFragment<FragmentAugmentedRealityBinding>() {

    private val sensors by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensors.getGPS(frequency = Duration.ofMillis(200)) }
    private val altimeter by lazy { sensors.getAltimeter(gps = gps) }
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(requireContext())
    }

    private var lastSize: Size? = null

    private var beacons = listOf<Beacon>()
    private var astroPoints = listOf<AugmentedRealityView.Point>()
    private var beaconPoints = listOf<AugmentedRealityView.Point>()
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private val compassSyncTimer = CoroutineTimer {
        binding.linearCompass.azimuth = Bearing(binding.arView.azimuth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observe(gps) {
            updateNearbyBeacons()
        }

        // TODO: Show paths
        observeFlow(beaconRepo.getBeacons()) {
            beacons = it
            updateNearbyBeacons()
        }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)

        // TODO: Show azimuth / altitude
        binding.linearCompass.showAzimuthArrow = false

        scheduleUpdates(INTERVAL_1_FPS)
    }

    override fun onResume() {
        super.onResume()

        binding.arView.start()

        // TODO: Allow user to turn camera off
        // TODO: Allow zoom when camera is off
        // TODO: Allow use without sensors
        requestCamera {
            if (it) {
                binding.camera.start(
                    readFrames = false,
                    shouldStabilizePreview = false
                )
            } else {
                alertNoCameraPermission()
            }
        }

        inBackground {
            // TODO: Show icons / render path rather than circles
            onDefault {
                val astro = AstronomyService()
                val location = LocationSubsystem.getInstance(requireContext()).location
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
                            astro.getMoonAltitude(location, it),
                            true
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
                            astro.getSunAltitude(location, it),
                            true
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
                                    moonAltitude,
                                    true
                                ), 2f, Color.WHITE,
                                getString(R.string.moon)
                            ),
                            AugmentedRealityView.Point(
                                AugmentedRealityView.HorizonCoordinate(
                                    sunAzimuth,
                                    sunAltitude,
                                    true
                                ),
                                2f,
                                AppColor.Yellow.color,
                                getString(R.string.sun)
                            )
                        )

                updatePoints()
            }
        }

        compassSyncTimer.interval(INTERVAL_60_FPS)
    }

    override fun onPause() {
        super.onPause()
        binding.camera.stop()
        binding.arView.stop()
        compassSyncTimer.stop()
    }

    override fun onUpdate() {
        super.onUpdate()

        // TODO: Move this to a coroutine (and to the AR view)
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
    ): FragmentAugmentedRealityBinding {
        return FragmentAugmentedRealityBinding.inflate(layoutInflater, container, false)
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
                    // TODO: Remove this logic (already present in AR view)
                    val bearing = gps.location.bearingTo(it.coordinate).value
                    val distance = gps.location.distanceTo(it.coordinate)
                    val elevation = if (it.elevation == null) {
                        0f
                    } else {
                        atan2((it.elevation - altimeter.altitude), distance).toDegrees()
                    }
                    // TODO: Find a better size / move the scaling to augmented reality view
                    val scaledSize = (360f / distance).coerceIn(1f, 5f)
                    val userDistance =
                        Distance.meters(distance).convertTo(userPrefs.baseDistanceUnits)
                            .toRelativeDistance()
                    val formattedDistance = formatter.formatDistance(
                        userDistance,
                        Units.getDecimalPlaces(userDistance.units),
                        strict = false
                    )
                    // TODO: Show icons and names
                    AugmentedRealityView.Point(
                        AugmentedRealityView.HorizonCoordinate(
                            bearing,
                            elevation
                        ),
                        scaledSize,
                        it.color,
                        it.name + "\n" + formattedDistance
                    )
                }.sortedBy { it.size }

                updatePoints()
            }
        }
    }

    private fun updatePoints() {
        // TODO: Allow layers to be toggled
        binding.arView.points = astroPoints + beaconPoints
    }
}