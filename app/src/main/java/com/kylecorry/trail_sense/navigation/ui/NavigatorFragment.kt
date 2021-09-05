package com.kylecorry.trail_sense.navigation.ui

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.asLiveData
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
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.quickactions.LowPowerQuickAction
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.paths.BacktrackPathSplitter
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.tools.backtrack.ui.QuickActionBacktrack
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trail_sense.tools.flashlight.ui.QuickActionFlashlight
import com.kylecorry.trail_sense.tools.maps.ui.QuickActionOfflineMaps
import com.kylecorry.trail_sense.tools.ruler.ui.QuickActionRuler
import com.kylecorry.trail_sense.tools.whistle.ui.QuickActionWhistle
import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.*


class NavigatorFragment : BoundFragment<ActivityNavigatorBinding>() {

    private var shownAccuracyToast = false
    private var sightingCompassInitialized = false
    private var sightingCompassActive = false
    private val compass by lazy { sensorService.getCompass() }
    private val gps by lazy { sensorService.getGPS() }
    private val camera by lazy {
        Camera(
            requireContext(),
            this,
            previewView = binding.viewCamera,
            analyze = false
        )
    }
    private val orientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val speedometer by lazy { sensorService.getSpeedometer() }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(userPrefs, gps) }

    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private lateinit var navController: NavController

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val backtrackRepo by lazy { WaypointRepo.getInstance(requireContext()) }

    private val sensorService by lazy { SensorService(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val throttle = Throttle(20)

    private val navigationService = NavigationService()
    private val astronomyService = AstronomyService()
    private val geoService = GeologyService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var beacons: Collection<Beacon> = listOf()
    private var backtrack: List<PathPoint>? = null
    private var nearbyBeacons: Collection<Beacon> = listOf()

    private var destination: Beacon? = null
    private var destinationBearing: Float? = null
    private var useTrueNorth = false

    private var leftQuickAction: QuickActionButton? = null
    private var rightQuickAction: QuickActionButton? = null

    private var isMoonUp = true
    private var isSunUp = true
    private var moonBearing = 0f
    private var sunBearing = 0f

    private var gpsErrorShown = false
    private var gpsTimeoutShown = false

    private var lastOrientation: DeviceOrientation.Orientation? = null

    private val astronomyIntervalometer = Timer {
        lifecycleScope.launch {
            updateAstronomyData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rightQuickAction?.onDestroy()
        leftQuickAction?.onDestroy()
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

        if (!Sensors.hasCompass(requireContext())) {
            requireMainActivity().errorBanner.report(
                UserError(
                    USER_ERROR_NO_COMPASS,
                    getString(R.string.no_compass_message),
                    R.drawable.ic_compass_icon
                )
            )
        }

        rightQuickAction = getQuickActionButton(
            userPrefs.navigation.rightQuickAction,
            binding.navigationRightQuickAction
        )
        leftQuickAction = getQuickActionButton(
            userPrefs.navigation.leftQuickAction,
            binding.navigationLeftQuickAction
        )

        beaconRepo.getBeacons().observe(viewLifecycleOwner) {
            beacons = it.map { it.toBeacon() }
            nearbyBeacons = getNearbyBeacons()
            updateUI()
        }

        backtrackRepo.getWaypoints().observe(viewLifecycleOwner) {
            val waypoints = it.filter {
                it.createdInstant > Instant.now().minus(userPrefs.navigation.backtrackHistory)
            }.sortedByDescending { it.createdInstant }
            backtrack = waypoints.map { it.toPathPoint() }
            updateUI()
        }

        rightQuickAction?.onCreate()
        leftQuickAction?.onCreate()
        navController = findNavController()

        compass.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        orientation.asLiveData().observe(viewLifecycleOwner, { onOrientationUpdate() })
        altimeter.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        gps.asLiveData().observe(viewLifecycleOwner, { onLocationUpdate() })
        speedometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })

        binding.location.setOnLongClickListener {
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

        binding.location.setOnClickListener {
            val sheet = LocationBottomSheet()
            sheet.gps = gps
            sheet.show(this)
        }

        binding.altitudeHolder.setOnClickListener {
            val sheet = AltitudeBottomSheet()
            sheet.backtrackPoints = backtrack
            sheet.currentAltitude = AltitudeReading(Instant.now(), altimeter.altitude)
            sheet.show(this)
        }

        CustomUiUtils.setButtonState(binding.sightingCompassBtn, sightingCompassActive)
        binding.sightingCompassBtn.setOnClickListener {
            setSightingCompassStatus(!sightingCompassActive)
        }

        binding.viewCamera.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.viewCameraLine.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.zoomRatioSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoom = (progress / 100f).coerceIn(0f, 1f)
                camera.setZoom(zoom)
                cache.putFloat(CACHE_CAMERA_ZOOM, zoom)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
                    "initial_location" to MyNamedCoordinate(
                        gps.location,
                        elevation = if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
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
        sightingCompassActive = isOn
        CustomUiUtils.setButtonState(binding.sightingCompassBtn, isOn)
        if (isOn) {
            requestPermissions(
                listOf(Manifest.permission.CAMERA)
            ) {
                if (Camera.isAvailable(requireContext())) {
                    enableSightingCompass()
                } else {
                    Alerts.toast(
                        requireContext(),
                        getString(R.string.camera_permission_denied),
                        short = false
                    )
                    setSightingCompassStatus(false)
                }
            }
        } else {
            disableSightingCompass()
        }
    }

    private fun enableSightingCompass() {
        if (!Camera.isAvailable(requireContext())) {
            return
        }
        sightingCompassInitialized = true

        camera.start(this::onCameraUpdate)
        binding.viewCameraLine.isVisible = true
        binding.viewCamera.isVisible = true
        binding.zoomRatioSeekbar.isVisible = true

        // TODO: Extract this logic to the flashlight (if camera is in use)
        if (userPrefs.navigation.rightQuickAction == QuickActionType.Flashlight) {
            binding.navigationRightQuickAction.isClickable = false
        }
        if (userPrefs.navigation.leftQuickAction == QuickActionType.Flashlight) {
            binding.navigationLeftQuickAction.isClickable = false
        }
        val handler = FlashlightHandler.getInstance(requireContext())
        handler.off()
    }

    private fun onCameraUpdate(): Boolean {
        val zoom = cache.getFloat(CACHE_CAMERA_ZOOM) ?: 0.5f
        camera.setZoom(zoom)
        return true
    }

    private fun disableSightingCompass() {
        camera.stop(this::onCameraUpdate)
        binding.viewCameraLine.isVisible = false
        binding.viewCamera.isVisible = false
        binding.zoomRatioSeekbar.isVisible = false
        if (userPrefs.navigation.rightQuickAction == QuickActionType.Flashlight) {
            binding.navigationRightQuickAction.isClickable = true
        }
        if (userPrefs.navigation.leftQuickAction == QuickActionType.Flashlight) {
            binding.navigationLeftQuickAction.isClickable = true
        }
        sightingCompassInitialized = false
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

    private fun getIndicators(): List<BearingIndicator> {
        // TODO: Don't create this on every update
        val indicators = mutableListOf<BearingIndicator>()
        if (userPrefs.astronomy.showOnCompass) {
            val showWhenDown = userPrefs.astronomy.showOnCompassWhenDown

            if (isSunUp) {
                indicators.add(BearingIndicator(sunBearing, R.drawable.ic_sun))
            } else if (!isSunUp && showWhenDown) {
                indicators.add(BearingIndicator(sunBearing, R.drawable.ic_sun, opacity = 0.5f))
            }

            if (isMoonUp) {
                indicators.add(BearingIndicator(moonBearing, getMoonImage()))
            } else if (!isMoonUp && showWhenDown) {
                indicators.add(BearingIndicator(moonBearing, getMoonImage(), opacity = 0.5f))
            }
        }

        if (destination != null) {
            indicators.add(
                BearingIndicator(
                    transformTrueNorthBearing(
                        gps.location.bearingTo(
                            destination!!.coordinate
                        ).value
                    ), R.drawable.ic_arrow_target,
                    distance = Distance.meters(gps.location.distanceTo(destination!!.coordinate)),
                    tint = destination!!.color
                )
            )
            return indicators
        }

        if (destinationBearing != null) {
            indicators.add(
                BearingIndicator(
                    destinationBearing!!,
                    R.drawable.ic_arrow_target,
                    Resources.color(requireContext(), R.color.colorAccent)
                )
            )
        }

        val nearby = nearbyBeacons
        for (beacon in nearby) {
            indicators.add(
                BearingIndicator(
                    transformTrueNorthBearing(gps.location.bearingTo(beacon.coordinate).value),
                    R.drawable.ic_arrow_target,
                    distance = Distance.meters(gps.location.distanceTo(beacon.coordinate)),
                    tint = beacon.color
                )
            )
        }

        return indicators
    }

    override fun onResume() {
        super.onResume()
        rightQuickAction?.onResume()
        leftQuickAction?.onResume()
        lastOrientation = null
        astronomyIntervalometer.interval(Duration.ofMinutes(1))
        useTrueNorth = userPrefs.navigation.useTrueNorth

        // Resume navigation
        val lastBeaconId = cache.getLong(LAST_BEACON_ID)
        if (lastBeaconId != null) {
            lifecycleScope.launch {
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
        binding.roundCompass.visibility =
            if (userPrefs.navigation.useRadarCompass) View.INVISIBLE else View.VISIBLE
        binding.radarCompass.visibility =
            if (userPrefs.navigation.useRadarCompass) View.VISIBLE else View.INVISIBLE

        // Update the UI
        updateNavigator()
    }

    override fun onPause() {
        super.onPause()
        sightingCompassInitialized = false
        camera.stop(this::onCameraUpdate)
        rightQuickAction?.onPause()
        leftQuickAction?.onPause()
        astronomyIntervalometer.stop()
        requireMainActivity().errorBanner.dismiss(USER_ERROR_COMPASS_POOR)
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
        return transformTrueNorthBearing(astronomyService.getSunAzimuth(gps.location).value)
    }

    private fun getMoonBearing(): Float {
        return transformTrueNorthBearing(astronomyService.getMoonAzimuth(gps.location).value)
    }

    private fun getDestinationBearing(): Float? {
        val destLocation = destination?.coordinate
        return when {
            destLocation != null -> {
                transformTrueNorthBearing(gps.location.bearingTo(destLocation).value)
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

    private fun transformTrueNorthBearing(bearing: Float): Float {
        return if (useTrueNorth) {
            bearing
        } else {
            Bearing.getBearing(bearing - getDeclination())
        }
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

        if ((compass.quality == Quality.Poor || compass.quality == Quality.Moderate) && !shownAccuracyToast) {
            val banner = requireMainActivity().errorBanner
            banner.report(
                UserError(
                    USER_ERROR_COMPASS_POOR,
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
        } else if (compass.quality == Quality.Good) {
            requireMainActivity().errorBanner.dismiss(USER_ERROR_COMPASS_POOR)
        }

        // Speed
        binding.speed.text = formatService.formatSpeed(speedometer.speed.speed)

        // Azimuth
        binding.compassAzimuth.text = formatService.formatDegrees(compass.bearing.value)
        binding.compassDirection.text = formatService.formatDirection(compass.bearing.direction)

        // Compass
        val indicators = getIndicators()
        val destBearing = getDestinationBearing()
        val destColor = if (destination != null) destination!!.color else Resources.color(
            requireContext(),
            R.color.colorAccent
        )
        binding.roundCompass.setIndicators(indicators)
        binding.roundCompass.setAzimuth(compass.rawBearing)
        binding.roundCompass.setDestination(destBearing, destColor)
        binding.radarCompass.setIndicators(indicators)
        binding.radarCompass.setAzimuth(compass.rawBearing)
        binding.radarCompass.setDeclination(getDeclination())
        binding.radarCompass.setLocation(gps.location)
        val bt = backtrack
        if (userPrefs.navigation.showBacktrackPath && bt != null) {
            val isTracking = BacktrackScheduler.isOn(requireContext())
            val currentPathId = cache.getLong(getString(R.string.pref_last_backtrack_path_id))

            val points = if (isTracking && currentPathId != null) {
                bt + listOf(gps.getPathPoint(currentPathId))
            } else {
                bt
            }.sortedByDescending { it.time }

            val paths = BacktrackPathSplitter(userPrefs).split(points)

            binding.radarCompass.setPaths(paths)
        }
        binding.radarCompass.setDestination(destBearing, destColor)
        binding.linearCompass.setIndicators(indicators)
        binding.linearCompass.setAzimuth(compass.rawBearing)
        binding.linearCompass.setDestination(destBearing, destColor)

        // Altitude
        binding.altitude.text = formatService.formatDistance(
            Distance.meters(altimeter.altitude).convertTo(userPrefs.baseDistanceUnits)
        )

        // Location
        binding.location.text = formatService.formatLocation(gps.location)

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

    private fun shouldShowLinearCompass(): Boolean {
        return userPrefs.navigation.showLinearCompass && orientation.orientation == DeviceOrientation.Orientation.Portrait
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

        if (shouldShowLinearCompass()) {
            binding.linearCompass.visibility = View.VISIBLE
            if (sightingCompassActive && !sightingCompassInitialized) {
                enableSightingCompass()
            }
            binding.sightingCompassBtn.isVisible = true
            binding.roundCompass.visibility = View.INVISIBLE
            binding.radarCompass.visibility = View.INVISIBLE
        } else {
            binding.linearCompass.visibility = View.INVISIBLE
            binding.sightingCompassBtn.isVisible = false
            disableSightingCompass()
            binding.roundCompass.visibility =
                if (userPrefs.navigation.useRadarCompass) View.INVISIBLE else View.VISIBLE
            binding.radarCompass.visibility =
                if (userPrefs.navigation.useRadarCompass) View.VISIBLE else View.INVISIBLE
        }
        updateUI()
        return true
    }

    private fun onLocationUpdate() {

        if (gps is CustomGPS && !(gps as CustomGPS).isTimedOut) {
            gpsTimeoutShown = false
            requireMainActivity().errorBanner.dismiss(USER_ERROR_GPS_TIMEOUT)
        }

        nearbyBeacons = getNearbyBeacons()
        compass.declination = getDeclination()

        if (sunBearing == 0f) {
            lifecycleScope.launch {
                updateAstronomyData()
            }
        }

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
                USER_ERROR_GPS_NOT_SET,
                getString(R.string.location_not_set),
                R.drawable.satellite,
                getString(R.string.set)
            ) {
                activity.errorBanner.dismiss(USER_ERROR_GPS_NOT_SET)
                navController.navigate(R.id.calibrateGPSFragment)
            }
            activity.errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CachedGPS && gps.location == Coordinate.zero && !gpsErrorShown) {
            val error = UserError(
                USER_ERROR_NO_GPS,
                getString(R.string.location_disabled),
                R.drawable.satellite
            )
            requireMainActivity().errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CustomGPS && (gps as CustomGPS).isTimedOut && !gpsTimeoutShown) {
            val error = UserError(
                USER_ERROR_GPS_TIMEOUT,
                getString(R.string.gps_signal_lost),
                R.drawable.satellite
            )
            requireMainActivity().errorBanner.report(error)
            gpsTimeoutShown = true
        }
    }

    private fun getQuickActionButton(
        type: QuickActionType,
        button: FloatingActionButton
    ): QuickActionButton {
        return when (type) {
            QuickActionType.None -> QuickActionNone(button, this)
            QuickActionType.Backtrack -> QuickActionBacktrack(button, this)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, this)
            QuickActionType.Ruler -> QuickActionRuler(button, this, binding.ruler)
            QuickActionType.Maps -> QuickActionOfflineMaps(button, this)
            QuickActionType.Whistle -> QuickActionWhistle(button, this)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, this)
            else -> QuickActionNone(button, this)
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
