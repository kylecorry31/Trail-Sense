package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.NavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.ui.setTextDistinct
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.TimerActionBehavior
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolAugmentedRealityBinding
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.ARCalibratorFactory
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.ARDiscCalibrator
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.ARStarCalibrator
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.BrightDiscFinder
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import androidx.camera.core.ImageCapture
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceManager
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceRefreshRequest
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.BeaconGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.BearingGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.PathGuidanceTarget
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.BeaconGuidanceTargetCoordinator
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARAstronomyLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARBeaconLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARPathLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARSatelliteLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.map_layers.AugmentedRealityPathLayerManager
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.hypot

class AugmentedRealityFragment : BoundFragment<FragmentToolAugmentedRealityBinding>() {

    private var mode = ARMode.Normal
    private var timeOverride: Instant? = null

    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(requireContext())
    }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val planetMapper by lazy { PlanetMapper(requireContext()) }
    private val beaconGuidanceCoordinator by lazy { BeaconGuidanceTargetCoordinator(navigator) }
    private val guidance = ARGuidanceManager()

    private val beaconLayer by lazy {
        ARBeaconLayer(
            getString(R.string.beacons),
            Distance.meters(userPrefs.augmentedReality.beaconViewDistance),
            onFocus = this::onBeaconFocused
        ) {
            if (navigator.getBeaconDestinationId() != it.id) {
                navigator.navigateTo(it)
            } else {
                navigator.cancelBeaconNavigation()
            }
            true
        }
    }

    private val astronomyLayer by lazy {
        ARAstronomyLayer(
            getString(R.string.astronomy),
            drawBelowHorizon = userPrefs.augmentedReality.showAstronomyBelowHorizon,
            drawStars = userPrefs.augmentedReality.showStars,
            drawConstellations = userPrefs.augmentedReality.showConstellations,
            drawLowBrightnessObjects = userPrefs.augmentedReality.showLowBrightnessObjects,
            onSunFocus = this::onSunFocused,
            onMoonFocus = this::onMoonFocused,
            onStarFocus = this::onStarFocused,
            onPlanetFocus = this::onPlanetFocused,
            onMeteorShowerFocus = this::onMeteorShowerFocused
        )
    }

    private val satelliteLayer by lazy {
        ARSatelliteLayer(
            binding.arView.gps,
            this::onSatelliteFocused
        )
    }

    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    private val gridLayer by lazy {
        ARGridLayer(
            30,
            northColor = Resources.getCardinalDirectionColor(requireContext()),
            horizonColor = Color.WHITE,
            labelColor = Color.WHITE,
            color = Color.WHITE.withAlpha(100),
            useTrueNorth = userPrefs.compass.useTrueNorth
        )
    }

    private val pathsLayer by lazy {
        ARPathLayer(
            userPrefs.augmentedReality.pathViewDistance,
            adjustForPathElevation = userPrefs.augmentedReality.adjustForPathElevation,
            this::onPathFocused
        )
    }
    private var pathLayerManager: AugmentedRealityPathLayerManager? = null

    private var isCameraEnabled = true

    private var lastLocation = Coordinate.zero
    private val layerManagementUpdater = CoroutineTimer(actionBehavior = TimerActionBehavior.Skip) {
        if (!isBound) return@CoroutineTimer
        if (!userPrefs.augmentedReality.showPathLayer) return@CoroutineTimer
        if (binding.arView.location == lastLocation) return@CoroutineTimer
        lastLocation = binding.arView.location
        // This is only handling the path layer for now
        val viewDistance = Distance.meters(userPrefs.augmentedReality.pathViewDistance * 2f)
        pathLayerManager?.onBoundsChanged(
            CoordinateBounds.from(
                Geofence(
                    binding.arView.location,
                    viewDistance
                )
            )
        )
    }

    private val guidanceLocationUpdater =
        CoroutineTimer(actionBehavior = TimerActionBehavior.Skip) {
            if (!isBound) return@CoroutineTimer
            if (guidance.target.value == null) return@CoroutineTimer
            refreshGuidance()
        }

    // Calibration
    private val calibrationFactory = ARCalibratorFactory()
    private val astronomyService = AstronomyService()
    private val calibrationPrefs by lazy {
        requireContext().getSharedPreferences("ar_calibration_experiment", 0)
    }
    private var isAutoCalibrated = false
    private var isCalibrating = false
    private var lastAutoCalibration = 0L
    private var pendingAutoDelta: Pair<Long, Float>? = null

    private val autoCalibrationTimer = CoroutineTimer(actionBehavior = TimerActionBehavior.Skip) {
        if (!isBound) return@CoroutineTimer
        tryAutoCalibrate()
    }

    private var visibleLayersOverride by state<List<ARLayer>?>(null)
    private var visibleLayers by state<List<ARLayer>>(emptyList())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.arView.setCompassSensorDelay(userPrefs.compass.updateFrequency.sensorDelay)

        // Beacon layer setup (TODO: Move this to a layer manager)
        observeFlow(beaconRepo.getBeacons()) {
            beaconLayer.setBeacons(it)
        }

        observeNavigation()

        observeFlow(guidance.target) {
            beaconGuidanceCoordinator.onTargetChanged(it)

            if (isBound) {
                updateGuidanceButtonState()
                refreshGuidance()
            }
        }

        observeFlow(guidance.targetState) { state ->
            if (!isBound) {
                return@observeFlow
            }

            if (state == null) {
                binding.arView.clearGuide()
                binding.guidancePanel.setState(null)
            } else {
                binding.guidancePanel.setState(state.display)
                binding.arView.guideTo(state.point) {
                    // Guidance targets remain active until canceled or replaced
                }
            }
        }

        binding.guidancePanel.onCancel = {
            guidance.setTarget(null)
            setMode(ARMode.Normal)
        }

        binding.accuracyView.setSensors(binding.arView.gps, binding.arView.geomagneticOrientationSensor)
        binding.accuracyView.setOnClickListener { displayAccuracyTips() }

        binding.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        binding.camera.setShowTorch(false)

        binding.arView.infiniteFocusWhenPointedUp = true
        binding.arView.bind(binding.camera)

        val arguments = requireArguments()
        val modeId = arguments.getLong("mode", ARMode.Normal.id)
        val desiredMode = ARMode.entries.withId(modeId) ?: ARMode.Normal

        isCameraEnabled = arguments.getBoolean("camera_enabled", true)

        binding.cameraToggle.setOnClickListener {
            if (isCameraEnabled) {
                stopCamera()
            } else {
                startCamera()
            }
        }

        binding.calibrateBtn.setOnClickListener {
            startCalibration()
        }

        binding.layersBtn.setOnClickListener {
            showLayersSheet()
        }

        binding.arSearchBtn.setOnClickListener {
            showGuidanceLayerPicker()
        }

        binding.timeBtn.setOnClickListener {
            if (binding.timeSheet.isVisible) {
                setTimeOverride(null)
                binding.timeSheet.hide()
            } else {
                binding.timeSheet.setTime(timeOverride)
                binding.timeSheet.show()
            }
            updateTimeButtonState()
        }

        binding.timeSheet.onTimeChanged = {
            setTimeOverride(it)
            updateTimeButtonState()
            refreshGuidance()
        }

        setMode(desiredMode, requireArguments().getBundle("extras"))

        binding.calibrateBtn.setOnLongClickListener {
            dialog(getString(R.string.reset_calibration_question)) { cancelled ->
                if (!cancelled) {
                    binding.arView.resetCalibration()
                    isAutoCalibrated = false
                }
            }
            true
        }

        binding.confirmCalibrationButton.setOnClickListener {
            calibrate()
        }

        binding.cancelCalibrationButton.setOnClickListener {
            stopCalibration()
        }

        binding.arView.setOnFocusLostListener {
            binding.focusActionButton.isVisible = false
        }

        ARCalibrateDisclaimer(requireContext()).alert()

        if (!Sensors.hasGyroscope(requireContext())) {
            ARNoGyroAlert(requireContext()).alert()
        }
    }

    override fun onResume() {
        super.onResume()

        pathLayerManager = AugmentedRealityPathLayerManager(
            requireContext(),
            pathsLayer,
            shouldCorrectElevations = userPrefs.augmentedReality.adjustForPathElevation
        )
        pathLayerManager?.start()
        layerManagementUpdater.interval(100)

        binding.arView.start()
        if (isCameraEnabled) {
            startCamera()
        } else {
            stopCamera()
        }

        updateLayerVisibility()
        updateTimeButtonState()
        updateGuidanceButtonState()
        guidanceLocationUpdater.interval(1000)
        refreshGuidance()
        autoCalibrationTimer.interval(1000)
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
                    readFrames = false,
                    shouldStabilizePreview = false,
                    captureSettings = ImageCaptureSettings(
                        quality = 100,
                        captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
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
        astronomyLayer.pause()
        setTimeOverride(null)
        binding.timeSheet.hide()
        pathLayerManager?.stop()
        layerManagementUpdater.stop()
        guidanceLocationUpdater.stop()
        autoCalibrationTimer.stop()
    }

    override fun onUpdate() {
        super.onUpdate()

        effect(
            "layer_visibility",
            visibleLayers,
            visibleLayersOverride,
            lifecycleHookTrigger.onResume()
        ) {
            binding.arView.setLayers(visibleLayersOverride ?: visibleLayers)
        }

        val astronomy = useService<AstronomySubsystem>()
        val isNight = astronomy.isNight()
        val prefs = useService<UserPreferences>()
        val shouldIncreaseExposureAtNight = useMemo(prefs) {
            prefs.augmentedReality.increaseExposureAtNight
        }

        // Increase exposure at night
        useEffect(binding.arView, isNight, shouldIncreaseExposureAtNight) {
            binding.arView.setExposureCompensation(if (isNight && shouldIncreaseExposureAtNight) 0.5f else 0f)
        }
    }

    // TODO: Extract focus formatters
    private fun onSunFocused(time: ZonedDateTime): Boolean {
        binding.arView.focusText =
            getString(R.string.sun) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            )
        return true
    }

    private fun onMoonFocused(time: ZonedDateTime, phase: MoonPhase): Boolean {
        binding.arView.focusText =
            getString(R.string.moon) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            ) + "\n${formatter.formatMoonPhase(phase.phase)} (${
                formatter.formatPercentage(
                    phase.illumination
                )
            })"
        return true
    }

    private fun onStarFocused(star: Star): Boolean {
        val constellations = astronomyService.getConstellationsForStar(star)
        val constellationString =
            constellations.joinToString("\n\n\n") { "${getString(R.string.constellation)}\n${it.name}" }
        binding.arView.focusText =
            "${getString(R.string.star)}\n${star.name}\n\n\n$constellationString".trim()
        return true
    }

    private fun onPlanetFocused(planet: Planet): Boolean {
        binding.arView.focusText = planetMapper.getName(planet)
        return true
    }

    private fun onMeteorShowerFocused(shower: MeteorShower): Boolean {
        binding.arView.focusText = getString(R.string.meteor_shower) + "\n" + shower.readableName()
        return true
    }

    private fun onSatelliteFocused(satellite: Satellite): Boolean {
        binding.arView.focusText = "${satellite.constellation} ${satellite.id}"
        return true
    }

    private fun onPathFocused(path: IMappablePath): Boolean {
        binding.arView.focusText = path.name
        return true
    }

    private fun observeNavigation() {
        observeFlow(navigator.destination) {
            beaconLayer.destination = it as? Destination.Beacon
            pathsLayer.destination = it as? Destination.Path
            if (mode == ARMode.Normal && it != null) {
                guidance.setTarget(when (it) {
                    is Destination.Beacon -> BeaconGuidanceTarget(it.beacon)
                    is Destination.Path -> PathGuidanceTarget(it)
                    is Destination.Bearing -> BearingGuidanceTarget(it)
                })
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

        // If the beacon isn't the destination, show the navigate button
        if (navigator.getBeaconDestinationId() != beacon.id) {
            binding.focusActionButton.setTextDistinct(getString(R.string.navigate))
            binding.focusActionButton.setOnClickListener {
                guidance.setTarget(BeaconGuidanceTarget(beacon))
            }
            binding.focusActionButton.isVisible = true
        } else {
            binding.focusActionButton.isVisible = false
        }

        return true
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentToolAugmentedRealityBinding {
        return FragmentToolAugmentedRealityBinding.inflate(layoutInflater, container, false)
    }

    private fun setMode(mode: ARMode, extras: Bundle? = null) {
        val previousMode = this.mode
        this.mode = mode
        when (mode) {
            ARMode.Normal -> {
                visibleLayersOverride = null
                if (previousMode == ARMode.Astronomy) {
                    setTimeOverride(null)
                    binding.timeSheet.hide()
                    updateTimeButtonState()
                }
            }

            ARMode.Astronomy -> {
                guidance.setTarget(null)
                visibleLayersOverride = listOf(gridLayer, astronomyLayer)
                val overrideTime = extras?.getString("time")?.let {
                    ZonedDateTime.parse(it)
                }
                overrideTime?.let {
                    setTimeOverride(it.toInstant())
                    binding.timeSheet.setTime(timeOverride)
                    binding.timeSheet.show()
                    updateTimeButtonState()
                }
                showAstronomyTargetPicker()
            }
        }

        updateGuidanceButtonState()
    }

    private fun showGuidanceLayerPicker() {
        val layers = currentVisibleLayers().filterIsInstance<ARGuidanceLayer>()
        if (layers.isEmpty()) {
            updateGuidanceButtonState()
            return
        }

        Pickers.item(
            requireContext(),
            getString(R.string.locate),
            layers.map { it.guidanceName }
        ) { selected ->
            if (selected == null) {
                handleGuidancePickerCancelled()
                return@item
            }

            val layer = layers.getOrNull(selected) ?: run {
                handleGuidancePickerCancelled()
                return@item
            }

            inBackground {
                val target = layer.pickGuidanceTarget(binding.arView)
                if (target == null) {
                    handleGuidancePickerCancelled()
                    return@inBackground
                }

                guidance.setTarget(target)
            }
        }
    }

    private fun showAstronomyTargetPicker() {
        inBackground {
            val target = astronomyLayer.pickGuidanceTarget(binding.arView)
            if (target == null) {
                handleGuidancePickerCancelled()
                return@inBackground
            }
            guidance.setTarget(target)
        }
    }

    private fun handleGuidancePickerCancelled() {
        if (mode == ARMode.Astronomy && guidance.target.value == null) {
            setMode(ARMode.Normal)
        }
    }

    private fun refreshGuidance() {
        val request = ARGuidanceRefreshRequest(
            requireContext(),
            binding.arView.location,
            timeOverride?.toZonedDateTime() ?: ZonedDateTime.now()
        )
        inBackground {
            guidance.updateTargetState(request)
        }
    }

    private fun currentVisibleLayers(): List<ARLayer> {
        return visibleLayersOverride ?: visibleLayers
    }

    private fun updateGuidanceButtonState() {
        val hasGuidanceLayers = currentVisibleLayers().any { it is ARGuidanceLayer }
        binding.arSearchBtn.isVisible = guidance.target.value != null || hasGuidanceLayers
    }

    private fun showLayersSheet() {
        val sheet = ARLayersBottomSheet()
        sheet.setOnDismissListener {
            updateLayerVisibility()
        }
        sheet.show(this)
    }

    private fun startCalibration() {
        val isSunUp = astronomyService.isSunUp(binding.arView.location)
        val isAuto = calibrationPrefs.getBoolean("auto", true)
        val options = listOf(
            if (isSunUp) "Center the sun" else "Center the moon",
            "Photo: stars (night, hold still)",
            "Photo: ${if (isSunUp) "sun" else "moon"} anywhere in frame",
            "Auto calibrate from sun / moon in frame: ${if (isAuto) "ON" else "OFF"}"
        )
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.calibrate))
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> startCenteredCalibration(isSunUp)
                    1 -> calibrateFromStars()
                    2 -> calibrateFromDisc(manual = true)
                    3 -> calibrationPrefs.edit().putBoolean("auto", !isAuto).apply()
                }
            }
            .show()
    }

    private fun startCenteredCalibration(isSunUp: Boolean) {
        binding.calibrationPanel.isVisible = true
        dialog(
            getString(R.string.calibrate),
            getString(
                R.string.ar_calibration_instructions,
                if (isSunUp) getString(R.string.sun) else getString(R.string.moon)
            ),
        ) { cancelled ->
            if (cancelled) {
                stopCalibration()
            }
        }
    }

    private fun calibrate() {
        inBackground {
            val useGyro = userPrefs.augmentedReality.useGyroOnlyAfterCalibration
            val calibrator = if (astronomyService.isSunUp(binding.arView.location)) {
                calibrationFactory.getSunCalibrator(binding.arView.location)
            } else {
                calibrationFactory.getMoonCalibrator(binding.arView.location)
            }
            binding.arView.calibrate(calibrator, useGyro)
            isAutoCalibrated = true
            stopCalibration()
        }
    }

    private fun calibrateFromStars() {
        if (isCalibrating) return
        isCalibrating = true
        Alerts.toast(requireContext(), "Hold still, taking a star photo...")
        inBackground {
            try {
                val calibrator = ARStarCalibrator()
                val delta = applyCalibration(calibrator)
                val result = calibrator.lastResult
                val message = if (delta == null || result == null) {
                    "Star calibration failed: ${calibrator.failureReason ?: "unknown"}"
                } else {
                    "Compass was off by %.2f°\n%d stars detected, %d identified, fit %.3f°\nTilt: sensor %.2f°, stars %.2f°\nTotal offset now %.2f°".format(
                        delta, result.stars, result.matched, result.rmsDegrees,
                        result.sensorAltitude, result.starAltitude, binding.arView.calibrationOffset
                    )
                }
                onMain { dialog("Star calibration", message, cancelText = null) }
            } finally {
                isCalibrating = false
            }
        }
    }

    private fun bodyReference(): Pair<String, AugmentedRealityCoordinate>? {
        if (timeOverride != null) return null
        val location = binding.arView.location
        val (name, position) = when {
            astronomyService.isSunUp(location) -> "sun" to astronomyService.getSunPosition(location)
            astronomyService.isMoonUp(location) -> "moon" to astronomyService.getMoonPosition(location)
            else -> return null
        }
        if (position.altitude < 0) return null
        return name to AugmentedRealityCoordinate(
            AugmentedRealityUtils.toEastNorthUp(position.azimuth.value, position.altitude, Float.MAX_VALUE),
            true
        )
    }

    private fun discCalibrator(name: String, reference: AugmentedRealityCoordinate): ARDiscCalibrator {
        val finder = if (name == "sun") {
            BrightDiscFinder(minPeak = 245, minContrast = 30)
        } else {
            BrightDiscFinder(minPeak = 150, minContrast = 80)
        }
        return ARDiscCalibrator(reference, finder)
    }

    /**
     * The first calibration goes through the normal path (which may switch to the gyroscope),
     * later ones adjust the existing calibration.
     */
    private suspend fun applyCalibration(calibrator: com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.IARCalibrator): Float? {
        return if (!isAutoCalibrated) {
            val before = binding.arView.calibrationOffset
            binding.arView.calibrate(calibrator, userPrefs.augmentedReality.useGyroOnlyAfterCalibration)
            val after = binding.arView.calibrationOffset
            if (after == 0f && before != 0f) null else {
                isAutoCalibrated = true
                after
            }
        } else {
            binding.arView.recalibrate(calibrator)
        }
    }

    private fun calibrateFromDisc(manual: Boolean) {
        if (isCalibrating) return
        val (name, reference) = bodyReference() ?: run {
            if (manual) Alerts.toast(requireContext(), "The sun and moon are not up")
            return
        }
        isCalibrating = true
        inBackground {
            try {
                val calibrator = discCalibrator(name, reference)
                val delta = applyCalibration(calibrator)
                val result = calibrator.lastResult
                if (manual) {
                    val message = if (delta == null || result == null) {
                        "Could not find the $name in the frame"
                    } else {
                        "Compass was off by %.2f°\nTilt error %.2f°\nDisc radius %.0f px\nTotal offset now %.2f°".format(
                            delta, result.altitudeError, result.radiusPixels, binding.arView.calibrationOffset
                        )
                    }
                    onMain { dialog("Calibrate from the $name", message, cancelText = null) }
                } else if (delta != null) {
                    onMain { Alerts.toast(requireContext(), "Auto calibrated from the $name (%+.1f°)".format(delta)) }
                }
            } finally {
                isCalibrating = false
            }
        }
    }

    /**
     * Calibrates whenever the sun or moon is in the frame. Two detections in a row must agree so
     * that a light or reflection doesn't cause a bad calibration.
     */
    private fun tryAutoCalibrate() {
        if (isCalibrating || !isCameraEnabled) return
        if (!calibrationPrefs.getBoolean("auto", true)) return
        val now = System.currentTimeMillis()
        if (now - lastAutoCalibration < 10_000) return
        val (name, reference) = bodyReference() ?: return
        val pixel = binding.arView.toPixel(reference, nanIfBehindCamera = true)
        val margin = 0.1f
        if (!pixel.x.isFinite() || !pixel.y.isFinite() ||
            pixel.x < binding.arView.width * margin || pixel.x > binding.arView.width * (1 - margin) ||
            pixel.y < binding.arView.height * margin || pixel.y > binding.arView.height * (1 - margin)
        ) {
            pendingAutoDelta = null
            return
        }
        isCalibrating = true
        inBackground {
            var confirmed = false
            try {
                // Measure without applying first
                val delta = discCalibrator(name, reference).calibrateBearing(binding.arView, binding.camera)
                val previous = pendingAutoDelta
                pendingAutoDelta = delta?.let { System.currentTimeMillis() to it }
                confirmed = delta != null && previous != null &&
                        System.currentTimeMillis() - previous.first < 3000 &&
                        kotlin.math.abs(previous.second - delta) < 1f
            } finally {
                isCalibrating = false
            }
            if (confirmed) {
                pendingAutoDelta = null
                lastAutoCalibration = System.currentTimeMillis()
                onMain { calibrateFromDisc(manual = false) }
            }
        }
    }

    private fun stopCalibration() {
        binding.calibrationPanel.isVisible = false
    }

    private fun displayAccuracyTips() {
        context ?: return

        val alerter = ImproveAccuracyAlerter(
            requireContext(),
            getString(R.string.ar_calibration_instructions_hint)
        )
        alerter.alert(listOf(binding.arView.gps, binding.arView.geomagneticOrientationSensor))
    }

    private fun updateLayerVisibility() {
        pathsLayer.appearance = userPrefs.augmentedReality.pathAppearance
        visibleLayers = listOfNotNull(
            if (userPrefs.augmentedReality.showGridLayer) gridLayer else null,
            if (userPrefs.augmentedReality.showSatelliteLayer) satelliteLayer else null,
            if (userPrefs.augmentedReality.showAstronomyLayer) astronomyLayer else null,
            if (userPrefs.augmentedReality.showPathLayer) pathsLayer else null,
            if (userPrefs.augmentedReality.showBeaconLayer) beaconLayer else null
        )
        updateGuidanceButtonState()
    }

    private fun setTimeOverride(time: Instant?) {
        timeOverride = time
        astronomyLayer.timeOverride = time?.toZonedDateTime()
    }

    private fun updateTimeButtonState() {
        val isActive = timeOverride != null || binding.timeSheet.isVisible
        binding.timeBtn.alpha = if (isActive) 1f else 0.7f
    }

    companion object {
        fun open(
            navController: NavController,
            mode: ARMode = ARMode.Normal,
            enableCamera: Boolean = true,
            extras: Bundle? = null
        ) {
            navController.navigate(
                R.id.augmentedRealityFragment, Bundle().apply {
                    putLong("mode", mode.id)
                    putBoolean("camera_enabled", enableCamera)
                    putBundle("extras", extras)
                }
            )
        }
    }

}
