package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.domain.CompassStyle
import com.kylecorry.trail_sense.navigation.domain.CompassStyleChooser
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.PathLoader
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.ui.asMappable
import com.kylecorry.trail_sense.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.UserError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.*


class NavigatorFragment : BoundFragment<ActivityNavigatorBinding>() {

    private var shownAccuracyToast = false
    private var showSightingCompass = false
    private val compass by lazy { sensorService.getCompass() }
    private val gps by lazy { sensorService.getGPS(frequency = Duration.ofMillis(200)) }
    private val sightingCompass by lazy {
        SightingCompassView(
            binding.viewCamera,
            binding.viewCameraLine
        )
    }
    private val orientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val speedometer by lazy { sensorService.getSpeedometer() }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(userPrefs, gps) }

    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private lateinit var navController: NavController

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val pathService by lazy { PathService.getInstance(requireContext()) }

    private val sensorService by lazy { SensorService(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val throttle = Throttle(20)

    private val navigationService = NavigationService()
    private val astronomyService = AstronomyService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var beacons: Collection<Beacon> = listOf()
    private var paths: List<Path> = emptyList()
    private var currentBacktrackPathId: Long? = null
    private var nearbyBeacons: Collection<Beacon> = listOf()

    private var destination: Beacon? = null
    private var destinationBearing: Float? = null
    private var useTrueNorth = false

    private var isMoonUp = true
    private var isSunUp = true
    private var moonBearing = 0f
    private var sunBearing = 0f

    private var gpsErrorShown = false
    private var gpsTimeoutShown = false

    private var lastOrientation: DeviceOrientation.Orientation? = null

    private val pathLoader by lazy { PathLoader(pathService) }

    private val loadPathRunner = ControlledRunner<Unit>()

    private val astronomyIntervalometer = Timer {
        lifecycleScope.launch {
            updateAstronomyData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.let {
            tryOrNothing {
                Screen.setShowWhenLocked(it, false)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("destination") ?: 0L

        if (beaconId != 0L) {
            showCalibrationDialog()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    destination = beaconRepo.getBeacon(beaconId)?.toBeacon()
                    cache.putLong(LAST_BEACON_ID, beaconId)
                }
                withContext(Dispatchers.Main) {
                    handleShowWhenLocked()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.speed.setShowDescription(false)
        binding.altitude.setShowDescription(false)

        if (!Sensors.hasCompass(requireContext())) {
            requireMainActivity().errorBanner.report(
                UserError(
                    ErrorBannerReason.NoCompass,
                    getString(R.string.no_compass_message),
                    R.drawable.ic_compass_icon
                )
            )
        }

        NavigationQuickActionBinder(
            this,
            binding,
            userPrefs.navigation
        ).bind()

        beaconRepo.getBeacons().observe(viewLifecycleOwner) {
            beacons = it.map { it.toBeacon() }
            nearbyBeacons = getNearbyBeacons()
            updateUI()
        }

        pathService.getLivePaths().observe(viewLifecycleOwner) {
            paths = it.filter { path -> path.style.visible }
            runInBackground {
                withContext(Dispatchers.IO) {
                    currentBacktrackPathId = pathService.getBacktrackPathId()
                    updateCompassPaths(true)
                }
                withContext(Dispatchers.Main) {
                    updateUI()
                }
            }

        }

        navController = findNavController()

        compass.asLiveData().observe(viewLifecycleOwner
        ) { updateUI() }
        orientation.asLiveData().observe(viewLifecycleOwner
        ) { onOrientationUpdate() }
        altimeter.asLiveData().observe(viewLifecycleOwner
        ) { updateUI() }
        gps.asLiveData().observe(viewLifecycleOwner
        ) { onLocationUpdate() }
        speedometer.asLiveData().observe(viewLifecycleOwner
        ) { updateUI() }

        binding.navigationTitle.subtitle.setOnLongClickListener {
            // TODO: Show custom share sheet instead
            Pickers.menu(it, R.menu.location_share_menu) { menuItem ->
                val sender = when (menuItem) {
                    R.id.action_send -> LocationSharesheet(requireContext())
                    R.id.action_maps -> LocationGeoSender(requireContext())
                    else -> LocationCopy(requireContext())
                }
                sender.send(gps.location)
                true
            }
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

        CustomUiUtils.setButtonState(binding.sightingCompassBtn, showSightingCompass)
        binding.sightingCompassBtn.setOnClickListener {
            setSightingCompassStatus(!showSightingCompass)
        }
        sightingCompass.stop()

        binding.viewCamera.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.viewCameraLine.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.beaconBtn.setOnClickListener {
            if (destination == null) {
                navController.navigate(R.id.action_navigatorFragment_to_beaconListFragment)
            } else {
                destination = null
                cache.remove(LAST_BEACON_ID)
                updateNavigator()
            }
        }

        binding.beaconBtn.setOnLongClickListener {
            if (gps.hasValidReading) {
                val bundle = bundleOf(
                    "initial_location" to GeoUri(
                        gps.location,
                        null,
                        mapOf("ele" to (if (altimeter.hasValidReading) altimeter.altitude else gps.altitude).toString())
                    )
                )
                navController.navigate(R.id.action_navigatorFragment_to_beaconListFragment, bundle)
            } else {
                navController.navigate(R.id.action_navigatorFragment_to_beaconListFragment)

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
    }

    private fun setSightingCompassStatus(isOn: Boolean) {
        showSightingCompass = isOn
        CustomUiUtils.setButtonState(binding.sightingCompassBtn, isOn)
        if (isOn) {
            requestCamera { hasPermission ->
                if (hasPermission){
                    enableSightingCompass()
                } else {
                    alertNoCameraPermission()
                    setSightingCompassStatus(false)
                }
            }
        } else {
            disableSightingCompass()
        }
    }

    private fun enableSightingCompass() {
        sightingCompass.start()

        if (sightingCompass.isRunning()) {
            // TODO: Extract this logic to the flashlight (if camera is in use)
            if (userPrefs.navigation.rightQuickAction == QuickActionType.Flashlight) {
                binding.navigationTitle.rightQuickAction.isClickable = false
            }
            if (userPrefs.navigation.leftQuickAction == QuickActionType.Flashlight) {
                binding.navigationTitle.leftQuickAction.isClickable = false
            }
        }
    }

    private fun disableSightingCompass() {
        sightingCompass.stop()
        if (userPrefs.navigation.rightQuickAction == QuickActionType.Flashlight) {
            binding.navigationTitle.rightQuickAction.isClickable = true
        }
        if (userPrefs.navigation.leftQuickAction == QuickActionType.Flashlight) {
            binding.navigationTitle.leftQuickAction.isClickable = true
        }
    }

    private fun toggleDestinationBearing() {
        if (destination != null) {
            // Don't set destination bearing while navigating
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

        handleShowWhenLocked()
    }

    private fun handleShowWhenLocked() {
        activity?.let {
            val shouldShow =
                isBound && userPrefs.navigation.lockScreenPresence && (destination != null || destinationBearing != null)
            tryOrNothing {
                Screen.setShowWhenLocked(it, shouldShow)
            }
        }
    }

    private fun displayAccuracyTips() {
        context ?: return

        val gpsHorizontalAccuracy = gps.horizontalAccuracy
        val gpsVerticalAccuracy = gps.verticalAccuracy

        val gpsHAccuracyStr =
            if (gpsHorizontalAccuracy == null) getString(R.string.accuracy_distance_unknown) else getString(
                R.string.accuracy_distance_format,
                formatService.formatDistance(
                    Distance.meters(gpsHorizontalAccuracy).convertTo(userPrefs.baseDistanceUnits)
                )
            )
        val gpsVAccuracyStr =
            if (gpsVerticalAccuracy == null) getString(R.string.accuracy_distance_unknown) else getString(
                R.string.accuracy_distance_format,
                formatService.formatDistance(
                    Distance.meters(gpsVerticalAccuracy).convertTo(userPrefs.baseDistanceUnits)
                )
            )

        Alerts.dialog(
            requireContext(),
            getString(R.string.accuracy_info_title),
            getString(
                R.string.accuracy_info,
                gpsHAccuracyStr,
                gpsVAccuracyStr,
                gps.satellites.toString()
            ),
            cancelText = null
        )
    }

    private suspend fun updateAstronomyData() {
        if (gps.location == Coordinate.zero) {
            return
        }

        withContext(Dispatchers.Default) {
            isMoonUp = astronomyService.isMoonUp(gps.location)
            isSunUp = astronomyService.isSunUp(gps.location)
            sunBearing = getSunBearing()
            moonBearing = getMoonBearing()
        }
    }

    private fun getReferencePoints(): List<IMappableReferencePoint> {
        val references = mutableListOf<IMappableReferencePoint>()
        if (userPrefs.astronomy.showOnCompass) {
            val showWhenDown = userPrefs.astronomy.showOnCompassWhenDown

            if (isSunUp) {
                references.add(MappableReferencePoint(1, R.drawable.ic_sun, Bearing(sunBearing)))
            } else if (!isSunUp && showWhenDown) {
                references.add(
                    MappableReferencePoint(
                        1,
                        R.drawable.ic_sun,
                        Bearing(sunBearing),
                        opacity = 0.5f
                    )
                )
            }

            if (isMoonUp) {
                references.add(MappableReferencePoint(2, getMoonImage(), Bearing(moonBearing)))
            } else if (!isMoonUp && showWhenDown) {
                references.add(
                    MappableReferencePoint(
                        2,
                        getMoonImage(),
                        Bearing(moonBearing),
                        opacity = 0.5f
                    )
                )
            }
        }
        return references
    }

    override fun onResume() {
        super.onResume()
        lastOrientation = null
        astronomyIntervalometer.interval(Duration.ofMinutes(1))
        useTrueNorth = userPrefs.navigation.useTrueNorth

        // Resume navigation
        val lastBeaconId = cache.getLong(LAST_BEACON_ID)
        if (lastBeaconId != null) {
            runInBackground {
                withContext(Dispatchers.IO) {
                    destination = beaconRepo.getBeacon(lastBeaconId)?.toBeacon()
                }
                withContext(Dispatchers.Main) {
                    handleShowWhenLocked()
                }
            }
        }

        val lastDestBearing = cache.getFloat(LAST_DEST_BEARING)
        if (lastDestBearing != null) {
            destinationBearing = lastDestBearing
        }

        compass.declination = getDeclination()

        binding.beaconBtn.show()
        binding.roundCompass.isInvisible = userPrefs.navigation.useRadarCompass
        binding.radarCompass.isInvisible = !userPrefs.navigation.useRadarCompass

        // Update the UI
        updateNavigator()
    }

    override fun onPause() {
        super.onPause()
        loadPathRunner.cancel()
        sightingCompass.stop()
        astronomyIntervalometer.stop()
        requireMainActivity().errorBanner.dismiss(ErrorBannerReason.CompassPoor)
        shownAccuracyToast = false
        gpsErrorShown = false
    }

    private fun getNearbyBeacons(): Collection<Beacon> {

        if (!userPrefs.navigation.showMultipleBeacons) {
            return listOf()
        }

        return navigationService.getNearbyBeacons(
            gps.location,
            beacons,
            userPrefs.navigation.numberOfVisibleBeacons,
            8f,
            userPrefs.navigation.maxBeaconDistance
        )
    }

    private fun getSunBearing(): Float {
        return fromTrueNorth(astronomyService.getSunAzimuth(gps.location).value)
    }

    private fun getMoonBearing(): Float {
        return fromTrueNorth(astronomyService.getMoonAzimuth(gps.location).value)
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
        return DeclinationUtils.fromTrueNorthBearing(bearing, getDeclination())
    }

    private fun getDeclination(): Float {
        return declination.getDeclination()
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            getPosition(),
            nearby,
            getDeclination(),
            useTrueNorth
        )
    }

    private fun updateUI() {

        if (throttle.isThrottled() || !isBound) {
            return
        }

        val selectedBeacon = getSelectedBeacon(nearbyBeacons)

        if (selectedBeacon != null) {
            binding.navigationSheet.show(
                getPosition(),
                selectedBeacon,
                getDeclination(),
                useTrueNorth
            )
        } else {
            binding.navigationSheet.hide()
        }

        detectAndShowGPSError()
        binding.gpsStatus.setStatusText(getGPSStatus())
        binding.gpsStatus.setBackgroundTint(getGPSColor())
        binding.compassStatus.setStatusText(formatService.formatQuality(compass.quality))
        binding.compassStatus.setBackgroundTint(getCompassColor())

        if ((compass.quality == Quality.Poor) && !shownAccuracyToast) {
            val banner = requireMainActivity().errorBanner
            banner.report(
                UserError(
                    ErrorBannerReason.CompassPoor,
                    getString(
                        R.string.compass_calibrate_toast, formatService.formatQuality(
                            compass.quality
                        ).lowercase(Locale.getDefault())
                    ),
                    R.drawable.ic_compass_icon,
                    getString(R.string.how)
                ) {
                    displayAccuracyTips()
                    banner.hide()
                })
            shownAccuracyToast = true
        } else if (compass.quality == Quality.Good || compass.quality == Quality.Moderate) {
            requireMainActivity().errorBanner.dismiss(ErrorBannerReason.CompassPoor)
        }

        // Speed
        binding.speed.title = formatService.formatSpeed(speedometer.speed.speed)

        // Azimuth
        binding.navigationTitle.title.text =
            formatService.formatDegrees(compass.bearing.value, replace360 = true)
                .padStart(4, ' ') + "   " + formatService.formatDirection(compass.bearing.direction)
                .padStart(2, ' ')

        // Compass
        updateCompassView()

        // Altitude
        binding.altitude.title = formatService.formatDistance(
            Distance.meters(altimeter.altitude).convertTo(userPrefs.baseDistanceUnits)
        )

        // Location
        binding.navigationTitle.subtitle.text = formatService.formatLocation(gps.location)

        updateNavigationButton()

        // show on lock screen
        if (userPrefs.navigation.lockScreenPresence && (destination != null || destinationBearing != null)) {
            activity?.let {
                tryOrNothing {
                    Screen.setShowWhenLocked(it, true)
                }
            }
        }
    }

    private fun updateCompassView() {
        val destBearing = getDestinationBearing()
        val destination = destination
        val destColor = destination?.color ?: Resources.color(
            requireContext(),
            R.color.colorAccent
        )

        val direction = destBearing?.let {
            MappableBearing(
                Bearing(it),
                destColor
            )
        }

        val references = getReferencePoints()
        val declination = getDeclination()
        val nearby = nearbyBeacons.toList()

        // TODO: Only update the current compass
        val compasses = listOf<INearbyCompassView>(
            binding.roundCompass,
            binding.radarCompass,
            binding.linearCompass
        )

        compasses.forEach {
            it.setAzimuth(compass.bearing)
            it.setDeclination(declination)
            it.setLocation(gps.location)
            it.showLocations(nearby)
            it.showReferences(references)
            it.showDirection(direction)
            it.highlightLocation(destination)
        }
    }

    private fun updateCompassPaths(reload: Boolean = false) {
        runInBackground {
            loadPathRunner.joinPreviousOrRun {
                val mappablePaths = withContext(Dispatchers.IO) {
                    val loadGeofence = Geofence(
                        gps.location,
                        Distance.meters(userPrefs.navigation.maxBeaconDistance + 10)
                    )
                    val load = CoordinateBounds.from(loadGeofence)

                    val unloadGeofence =
                        loadGeofence.copy(radius = Distance.meters(loadGeofence.radius.distance + 1000))
                    val unload = CoordinateBounds.from(unloadGeofence)

                    pathLoader.update(paths, load, unload, reload)

                    val isTracking = BacktrackScheduler.isOn(requireContext())
                    val mappablePaths = mutableListOf<IMappablePath>()
                    val currentPathId = currentBacktrackPathId
                    for (points in pathLoader.points) {
                        val path = paths.firstOrNull { it.id == points.key } ?: continue
                        val pts = if (isTracking && currentPathId == path.id) {
                            listOf(gps.getPathPoint(currentPathId)) + points.value
                        } else {
                            points.value
                        }
                        mappablePaths.add(pts.asMappable(requireContext(), path))
                    }
                    mappablePaths
                }

                withContext(Dispatchers.Main) {
                    if (isBound) {
                        binding.radarCompass.showPaths(mappablePaths)
                    }
                }
            }
        }


    }

    private fun getPosition(): Position {
        return Position(gps.location, altimeter.altitude, compass.bearing, speedometer.speed.speed)
    }

    private fun showCalibrationDialog() {
        if (userPrefs.navigation.showCalibrationOnNavigateDialog) {
            Alerts.dialog(
                requireContext(), getString(R.string.calibrate_compass_dialog_title), getString(
                    R.string.calibrate_compass_on_navigate_dialog_content,
                    getString(android.R.string.ok)
                ), cancelText = null
            )
        }
    }

    private fun onOrientationUpdate(): Boolean {

        if (orientation.orientation == lastOrientation) {
            return true
        }

        lastOrientation = orientation.orientation

        val style = CompassStyleChooser(userPrefs.navigation).getStyle(orientation.orientation)

        binding.linearCompass.isInvisible = style != CompassStyle.Linear
        binding.sightingCompassBtn.isInvisible = style != CompassStyle.Linear
        binding.roundCompass.isInvisible = style != CompassStyle.Round
        binding.radarCompass.isInvisible = style != CompassStyle.Radar

        if (style == CompassStyle.Linear) {
            if (showSightingCompass && !sightingCompass.isRunning()) {
                enableSightingCompass()
            }
        } else {
            disableSightingCompass()
        }
        updateUI()
        return true
    }

    private fun onLocationUpdate() {

        if (gps is CustomGPS && !(gps as CustomGPS).isTimedOut) {
            gpsTimeoutShown = false
            requireMainActivity().errorBanner.dismiss(ErrorBannerReason.GPSTimeout)
        }

        nearbyBeacons = getNearbyBeacons()
        compass.declination = getDeclination()

        if (sunBearing == 0f) {
            lifecycleScope.launch {
                updateAstronomyData()
            }
        }

        updateCompassPaths()

        updateUI()
    }

    private fun updateNavigationButton() {
        if (destination != null) {
            binding.beaconBtn.setImageResource(R.drawable.ic_cancel)
        } else {
            binding.beaconBtn.setImageResource(R.drawable.ic_beacon)
        }
    }

    private fun updateNavigator() {
        handleShowWhenLocked()
        onLocationUpdate()
        updateNavigationButton()
    }

    @ColorInt
    private fun getCompassColor(): Int {
        return CustomUiUtils.getQualityColor(compass.quality)
    }

    @ColorInt
    private fun getGPSColor(): Int {
        if (gps is OverrideGPS) {
            return Resources.color(requireContext(), R.color.green)
        }

        if (gps is CachedGPS || !GPS.isAvailable(requireContext())) {
            return Resources.color(requireContext(), R.color.red)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return Resources.color(requireContext(), R.color.yellow)
        }

        if (!gps.hasValidReading || (userPrefs.requiresSatellites && gps.satellites < 4) || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)) {
            return Resources.color(requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(gps.quality)
    }

    private fun getGPSStatus(): String {
        if (gps is OverrideGPS) {
            return getString(R.string.gps_user)
        }

        if (gps is CachedGPS || !GPS.isAvailable(requireContext())) {
            return getString(R.string.unavailable)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return getString(R.string.gps_stale)
        }

        if (!gps.hasValidReading || (userPrefs.requiresSatellites && gps.satellites < 4) || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)) {
            return getString(R.string.gps_searching)
        }

        return formatService.formatQuality(gps.quality)
    }

    private fun detectAndShowGPSError() {
        if (gps is OverrideGPS && gps.location == Coordinate.zero && !gpsErrorShown) {
            val activity = requireMainActivity()
            val navController = findNavController()
            val error = UserError(
                ErrorBannerReason.LocationNotSet,
                getString(R.string.location_not_set),
                R.drawable.satellite,
                getString(R.string.set)
            ) {
                activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
                navController.navigate(R.id.calibrateGPSFragment)
            }
            activity.errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CachedGPS && gps.location == Coordinate.zero && !gpsErrorShown) {
            val error = UserError(
                ErrorBannerReason.NoGPS,
                getString(R.string.location_disabled),
                R.drawable.satellite
            )
            requireMainActivity().errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CustomGPS && (gps as CustomGPS).isTimedOut && !gpsTimeoutShown) {
            val error = UserError(
                ErrorBannerReason.GPSTimeout,
                getString(R.string.gps_signal_lost),
                R.drawable.satellite
            )
            requireMainActivity().errorBanner.report(error)
            gpsTimeoutShown = true
        }
    }

    @DrawableRes
    private fun getMoonImage(): Int {
        return MoonPhaseImageMapper().getPhaseImage(astronomyService.getCurrentMoonPhase().phase)
    }

    companion object {
        const val LAST_BEACON_ID = "last_beacon_id_long"
        const val LAST_DEST_BEARING = "last_dest_bearing"
        const val CACHE_CAMERA_ZOOM = "sighting_compass_camera_zoom"
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityNavigatorBinding {
        return ActivityNavigatorBinding.inflate(layoutInflater, container, false)
    }

}
