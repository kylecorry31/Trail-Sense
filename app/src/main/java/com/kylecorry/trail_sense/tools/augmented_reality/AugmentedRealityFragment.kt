package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.databinding.FragmentAugmentedRealityBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.hypot

// TODO: Support arguments for default layer visibility (ex. coming from astronomy, enable only sun/moon)
class AugmentedRealityFragment : BoundFragment<FragmentAugmentedRealityBinding>() {

    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(requireContext())
    }

    private var lastSize: Size? = null

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private val beaconLayer by lazy {
        ARBeaconLayer(
            Distance.meters(userPrefs.navigation.maxBeaconDistance),
            onFocus = this::onBeaconFocused
        ) {
            if (navigator.getDestinationId() != it.id) {
                navigator.navigateTo(it)
            } else {
                navigator.cancelNavigation()
            }
            true
        }
    }

    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    private val sunLayer = ARMarkerLayer()
    private val moonLayer = ARMarkerLayer()
    private val horizonLayer = ARHorizonLayer()
    private val northLayer = ARNorthLayer()

    private var isCameraEnabled = true

    private val fovRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.Default)

    // TODO: Draw an indicator around the focused marker

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeFlow(beaconRepo.getBeacons()) {
            beaconLayer.setBeacons(it)
        }

        observeFlow(navigator.destination) {
            if (it == null) {
                binding.arView.clearGuide()
            } else {
                binding.arView.guideTo(ARPosition.geographic(it.coordinate, it.elevation)) {
                    // Do nothing when reached
                }
            }
            beaconLayer.destination = it
        }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.setShowTorch(false)

        binding.arView.setLayers(listOf(northLayer, horizonLayer, sunLayer, moonLayer, beaconLayer))

        binding.cameraToggle.setOnClickListener {
            if (isCameraEnabled) {
                stopCamera()
            } else {
                startCamera()
            }
        }

        scheduleUpdates(INTERVAL_1_FPS)
    }

    override fun onResume() {
        super.onResume()

        binding.arView.start()
        isCameraEnabled = Camera.isAvailable(requireContext())
        binding.cameraToggle.isVisible = isCameraEnabled
        if (isCameraEnabled) {
            startCamera()
        } else {
            stopCamera()
        }
        updateAstronomyLayers()
    }

    // TODO: Move this to the AR view
    private fun startCamera() {
        isCameraEnabled = true
        binding.cameraToggle.setImageResource(R.drawable.ic_camera)
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        requestCamera {
            if (it) {
                binding.camera.isVisible = true
                binding.camera.start(
                    readFrames = false, shouldStabilizePreview = false
                )
            } else {
                binding.camera.isInvisible = true
                isCameraEnabled = false
                binding.cameraToggle.setImageResource(R.drawable.ic_camera_off)
                binding.arView.backgroundFillColor = Color.BLACK
                alertNoCameraPermission()
            }
        }
    }

    private fun stopCamera() {
        binding.cameraToggle.setImageResource(R.drawable.ic_camera_off)
        isCameraEnabled = false
        binding.arView.backgroundFillColor = Color.BLACK
        binding.camera.stop()
        binding.camera.isInvisible = true
    }

    override fun onPause() {
        super.onPause()
        binding.camera.stop()
        binding.arView.stop()
        fovRunner.cancel()
    }

    override fun onUpdate() {
        super.onUpdate()

        inBackground {
            fovRunner.enqueue {
                if (!isBound) {
                    return@enqueue
                }

                val fov = binding.camera.fov

                onMain {
                    binding.arView.fov = com.kylecorry.sol.math.geometry.Size(fov.first, fov.second)

                    // Set the arView size to be the camera preview size
                    val size = binding.camera.getPreviewSize()
                    if (size != lastSize) {
                        lastSize = size
                        if (binding.arView.layoutParams == null) {
                            binding.arView.layoutParams =
                                FrameLayout.LayoutParams(size.width, size.height)
                        } else {
                            binding.arView.updateLayoutParams {
                                width = size.width
                                height = size.height
                            }
                        }
                    }
                }
            }
        }

    }

    private fun updateAstronomyLayers() {
        // TODO: Extract this population
        inBackground {
            // TODO: Show icons / render path rather than circles
            onDefault {
                val astro = AstronomyService()
                val locationSubsystem = LocationSubsystem.getInstance(requireContext())
                val location = locationSubsystem.location

                val moonBeforePathObject = CircleCanvasObject(
                    Color.WHITE,
                    opacity = 20
                )

                val moonAfterPathObject = CircleCanvasObject(
                    Color.WHITE,
                    opacity = 127
                )

                val sunBeforePathObject = CircleCanvasObject(
                    AppColor.Yellow.color,
                    opacity = 20
                )

                val sunAfterPathObject = CircleCanvasObject(
                    AppColor.Yellow.color,
                    opacity = 127
                )

                val moonPositions = Time.getReadings(
                    LocalDate.now(),
                    ZoneId.systemDefault(),
                    Duration.ofMinutes(15)
                ) {
                    val obj = if (it.isBefore(ZonedDateTime.now())) {
                        moonBeforePathObject
                    } else {
                        moonAfterPathObject
                    }

                    ARMarkerImpl.horizon(
                        AugmentedRealityView.HorizonCoordinate(
                            astro.getMoonAzimuth(location, it).value,
                            astro.getMoonAltitude(location, it),
                            true
                        ),
                        1f,
                        obj,
                        onFocusedFn = {
                            binding.arView.focusText =
                                getString(R.string.moon) + "\n" + formatter.formatRelativeDateTime(
                                    it,
                                    includeSeconds = false
                                )
                            true
                        }
                    )
                }.map { it.value }

                val sunPositions = Time.getReadings(
                    LocalDate.now(), ZoneId.systemDefault(), Duration.ofMinutes(15)
                ) {
                    val obj = if (it.isBefore(ZonedDateTime.now())) {
                        sunBeforePathObject
                    } else {
                        sunAfterPathObject
                    }

                    ARMarkerImpl.horizon(
                        AugmentedRealityView.HorizonCoordinate(
                            astro.getSunAzimuth(location, it).value,
                            astro.getSunAltitude(location, it),
                            true
                        ),
                        1f,
                        obj,
                        onFocusedFn = {
                            binding.arView.focusText =
                                getString(R.string.sun) + "\n" + formatter.formatRelativeDateTime(
                                    it,
                                    includeSeconds = false
                                )
                            true
                        }
                    )
                }.map { it.value }

                val moonAltitude = astro.getMoonAltitude(location)
                val moonAzimuth = astro.getMoonAzimuth(location).value

                val sunAltitude = astro.getSunAltitude(location)
                val sunAzimuth = astro.getSunAzimuth(location).value

                val phase = astro.getMoonPhase(LocalDate.now())
                val moonIconId = MoonPhaseImageMapper().getPhaseImage(phase.phase)
                val moonIcon = Resources.drawable(requireContext(), moonIconId)
                val moonImageSize = Resources.dp(requireContext(), 24f).toInt()
                val moonBitmap = moonIcon?.toBitmapOrNull(moonImageSize, moonImageSize)

                val moon = ARMarkerImpl.horizon(
                    AugmentedRealityView.HorizonCoordinate(
                        moonAzimuth,
                        moonAltitude,
                        true
                    ),
                    2f,
                    moonBitmap?.let { BitmapCanvasObject(moonBitmap) }
                        ?: CircleCanvasObject(Color.WHITE),
                    onFocusedFn = {
                        // TODO: Display moon phase
                        binding.arView.focusText = getString(R.string.moon)
                        true
                    }
                )

                val sun = ARMarkerImpl.horizon(
                    AugmentedRealityView.HorizonCoordinate(
                        sunAzimuth,
                        sunAltitude,
                        true
                    ),
                    2f,
                    CircleCanvasObject(AppColor.Yellow.color),
                    onFocusedFn = {
                        binding.arView.focusText = getString(R.string.sun)
                        true
                    }
                )

                sunLayer.setMarkers(sunPositions + sun)
                moonLayer.setMarkers(moonPositions + moon)
            }
        }
    }

    private fun onBeaconFocused(beacon: Beacon): Boolean {
        val distance = hypot(
            binding.arView.location.distanceTo(beacon.coordinate),
            (beacon.elevation ?: binding.arView.altitude) - binding.arView.altitude
        )
        val userDistance = Distance.meters(distance).convertTo(userPrefs.baseDistanceUnits)
            .toRelativeDistance()
        val formattedDistance = formatter.formatDistance(
            userDistance,
            Units.getDecimalPlaces(userDistance.units),
            strict = false
        )
        binding.arView.focusText = beacon.name + "\n" + formattedDistance
        return true
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentAugmentedRealityBinding {
        return FragmentAugmentedRealityBinding.inflate(layoutInflater, container, false)
    }
}