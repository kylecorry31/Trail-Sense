package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.res.ColorStateList
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
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.time.TimerActionBehavior
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.Colors
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
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
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
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARBeaconGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceDisplayState
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.BeaconGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARAstronomyLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARBeaconLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARPathLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARSatelliteLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.shared.domain.IMappablePath
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
    private var activeGuidanceTarget: ARGuidanceTarget? = null
    private val guidanceRefreshRunner = CoroutineQueueRunner()

    private val beaconLayer by lazy {
        ARBeaconLayer(
            getString(R.string.beacons),
            Distance.meters(userPrefs.augmentedReality.beaconViewDistance),
            onFocus = this::onBeaconFocused
        ) {
            if (navigator.getDestinationId() != it.id) {
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
            if (activeGuidanceTarget == null) return@CoroutineTimer
            refreshGuidance()
        }

    // Calibration
    private val calibrationFactory = ARCalibratorFactory()
    private val astronomyService = AstronomyService()

    private var visibleLayersOverride by state<List<ARLayer>?>(null)
    private var visibleLayers by state<List<ARLayer>>(emptyList())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Beacon layer setup (TODO: Move this to a layer manager)
        observeFlow(beaconRepo.getBeacons()) {
            beaconLayer.setBeacons(it)
        }
        observeFlow(navigator.destination) {
            beaconLayer.destination = it
            syncBeaconGuidance(it)
        }

        binding.guidancePanel.isVisible = false
        binding.arGuideCancel.setOnClickListener {
            replaceActiveGuidanceTarget(null)
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

        binding.searchBtn.setOnClickListener {
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
        binding.timeSheet.hide()
        pathLayerManager?.stop()
        layerManagementUpdater.stop()
        guidanceLocationUpdater.stop()
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
        if (navigator.getDestinationId() != beacon.id) {
            binding.focusActionButton.setTextDistinct(getString(R.string.navigate))
            binding.focusActionButton.setOnClickListener {
                replaceActiveGuidanceTarget(BeaconGuidanceTarget(beacon))
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
                replaceActiveGuidanceTarget(null)
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

                replaceActiveGuidanceTarget(target)
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
            replaceActiveGuidanceTarget(target)
        }
    }

    private fun handleGuidancePickerCancelled() {
        if (mode == ARMode.Astronomy && activeGuidanceTarget == null) {
            setMode(ARMode.Normal)
        }
    }

    private fun replaceActiveGuidanceTarget(target: ARGuidanceTarget?) {
        val previousBeaconId = (activeGuidanceTarget as? ARBeaconGuidanceTarget)?.beacon?.id
        val newBeacon = (target as? ARBeaconGuidanceTarget)?.beacon

        activeGuidanceTarget = null
        binding.arView.clearGuide()
        updateGuidancePanel(null)

        if (previousBeaconId != null && previousBeaconId != newBeacon?.id) {
            navigator.cancelBeaconNavigation()
        }

        activeGuidanceTarget = target

        if (newBeacon != null) {
            navigator.navigateTo(newBeacon)
        }

        updateGuidanceButtonState()
        refreshGuidance()
    }

    private fun syncBeaconGuidance(destination: Beacon?) {
        if (mode != ARMode.Normal) {
            return
        }

        val activeBeaconId = (activeGuidanceTarget as? ARBeaconGuidanceTarget)?.beacon?.id

        if (destination == null) {
            return
        }

        if (activeBeaconId == destination.id) {
            return
        }

        replaceActiveGuidanceTarget(BeaconGuidanceTarget(destination))
    }

    private fun refreshGuidance() {
        val target = activeGuidanceTarget

        if (target == null) {
            binding.arView.clearGuide()
            updateGuidancePanel(null)
            return
        }

        inBackground {
            guidanceRefreshRunner.enqueue {
                tryOrLog {
                    val state = target.refresh(binding.arView)
                    onMain {
                        if (!isBound || activeGuidanceTarget !== target) {
                            return@onMain
                        }

                        updateGuidancePanel(state.display)
                        binding.arView.guideTo(state.point) {
                            // Guidance targets remain active until cancelled or replaced
                        }
                    }
                }
            }
        }
    }

    private fun updateGuidancePanel(state: ARGuidanceDisplayState?) {
        if (state == null) {
            binding.guidancePanel.isVisible = false
            return
        }

        binding.guidancePanel.isVisible = true
        binding.arGuideName.text = state.name
        binding.arGuideIcon.setImageResource(state.icon)
        binding.arGuideIcon.rotation = state.iconRotation
        binding.arGuideIcon.backgroundTintList =
            ColorStateList.valueOf(state.iconBackgroundTint ?: Color.TRANSPARENT)
        Colors.setImageColor(binding.arGuideIcon, state.iconTint)
    }

    private fun currentVisibleLayers(): List<ARLayer> {
        return visibleLayersOverride ?: visibleLayers
    }

    private fun updateGuidanceButtonState() {
        val hasGuidanceLayers = currentVisibleLayers().any { it is ARGuidanceLayer }
        binding.searchBtn.isVisible = activeGuidanceTarget != null || hasGuidanceLayers
    }

    private fun showLayersSheet() {
        val sheet = ARLayersBottomSheet()
        sheet.setOnDismissListener {
            updateLayerVisibility()
        }
        sheet.show(this)
    }

    private fun startCalibration() {
        binding.calibrationPanel.isVisible = true
        val isSunUp = astronomyService.isSunUp(binding.arView.location)
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
            stopCalibration()
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
