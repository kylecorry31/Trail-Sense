package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.ui.setOnProgressChangeListener
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onMain
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
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARMarkerLayer
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.BrightestPointFinder
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialFixAccuracyEstimator
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialPlateSolver
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialLocationEstimate
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialLocationEstimator
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialLocationSolver
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

class CelestialNavigationFragment : BoundFragment<FragmentCelestialNavigationBinding>() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val locationSubsystem by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val brightestPointFinder = BrightestPointFinder()
    private val plateSolver = CelestialPlateSolver(MAX_CATALOG_MAGNITUDE)
    private val estimator = CelestialLocationEstimator()
    private val accuracyEstimator = CelestialFixAccuracyEstimator()
    private val locationSolver = CelestialLocationSolver()
    private var solveJob: Job? = null
    private val isDetecting = AtomicBoolean(false)
    private val observationVersion = AtomicInteger(0)
    private val observedStars = mutableListOf<CelestialObservation>()
    private var isResumed = false
    private var estimate by state<CelestialLocationEstimate?>(null)
    private var status by state("")
    private var plateLocation: Coordinate? = null

    private val detectedStarLayer = ARMarkerLayer()
    private val observedStarLayer = ARMarkerLayer()
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
        binding.camera.setExposureCompensation(1f)
        binding.camera.setFocus(1f)
        binding.arView.bind(binding.camera)
        binding.arView.alwaysUseInfiniteFocus = true
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        binding.arView.reticleDiameter = Resources.dp(requireContext(), RETICLE_DIAMETER_DP)
        binding.arView.setLayers(listOf(gridLayer, observedStarLayer, detectedStarLayer))

        binding.detectStar.setOnClickListener {
            detectStarAtReticle()
        }
        binding.celestialNavigationTitle.leftButton.isEnabled = false
        binding.celestialNavigationTitle.leftButton.setOnClickListener {
            undoLastStar()
        }
        binding.exposureSlider.setOnProgressChangeListener { progress, _ ->
            binding.camera.setExposureCompensation(progress / 100f)
        }

        val layerFactory = LayerFactory()
        val mapLayers = listOf(
            BaseMapTileSource.SOURCE_ID,
            MyLocationGeoJsonSource.SOURCE_ID
        ).mapNotNull { Tools.getMapLayerDefinition(requireContext(), it) }
            .map(layerFactory::createLayer)
        binding.map.setLayers(mapLayers)

        binding.celestialNavigationTitle.rightButton.setOnClickListener {
            solveJob?.cancel()
            estimator.clear()
            synchronized(observedStars) {
                observedStars.clear()
            }
            observationVersion.incrementAndGet()
            binding.celestialNavigationTitle.leftButton.isEnabled = false
            observedStarLayer.setMarkers(emptyList())
            detectedStarLayer.setMarkers(emptyList())
            binding.arView.resetCalibration()
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
        // A detection in progress is cancelled when the fragment is paused
        endDetection()
        binding.map.start()
        binding.arView.start(useGPS = false)
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
            readFrames = false,
            shouldStabilizePreview = false,
            preferBackCamera = true
        )
    }

    private fun detectStarAtReticle() {
        Log.d(TAG, "Detect button tapped")
        if (!isDetecting.compareAndSet(false, true)) {
            Log.d(TAG, "Ignoring tap because reticle detection is already running")
            return
        }
        val image = binding.camera.previewImage
        if (image == null) {
            Log.d(TAG, "No preview image was available at tap time")
            isDetecting.set(false)
            return
        }
        Log.d(TAG, "Captured ${image.width}x${image.height} preview at tap time")
        val rotationMatrix = binding.arView.rotationMatrix.copyOf()

        binding.detectStar.isEnabled = false
        status = getString(R.string.celestial_navigation_analyzing)
        inBackground {
            var detected = false
            try {
                detected = withContext(Dispatchers.Default) {
                    try {
                        detectStar(image, rotationMatrix)
                    } finally {
                        image.recycle()
                    }
                }
            } finally {
                // This has to happen even if the detection was cancelled or failed, otherwise the
                // button will never work again
                endDetection()
            }
            if (detected) {
                Log.d(TAG, "Reticle detection succeeded; requesting location solve")
                requestLocationSolve()
            } else {
                Log.d(TAG, "Reticle detection did not produce a star")
            }
        }
    }

    private fun endDetection() {
        isDetecting.set(false)
        if (isResumed) {
            binding.detectStar.isEnabled = true
        }
    }

    private suspend fun detectStar(image: Bitmap, rotationMatrix: FloatArray): Boolean {
        val arView = binding.arView
        val scaleX = image.width.toFloat() / arView.width
        val scaleY = image.height.toFloat() / arView.height
        val reticleCenter = PixelCoordinate(arView.width / 2f, arView.height / 2f)
        val imageCenter = PixelCoordinate(
            reticleCenter.x * scaleX,
            reticleCenter.y * scaleY
        )
        val reticleRadius = arView.reticleDiameter / 2f
        val starPixel = brightestPointFinder.find(
            image,
            imageCenter,
            reticleRadius * scaleX,
            reticleRadius * scaleY
        )
        if (starPixel == null) {
            Log.d(TAG, "No brightest point found inside reticle")
            setStatus(R.string.celestial_navigation_observed_stars, getObservedStarCount())
            return false
        }

        val previewPixel = PixelCoordinate(
            starPixel.x / scaleX,
            starPixel.y / scaleY
        )
        val coordinate = arView.toCoordinate(
            previewPixel,
            rotationMatrixOverride = rotationMatrix
        )
        val observedStarCount = synchronized(observedStars) {
            observedStars.add(
                CelestialObservation(Bearing.from(coordinate.bearing), coordinate.elevation)
            )
            observedStars.size
        }
        val version = observationVersion.incrementAndGet()
        Log.d(
            TAG,
            "Detected star $observedStarCount at pixel (${starPixel.x}, ${starPixel.y}), " +
                    "azimuth=${coordinate.bearing}, altitude=${coordinate.elevation}, version=$version"
        )
        onMain {
            if (isResumed) {
                binding.celestialNavigationTitle.leftButton.isEnabled = true
            }
        }
        showObservedStars()
        if (observedStarCount == 1) {
            onMain {
                if (isResumed) {
                    arView.switchToGyro()
                }
            }
        }
        setStatus(R.string.celestial_navigation_observed_stars, observedStarCount)
        return true
    }

    private fun undoLastStar() {
        val remaining = synchronized(observedStars) {
            if (observedStars.isEmpty()) {
                return
            }
            observedStars.removeLast()
            observedStars.size
        }
        val version = observationVersion.incrementAndGet()
        Log.d(TAG, "Undo removed last star; $remaining observations remain, version=$version")

        solveJob?.cancel()
        estimator.clear()
        estimate = null
        plateLocation = locationSubsystem.location
            ?: Time.getLocationFromTimeZone(ZoneId.systemDefault())
        detectedStarLayer.setMarkers(emptyList())
        binding.celestialNavigationTitle.leftButton.isEnabled = remaining > 0
        if (remaining == 0) {
            binding.arView.resetCalibration()
        }
        status = if (remaining == 0) {
            getString(R.string.celestial_navigation_searching)
        } else {
            getString(R.string.celestial_navigation_undo_star)
        }
        inBackground {
            showObservedStars()
        }
        if (remaining >= MIN_MATCHED_STARS) {
            Log.d(TAG, "Undo left enough observations; requesting a new solve")
            requestLocationSolve()
        } else {
            Log.d(TAG, "Undo left fewer than $MIN_MATCHED_STARS observations; not solving")
        }
    }

    private fun requestLocationSolve() {
        val observations = synchronized(observedStars) { observedStars.toList() }
        val version = observationVersion.get()
        Log.d(TAG, "Location solve requested for version=$version with ${observations.size} observations")
        if (observations.size < MIN_MATCHED_STARS) {
            Log.d(TAG, "Skipping solve; only ${observations.size} observations")
            return
        }
        // Only the newest set of observations matters, so don't let solves pile up
        solveJob?.cancel()
        solveJob = inBackground {
            Log.d(TAG, "Starting solve for version=$version")
            withContext(Dispatchers.Default) {
                solveLocation(observations, version)
            }
            Log.d(TAG, "Solve finished for version=$version")
        }
    }

    private suspend fun solveLocation(observations: List<CelestialObservation>, version: Int) {

        val readingTime = ZonedDateTime.now()
        val detected = plateSolver.solve(
            observations,
            readingTime,
            plateLocation ?: Time.getLocationFromTimeZone(readingTime.zone)
        )
        Log.d(TAG, "Plate solver returned ${detected.size}/${observations.size} matches")
        showDetectedStars(detected)
        if (detected.size < MIN_MATCHED_STARS) {
            setStatus(R.string.celestial_navigation_matched_stars, detected.size, observations.size)
            return
        }
        setStatus(R.string.celestial_navigation_solving, detected.size)

        val readings = detected.map {
            StarReading(it.star, it.reading.altitude, it.reading.azimuth.value, readingTime)
        }
        val location = locationSolver.solve(
            readings,
            plateLocation ?: Time.getLocationFromTimeZone(readingTime.zone)
        )
        if (location == null || !isValid(location)) {
            Log.d(TAG, "Matched stars did not produce a location")
            setStatus(R.string.celestial_navigation_no_solution)
            return
        }

        val frameAccuracy = accuracyEstimator.getAccuracy(
            readings,
            location,
            detected.map { it.confidence }.average().toFloat()
        )
        Log.d(TAG, "Solved location $location with an accuracy of $frameAccuracy m")
        if (version != observationVersion.get()) {
            Log.d(TAG, "Discarding stale solve for version=$version")
            return
        }
        val update = estimator.add(location, frameAccuracy)
        if (update.accepted) {
            Log.d(TAG, "Accepted location ${update.estimate.location}")
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
            Log.d(TAG, "Rejected location as an outlier")
            setStatus(R.string.celestial_navigation_outlier)
        }
    }

    private fun isValid(location: Coordinate): Boolean {
        return location.latitude.isFinite() && location.longitude.isFinite()
    }

    private fun getObservedStarCount(): Int {
        return synchronized(observedStars) { observedStars.size }
    }

    private suspend fun showObservedStars() {
        val observations = synchronized(observedStars) { observedStars.toList() }
        val markers = observations.map {
            ARMarker(
                SphericalARPoint(
                    it.azimuth.value,
                    it.altitude,
                    angularDiameter = 0.25f
                ),
                CanvasCircle(Color.YELLOW.withAlpha(80), Color.YELLOW)
            )
        }
        onMain {
            if (isResumed) {
                observedStarLayer.setMarkers(markers)
            }
        }
    }

    private suspend fun showDetectedStars(detected: List<DetectedStar>) {
        val markers = detected.map {
            ARMarker(
                SphericalARPoint(
                    it.reading.azimuth.value,
                    it.reading.altitude,
                    angularDiameter = 0.25f
                ),
                CanvasCircle(Color.GREEN.withAlpha(80), Color.GREEN),
                onFocusedFn = {
                    binding.arView.focusText = it.star.name
                    true
                }
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
        private const val TAG = "CelestialNavigation"
        private const val RETICLE_DIAMETER_DP = 48f
        private const val MIN_MATCHED_STARS = 4
        private const val MAX_CATALOG_MAGNITUDE = 3f
    }

}
