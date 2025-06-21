package com.kylecorry.trail_sense.tools.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.ui.setTextDistinct
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.interval
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.settings.ui.CompassCalibrationView
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.diagnostics.status.GpsStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.StatusBadge
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyle
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyleChooser
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.tools.navigation.ui.data.UpdateAstronomyLayerCommand
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationCompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.BeaconCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.MarkerCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.NavigationCompassLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import java.time.Duration
import java.time.Instant


class NavigatorFragment : BoundFragment<ActivityNavigatorBinding>() {

    private val compass by lazy { sensorService.getCompass() }
    private val gps by lazy { sensorService.getGPS(frequency = Duration.ofMillis(200)) }
    private val orientation by lazy { sensorService.getOrientation() }
    private val clinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val altimeter by lazy { sensorService.getAltimeter(gps = gps) }
    private val speedometer by lazy { sensorService.getSpeedometer(gps = gps) }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            userPrefs,
            gps
        )
    }
    private var declination = 0f

    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }

    private val sensorService by lazy { SensorService(requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }

    private val navigationService = NavigationService()
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private var beacons: Collection<Beacon> = listOf()
    private var nearbyBeacons: List<Beacon> = listOf()

    private var destination: Beacon? = null
    private var destinationBearing: Float? = null
    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    // Status badges
    private val gpsStatusBadgeProvider by lazy { GpsStatusBadgeProvider(gps, requireContext()) }
    private val compassStatusBadgeProvider by lazy {
        SensorStatusBadgeProvider(
            compass,
            requireContext(),
            R.drawable.ic_compass_icon
        )
    }

    // Diagnostics
    private val errors by lazy { NavigatorUserErrors(this) }

    // Data commands
    private val updateAstronomyLayerCommand by lazy {
        UpdateAstronomyLayerCommand(
            astronomyCompassLayer,
            userPrefs,
            gps
        ) { declination }
    }

    private val loadBeaconsRunner = CoroutineQueueRunner()

    private val layers = NavigationCompassLayerManager()

    // Compass layers
    private val beaconCompassLayer = BeaconCompassLayer()
    private val astronomyCompassLayer = MarkerCompassLayer()
    private val navigationCompassLayer = NavigationCompassLayer()

    // Feature availability
    private val hasCompass by lazy { sensorService.hasCompass() }

    // Cached preferences
    private val baseDistanceUnits by lazy { userPrefs.baseDistanceUnits }
    private val isNearbyEnabled by lazy { userPrefs.navigation.showMultipleBeacons }
    private val nearbyCount by lazy { userPrefs.navigation.numberOfVisibleBeacons }
    private val nearbyDistance
        get() = userPrefs.navigation.maxBeaconDistance
    private val useRadarCompass by lazy { userPrefs.navigation.useRadarCompass }
    private val styleChooser by lazy { CompassStyleChooser(userPrefs.navigation, hasCompass) }
    private val useTrueNorth by lazy { userPrefs.compass.useTrueNorth }
    private val screenLock by lazy { NavigationScreenLock(userPrefs.navigation.keepScreenUnlockedWhileOpen) }


    // State
    private var compassStatusBadge: StatusBadge? = null
    private var gpsStatusBadge: StatusBadge? = null
    private var diagnosticResults by state<Map<Int, List<String>>>(emptyMap())

    private val northReferenceHideTimer = CoroutineTimer {
        if (isBound) {
            binding.northReferenceIndicator.showLabel = false
        }
    }

    private val triggers = HookTriggers()

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.let { screenLock.releaseLock(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("destination") ?: 0L

        // Load the destination and start navigation
        if (beaconId != 0L) {
            showCalibrationDialog()
            inBackground {
                navigator.navigateTo(beaconId)
                destination = navigator.getDestination()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe diagnostics
        listOf(
            GPSDiagnosticScanner(gps),
            MagnetometerDiagnosticScanner()
        ).mapIndexed { index, scanner ->
            observeFlow(
                scanner.fullScan(requireContext()),
                BackgroundMinimumState.Resumed
            ) { results ->
                diagnosticResults = diagnosticResults + (index to results.map { it.id })
            }
        }

        // Register timers

        // TODO: This shouldn't be needed - layers can be updated when the data changes
        interval(100) {
            updateCompassLayers()
        }

        interval(Duration.ofSeconds(1)) {
            updateSensorStatus()
        }

        binding.roundCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                beaconCompassLayer,
                navigationCompassLayer
            )
        )

        binding.linearCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                beaconCompassLayer,
                navigationCompassLayer
            )
        )

        binding.radarCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                navigationCompassLayer
            )
        )


        binding.speed.setShowDescription(false)
        binding.altitude.setShowDescription(false)

        NavigationQuickActionBinder(
            this,
            binding,
            userPrefs.navigation
        ).bind()

        observeFlow(beaconRepo.getBeacons()) {
            beacons = it
            updateNearbyBeacons()
        }

        observe(compass) { }
        observe(clinometer) { }
        observe(altimeter) { }
        observe(gps) { }
        observe(speedometer) { }

        binding.navigationTitle.subtitle.setOnLongClickListener {
            Share.shareLocation(
                this,
                gps.location
            )
            true
        }

        binding.navigationTitle.subtitle.setOnClickListener {
            val sheet = LocationBottomSheet()
            sheet.gps = gps
            sheet.show(this)
        }

        binding.altitude.setOnClickListener {
            val sheet = AltitudeBottomSheet()
            sheet.currentAltitude = Reading(altimeter.altitude, Instant.now())
            sheet.show(this)
        }

        binding.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.beaconBtn.setOnClickListener {
            if (destination == null) {
                findNavController().openTool(Tools.BEACONS)
            } else {
                destination = null
                navigator.cancelNavigation()
            }
        }

        binding.beaconBtn.setOnLongClickListener {
            if (gps.hasValidReading) {
                val bundle = bundleOf(
                    "initial_location" to GeoUri(
                        gps.location,
                        if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
                    )
                )
                findNavController().openTool(Tools.BEACONS, bundle)
            } else {
                findNavController().openTool(Tools.BEACONS)
            }
            true
        }

        binding.accuracyView.setOnClickListener { displayAccuracyTips() }

        binding.roundCompass.setOnClickListener {
            toggleDestinationBearing()
        }
        binding.radarCompass.setOnSingleTapListener {
            toggleDestinationBearing()
        }
        binding.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        if (!hasCompass) {
            binding.radarCompass.shouldDrawDial = userPrefs.navigation.showDialTicksWhenNoCompass
            binding.compassStatus.isVisible = false
            binding.navigationTitle.title.isVisible = false
            binding.northReferenceIndicator.isVisible = false
        }

        scheduleUpdates(INTERVAL_30_FPS)
    }

    private fun toggleDestinationBearing() {
        if (destination != null) {
            // Don't set destination bearing while navigating
            return
        }

        // If there is no compass, don't allow setting a destination bearing
        if (!hasCompass) {
            destinationBearing = null
            cache.remove(LAST_DEST_BEARING)
            return
        }

        if (destinationBearing == null) {
            destinationBearing = compass.rawBearing
            cache.putFloat(LAST_DEST_BEARING, compass.rawBearing)
            Alerts.toast(
                requireContext(),
                getString(R.string.toast_destination_bearing_set)
            )
        } else {
            destinationBearing = null
            cache.remove(LAST_DEST_BEARING)
        }
    }

    private fun handleShowWhenLocked() {
        activity?.let { screenLock.updateLock(it) }
    }

    fun displayAccuracyTips() {
        context ?: return

        val alerter = ImproveAccuracyAlerter(requireContext())
        alerter.alert(listOf(gps, compass))
    }

    private fun updateAstronomyData() {
        inBackground {
            if (gps.location == Coordinate.zero) {
                return@inBackground
            }

            updateAstronomyLayerCommand.execute()
        }
    }

    override fun onResume() {
        super.onResume()

        layers.resume(requireContext(), binding.radarCompass)

        // Resume navigation
        inBackground {
            destination = navigator.getDestination()
        }

        // Restore the last destination bearing
        val lastDestBearing = cache.getFloat(LAST_DEST_BEARING)
        if (lastDestBearing != null && hasCompass) {
            destinationBearing = lastDestBearing
        }

        // Show the north reference indicator
        binding.northReferenceIndicator.showDetailsOnClick = true
        binding.northReferenceIndicator.useTrueNorth = useTrueNorth
        binding.northReferenceIndicator.showLabel = true
        northReferenceHideTimer.once(Duration.ofSeconds(5))
    }

    override fun onPause() {
        super.onPause()
        loadBeaconsRunner.cancel()
        errors.reset()
        layers.pause(requireContext(), binding.radarCompass)
        northReferenceHideTimer.stop()
    }

    private fun updateNearbyBeacons() {
        inBackground {
            onIO {
                loadBeaconsRunner.skipIfRunning {
                    if (!isNearbyEnabled) {
                        nearbyBeacons = listOfNotNull(destination)
                        return@skipIfRunning
                    }

                    nearbyBeacons = (navigationService.getNearbyBeacons(
                        gps.location,
                        beacons,
                        nearbyCount,
                        8f,
                        nearbyDistance
                    ) + listOfNotNull(destination)).distinctBy { it.id }
                }
            }
        }
    }

    private fun getDestinationBearing(): Float? {
        val destLocation = destination?.coordinate
        return when {
            destLocation != null -> {
                fromTrueNorth(gps.location.bearingTo(destLocation).value)
            }

            destinationBearing != null -> {
                destinationBearing
            }

            else -> {
                null
            }
        }
    }

    private fun getSelectedBeacon(nearby: Collection<Beacon>): Beacon? {
        return destination ?: getFacingBeacon(nearby)
    }

    private fun fromTrueNorth(bearing: Float): Float {
        if (useTrueNorth) {
            return bearing
        }
        return DeclinationUtils.fromTrueNorthBearing(bearing, declination)
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            gps.location,
            compass.rawBearing,
            nearby,
            declination,
            useTrueNorth
        )
    }

    override fun onUpdate() {
        super.onUpdate()

        if (!isBound) {
            return
        }

        // TODO: Move selected beacon updating to a coroutine
        effect(
            "selected_beacon",
            destination,
            compass.rawBearing.safeRoundToInt(),
            lifecycleHookTrigger.onResume()
        ) {
            val selectedBeacon = getSelectedBeacon(nearbyBeacons)
            if (selectedBeacon != null) {
                binding.navigationSheet.show(
                    gps.location,
                    altimeter.altitude,
                    speedometer.speed.speed,
                    selectedBeacon,
                    declination,
                    useTrueNorth
                )
            } else {
                binding.navigationSheet.hide()
            }
        }

        effect("gps_status", gpsStatusBadge, lifecycleHookTrigger.onResume()) {
            gpsStatusBadge?.let {
                binding.gpsStatus.setStatusText(it.name)
                binding.gpsStatus.setBackgroundTint(it.color)
            }
        }

        effect("compass_status", compassStatusBadge, lifecycleHookTrigger.onResume()) {
            compassStatusBadge?.let {
                binding.compassStatus.setStatusText(it.name)
                binding.compassStatus.setBackgroundTint(it.color)
            }
        }

        effect("speed", speedometer.speed.speed, lifecycleHookTrigger.onResume()) {
            binding.speed.title = formatService.formatSpeed(speedometer.speed.speed)
        }

        effect("azimuth", compass.rawBearing, lifecycleHookTrigger.onResume()) {
            updateCompassBearing()
        }

        useEffect(triggers.frequency("compass_invalidation", Duration.ofSeconds(1))) {
            binding.radarCompass.invalidate()
        }

        effect("altitude", altimeter.altitude, lifecycleHookTrigger.onResume()) {
            binding.altitude.title = formatService.formatDistance(
                Distance.meters(altimeter.altitude).convertTo(baseDistanceUnits)
            )
        }

        effect("location", gps.location, layers.key, lifecycleHookTrigger.onResume()) {
            updateLocation()
        }

        effect(
            "astronomy",
            triggers.distance(
                "astronomy",
                gps.location,
                ASTRONOMY_UPDATE_DISTANCE,
                highAccuracy = false
            ),
            triggers.frequency("astronomy", ASTRONOMY_UPDATE_FREQUENCY),
            lifecycleHookTrigger.onResume()
        ) {
            updateAstronomyData()
        }

        effect("navigation", destination, lifecycleHookTrigger.onResume()) {
            handleShowWhenLocked()
            updateNavigationButton()
        }

        effect("device_orientation", clinometer.incline.toInt(), lifecycleHookTrigger.onResume()) {
            val deviceOrientation = if (clinometer.incline > -30) {
                DeviceOrientation.Orientation.Portrait
            } else {
                DeviceOrientation.Orientation.Flat
            }
            val style =
                styleChooser.getStyle(deviceOrientation)

            binding.linearCompass.isInvisible = style != CompassStyle.Linear
            binding.roundCompass.isInvisible = style != CompassStyle.Round
            binding.radarCompass.isInvisible = style != CompassStyle.Radar
        }

        effect("sighting_compass_flashlight", binding.linearCompass.isCameraActive) {
            // TODO: Extract this logic to the flashlight (if camera is in use)
            if (userPrefs.navigation.rightButton == Tools.QUICK_ACTION_FLASHLIGHT) {
                binding.navigationTitle.rightButton.isClickable =
                    !binding.linearCompass.isCameraActive
            }
            if (userPrefs.navigation.leftButton == Tools.QUICK_ACTION_FLASHLIGHT) {
                binding.navigationTitle.leftButton.isClickable =
                    !binding.linearCompass.isCameraActive
            }
        }

        effect("error_messages", diagnosticResults, lifecycleHookTrigger.onResume()) {
            val codes = diagnosticResults.values.flatten().distinct()
            errors.update(codes)
        }
    }

    private fun updateCompassBearing() {
        val bearing = compass.bearing

        // Azimuth
        if (hasCompass) {
            val titleText = memo("azimuth_title", bearing.value.safeRoundToInt()) {
                val azimuthText =
                    formatService.formatDegrees(bearing.value, replace360 = true)
                        .padStart(4, ' ')
                val directionText = formatService.formatDirection(bearing.direction)
                    .padStart(2, ' ')
                "$azimuthText   $directionText"
            }
            binding.navigationTitle.title.setTextDistinct(titleText)
        }

        layers.onBearingChanged(bearing.value)

        // Compass
        listOf<ICompassView>(
            binding.roundCompass,
            binding.radarCompass,
            binding.linearCompass
        ).forEach {
            it.azimuth = bearing
            it.declination = declination
        }
    }

    private fun updateLocation() {
        val location = gps.location

        declination = declinationProvider.getDeclination()
        compass.declination = declination

        binding.navigationTitle.subtitle.setTextDistinct(
            formatService.formatLocation(location)
        )

        layers.onLocationChanged(location, gps.horizontalAccuracy)

        // Compass center point
        listOf<ICompassView>(
            binding.roundCompass,
            binding.radarCompass,
            binding.linearCompass
        ).forEach {
            it.compassCenter = location
        }

        updateNearbyBeacons()

        if (useRadarCompass) {
            val loadGeofence = Geofence(
                location,
                Distance.meters(nearbyDistance + 10)
            )
            val bounds = CoordinateBounds.from(loadGeofence)
            layers.onBoundsChanged(bounds)
        }
    }

    private fun updateCompassLayers() {
        inBackground {
            val destBearing = getDestinationBearing()
            val destination = destination
            val destColor = destination?.color ?: AppColor.Blue.color

            val direction = destBearing?.let {
                MappableBearing(
                    Bearing(it),
                    destColor
                )
            }

            // Update beacon layers
            layers.setBeacons(nearbyBeacons)
            beaconCompassLayer.setBeacons(nearbyBeacons)
            beaconCompassLayer.highlight(destination)
            layers.setDestination(destination)

            // Destination
            if (destination != null) {
                navigationCompassLayer.setDestination(destination)
            } else if (direction != null) {
                navigationCompassLayer.setDestination(direction)
            } else {
                navigationCompassLayer.setDestination(null as MappableBearing?)
            }
        }
    }

    private fun showCalibrationDialog() {
        if (userPrefs.navigation.showCalibrationOnNavigateDialog) {
            dialog(
                getString(R.string.calibrate_compass_dialog_title),
                getString(
                    R.string.calibrate_compass_on_navigate_dialog_content,
                    getString(android.R.string.ok)
                ),
                contentView = CompassCalibrationView.sized(
                    requireContext(),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Resources.dp(requireContext(), 200f).toInt()
                ),
                cancelText = null,
                cancelOnOutsideTouch = false,
                scrollable = true
            )
        }
    }

    private fun updateSensorStatus() {
        inBackground {
            compassStatusBadge = compassStatusBadgeProvider.getBadge()
            gpsStatusBadge = gpsStatusBadgeProvider.getBadge()
        }
    }

    private fun updateNavigationButton() {
        binding.beaconBtn.setImageResource(
            if (destination != null) {
                R.drawable.ic_cancel
            } else {
                R.drawable.ic_beacon
            }
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityNavigatorBinding {
        return ActivityNavigatorBinding.inflate(layoutInflater, container, false)
    }

    companion object {
        const val LAST_DEST_BEARING = "last_dest_bearing"
        const val CACHE_CAMERA_ZOOM = "sighting_compass_camera_zoom"
        private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f).meters()
        private val ASTRONOMY_UPDATE_FREQUENCY = Duration.ofMinutes(1)
    }
}
