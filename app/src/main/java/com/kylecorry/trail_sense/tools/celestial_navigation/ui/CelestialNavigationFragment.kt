package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import com.kylecorry.andromeda.bitmaps.BitmapUtils.average
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.orientation.CustomRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.DetectedStar
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCelestialNavigationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.LayerFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider.Companion.ACCELEROMETER_LOW_PASS
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider.Companion.MAGNETOMETER_LOW_PASS
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARMarkerLayer
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialLocationEstimate
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialLocationEstimator
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.StarFinderFactory
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

class CelestialNavigationFragment : BoundFragment<FragmentCelestialNavigationBinding>() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val locationSubsystem by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val starFinder = StarFinderFactory().getStarFinder()
    private val estimator = CelestialLocationEstimator()
    private val isProcessing = AtomicBoolean(false)
    private var lastFrameTime = 0L
    private var isResumed = false
    private var estimate by state<CelestialLocationEstimate?>(null)
    private var status by state("")
    private var plateLocation: Coordinate? = null

    private val orientationSensor by lazy {
        CustomRotationSensor(
            LowPassMagnetometer(
                requireContext(),
                SensorService.MOTION_SENSOR_DELAY,
                MAGNETOMETER_LOW_PASS
            ),
            LowPassAccelerometer(
                requireContext(),
                SensorService.MOTION_SENSOR_DELAY,
                ACCELEROMETER_LOW_PASS
            ),
            Gyroscope(requireContext(), SensorService.MOTION_SENSOR_DELAY),
            gyroWeight = 1f,
            validMagnetometerMagnitudes = Range(20f, 65f),
            validAccelerometerMagnitudes = Range(4f, 20f),
            onlyUseMagnetometerQuality = true
        )
    }

    private val detectedStarLayer = ARMarkerLayer()
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

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCelestialNavigationBinding {
        return FragmentCelestialNavigationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        status = getString(R.string.celestial_navigation_searching)
        plateLocation = locationSubsystem.location
            ?: Time.getLocationFromTimeZone(ZoneId.systemDefault())

        binding.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        binding.camera.setShowTorch(false)
        binding.camera.setManualExposure(Duration.ofMillis(250), 3200)
        binding.camera.setFocus(1f)
        binding.arView.bind(binding.camera)
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        binding.arView.reticleDiameter = Resources.dp(requireContext(), 8f)
        binding.arView.setLayers(listOf(gridLayer, detectedStarLayer))

        val layerFactory = LayerFactory()
        val mapLayers = listOf(
            BaseMapTileSource.SOURCE_ID,
            MyLocationGeoJsonSource.SOURCE_ID
        ).mapNotNull { Tools.getMapLayerDefinition(requireContext(), it) }
            .map(layerFactory::createLayer)
        binding.map.setLayers(mapLayers)

        binding.celestialNavigationTitle.rightButton.setOnClickListener {
            estimator.clear()
            estimate = null
            plateLocation = locationSubsystem.location
                ?: Time.getLocationFromTimeZone(ZoneId.systemDefault())
            status = getString(R.string.celestial_navigation_searching)
        }
        binding.celestialNavigationTitle.title.setOnLongClickListener {
            estimate?.location?.let { Share.shareLocation(this, it) }
            true
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect("status", status) {
            binding.status.text = status
        }
        effect("estimate", estimate) {
            val current = estimate
            binding.celestialNavigationTitle.title.text = current?.let {
                formatter.formatLocation(it.location)
            } ?: getString(R.string.celestial_navigation)
            binding.celestialNavigationTitle.subtitle.text = current?.let {
                getString(
                    R.string.celestial_navigation_accuracy,
                    formatAccuracy(it.accuracyMeters),
                    it.sampleCount
                )
            } ?: getString(R.string.celestial_navigation_no_fix)

            current?.let { updateMap(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        binding.map.start()
        binding.arView.start(useGPS = false, customOrientationSensor = orientationSensor)
        requestCamera { hasPermission ->
            if (hasPermission && isResumed) {
                startCamera()
            } else if (!hasPermission) {
                alertNoCameraPermission()
            }
        }
    }

    override fun onPause() {
        isResumed = false
        binding.camera.stop()
        binding.arView.stop()
        binding.map.stop()
        super.onPause()
    }

    private fun startCamera() {
        binding.camera.start(
            readFrames = true,
            shouldStabilizePreview = false,
            preferBackCamera = true
        ) { image ->
            val now = SystemClock.elapsedRealtime()
            if (!isResumed || now - lastFrameTime < FRAME_INTERVAL_MS ||
                !isProcessing.compareAndSet(false, true)
            ) {
                image.recycle()
                return@start
            }
            lastFrameTime = now
            val arView = binding.arView
            val rotationMatrix = arView.rotationMatrix.copyOf()
            val previewRect = binding.camera.camera?.getPreviewRect(false)
            if (previewRect == null) {
                isProcessing.set(false)
                image.recycle()
                return@start
            }
            inBackground {
                status = getString(R.string.celestial_navigation_analyzing)
                withContext(Dispatchers.Default) {
                    try {
                        processFrame(
                            image,
                            arView,
                            rotationMatrix,
                            previewRect.left,
                            previewRect.top,
                            previewRect.width(),
                            previewRect.height()
                        )
                    } finally {
                        image.recycle()
                        isProcessing.set(false)
                    }
                }
            }
        }
    }

    private suspend fun processFrame(
        image: Bitmap,
        arView: AugmentedRealityView,
        rotationMatrix: FloatArray,
        previewLeft: Float,
        previewTop: Float,
        previewWidth: Float,
        previewHeight: Float
    ) {
        if (image.average() >= MAX_SKY_BRIGHTNESS) {
            setStatus(R.string.celestial_navigation_too_bright)
            return
        }

        val allStarPixels = starFinder.findStars(image)
        if (allStarPixels.size < MIN_DETECTED_STARS) {
            setStatus(R.string.celestial_navigation_detected_stars, allStarPixels.size)
            return
        }
        val starPixels = allStarPixels.sortedByDescending {
            Color.red(
                image.getPixel(
                    it.x.toInt().coerceIn(0, image.width - 1),
                    it.y.toInt().coerceIn(0, image.height - 1)
                )
            )
        }.take(MAX_PLATE_STARS)

        val scaleX = image.width / previewWidth
        val scaleY = image.height / previewHeight
        val observations = starPixels.map {
            PixelCoordinate(it.x / scaleX + previewLeft, it.y / scaleY + previewTop)
        }.map {
            arView.toCoordinate(it, rotationMatrixOverride = rotationMatrix)
        }.map {
            CelestialObservation(Bearing.from(it.bearing), it.elevation)
        }

        val readingTime = ZonedDateTime.now()
        val detected = Astronomy.plateSolve(
            observations,
            readingTime,
            plateLocation,
            tolerance = 0.1f,
            minMagnitude = MAX_CATALOG_MAGNITUDE
        )
        showDetectedStars(detected)
        if (detected.size < MIN_MATCHED_STARS) {
            setStatus(R.string.celestial_navigation_matched_stars, detected.size, starPixels.size)
            return
        }

        val readings = detected.map {
            StarReading(it.star, it.reading.altitude, it.reading.azimuth.value, readingTime)
        }
        val location = Astronomy.getLocationFromStars(readings, plateLocation)
        if (location == null) {
            setStatus(R.string.celestial_navigation_no_solution)
            return
        }

        val frameAccuracy = getFrameAccuracy(readings, location, detected)
        val update = estimator.add(location, frameAccuracy)
        if (update.accepted) {
            plateLocation = update.estimate.location
            onMain {
                if (isResumed) {
                    estimate = update.estimate
                    status = getString(
                        R.string.celestial_navigation_fix_status,
                        detected.size,
                        update.estimate.sampleCount
                    )
                }
            }
        } else {
            setStatus(R.string.celestial_navigation_outlier)
        }
    }

    private fun getFrameAccuracy(
        readings: List<StarReading>,
        location: Coordinate,
        detected: List<DetectedStar>
    ): Float {
        val alternateLocations = readings.indices.mapNotNull { excludedIndex ->
            Astronomy.getLocationFromStars(
                readings.filterIndexed { index, _ -> index != excludedIndex },
                plateLocation
            )
        }
        val stability = if (alternateLocations.isEmpty()) {
            50_000f
        } else {
            sqrt(alternateLocations.map { location.distanceTo(it).toDouble() }
                .map { it * it }
                .average()).toFloat() * 2
        }
        val confidencePenalty = (1 - detected.map { it.confidence }.average()).toFloat() * 100_000f
        return max(5000f, max(stability, confidencePenalty))
    }

    private suspend fun showDetectedStars(detected: List<DetectedStar>) {
        val markers = detected.map {
            ARMarker(
                SphericalARPoint(
                    it.reading.azimuth.value,
                    it.reading.altitude,
                    angularDiameter = 0.25f
                ),
                CanvasCircle(Color.GREEN.withAlpha(80), Color.GREEN)
            )
        }
        onMain {
            if (isResumed) {
                detectedStarLayer.setMarkers(markers)
            }
        }
    }

    private suspend fun setStatus(resource: Int, vararg args: Any) {
        onMain {
            if (isResumed) {
                status = getString(resource, *args)
            }
        }
    }

    private fun updateMap(current: CelestialLocationEstimate) {
        binding.map.userLocation = current.location
        binding.map.userLocationAccuracy = Distance.meters(current.accuracyMeters)
        binding.map.mapCenter = current.location
        binding.map.doOnLayout {
            val size = minOf(binding.map.width, binding.map.height).coerceAtLeast(1)
            binding.map.resolutionPixels = current.accuracyMeters * 2.5f / size
        }
        binding.map.layerManager.invalidate()
    }

    private fun formatAccuracy(meters: Float): String {
        val distance = if (meters >= 1000) {
            Distance.kilometers(meters / 1000)
        } else {
            Distance.meters(meters)
        }
        return formatter.formatDistance(distance, if (meters >= 10_000) 0 else 1)
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 2000L
        private const val MAX_SKY_BRIGHTNESS = 100f
        private const val MIN_DETECTED_STARS = 6
        private const val MIN_MATCHED_STARS = 4
        private const val MAX_PLATE_STARS = 20
        private const val MAX_CATALOG_MAGNITUDE = 3f
    }
}
