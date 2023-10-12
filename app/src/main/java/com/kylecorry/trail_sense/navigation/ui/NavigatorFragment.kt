package com.kylecorry.trail_sense.navigation.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.interval
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.ui.CompassCalibrationView
import com.kylecorry.trail_sense.calibration.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.diagnostics.GPSDiagnostic
import com.kylecorry.trail_sense.diagnostics.IDiagnostic
import com.kylecorry.trail_sense.diagnostics.MagnetometerDiagnostic
import com.kylecorry.trail_sense.diagnostics.status.GpsStatusBadgeProvider
import com.kylecorry.trail_sense.diagnostics.status.SensorStatusBadgeProvider
import com.kylecorry.trail_sense.diagnostics.status.StatusBadge
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.domain.CompassStyle
import com.kylecorry.trail_sense.navigation.domain.CompassStyleChooser
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.navigation.ui.data.UpdateAstronomyLayerCommand
import com.kylecorry.trail_sense.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.navigation.ui.layers.*
import com.kylecorry.trail_sense.navigation.ui.layers.compass.BeaconCompassLayer
import com.kylecorry.trail_sense.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.navigation.ui.layers.compass.MarkerCompassLayer
import com.kylecorry.trail_sense.navigation.ui.layers.compass.NavigationCompassLayer
import com.kylecorry.trail_sense.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.maps.infrastructure.layers.ILayerManager
import com.kylecorry.trail_sense.tools.maps.infrastructure.layers.MultiLayerManager
import com.kylecorry.trail_sense.tools.maps.infrastructure.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.tools.maps.infrastructure.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.maps.infrastructure.layers.PathLayerManager
import com.kylecorry.trail_sense.tools.maps.infrastructure.layers.TideLayerManager
import java.time.Duration
import java.time.Instant
import java.util.*


class NavigatorFragment : BoundFragment<ActivityNavigatorBinding>() {

    private var showSightingCompass = false
    private val compass by lazy { sensorService.getCompass() }
    private val gps by lazy { sensorService.getGPS(frequency = Duration.ofMillis(200)) }
    private var sightingCompass: SightingCompassView? = null
    private val orientation by lazy { sensorService.getDeviceOrientationSensor() }
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

    private lateinit var navController: NavController

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
    private var gpsStatusBadge: StatusBadge? = null
    private var compassStatusBadge: StatusBadge? = null

    // Diagnostics
    private val errors by lazy { NavigatorUserErrors(this) }
    private lateinit var diagnostics: List<IDiagnostic>

    // Data commands
    private val updateAstronomyLayerCommand by lazy {
        UpdateAstronomyLayerCommand(
            astronomyCompassLayer,
            userPrefs,
            gps
        ) { declination }
    }


    private var astronomyDataLoaded = false

    private var lastOrientation: DeviceOrientation.Orientation? = null

    private val loadBeaconsRunner = CoroutineQueueRunner()

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer()
    private val myLocationLayer = MyLocationLayer()
    private val myAccuracyLayer = MyAccuracyLayer()
    private val tideLayer = TideLayer()
    private var layerManager: ILayerManager? = null

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
    private val lockScreenPresence by lazy { userPrefs.navigation.lockScreenPresence }
    private val styleChooser by lazy { CompassStyleChooser(userPrefs.navigation, hasCompass) }
    private val useTrueNorth by lazy { userPrefs.compass.useTrueNorth }

    override fun onDestroyView() {
        super.onDestroyView()
        sightingCompass = null
        activity?.let {
            tryOrNothing {
                Screen.setShowWhenLocked(it, false)
            }
        }
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
                onMain { handleShowWhenLocked() }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diagnostics = listOf(
            GPSDiagnostic(requireContext(), null, gps),
            MagnetometerDiagnostic(requireContext(), viewLifecycleOwner)
        )

        sightingCompass = SightingCompassView(
            binding.viewCamera,
            binding.viewCameraLine
        )

        // Register timers
        interval(Duration.ofMinutes(1)) {
            updateAstronomyData()
        }

        interval(100) {
            updateCompassLayers()
        }

        interval(Duration.ofSeconds(1)) {
            updateSensorStatus()
        }


        // Initialize layers
        beaconLayer.setOutlineColor(Resources.color(requireContext(), R.color.colorSecondary))
        binding.radarCompass.setLayers(
            listOf(
                pathLayer,
                myAccuracyLayer,
                myLocationLayer,
                tideLayer,
                beaconLayer
            )
        )

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

        navController = findNavController()

        observe(compass) {}
        observe(orientation) { onOrientationUpdate() }
        observe(altimeter) { }
        observe(gps) { onLocationUpdate() }
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

        CustomUiUtils.setButtonState(binding.sightingCompassBtn, showSightingCompass)
        binding.sightingCompassBtn.setOnClickListener {
            setSightingCompassStatus(!showSightingCompass)
        }
        sightingCompass?.stop()

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
                navigator.cancelNavigation()
                updateNavigator()
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

        if (!hasCompass) {
            myLocationLayer.setShowDirection(false)
        }

        scheduleUpdates(INTERVAL_60_FPS)
    }

    private fun setSightingCompassStatus(isOn: Boolean) {
        showSightingCompass = isOn
        CustomUiUtils.setButtonState(binding.sightingCompassBtn, isOn)
        if (isOn) {
            requestCamera { hasPermission ->
                if (hasPermission) {
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
        sightingCompass?.start()

        if (sightingCompass?.isRunning() == true) {
            // TODO: Extract this logic to the flashlight (if camera is in use)
            if (userPrefs.navigation.rightButton == QuickActionType.Flashlight) {
                binding.navigationTitle.rightButton.isClickable = false
            }
            if (userPrefs.navigation.leftButton == QuickActionType.Flashlight) {
                binding.navigationTitle.leftButton.isClickable = false
            }
        }
    }

    private fun disableSightingCompass() {
        sightingCompass?.stop()
        if (userPrefs.navigation.rightButton == QuickActionType.Flashlight) {
            binding.navigationTitle.rightButton.isClickable = true
        }
        if (userPrefs.navigation.leftButton == QuickActionType.Flashlight) {
            binding.navigationTitle.leftButton.isClickable = true
        }
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

        handleShowWhenLocked()
    }

    private fun handleShowWhenLocked() {
        activity?.let {
            val shouldShow =
                isBound && lockScreenPresence && (destination != null || destinationBearing != null)
            tryOrNothing {
                Screen.setShowWhenLocked(it, shouldShow)
            }
        }
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
            astronomyDataLoaded = true
        }
    }

    override fun onResume() {
        super.onResume()
        lastOrientation = null

        layerManager = MultiLayerManager(
            listOf(
                PathLayerManager(requireContext(), pathLayer),
                MyAccuracyLayerManager(myAccuracyLayer, AppColor.Orange.color, 25),
                MyLocationLayerManager(myLocationLayer, Color.WHITE),
                TideLayerManager(requireContext(), tideLayer)
            )
        )
        if (useRadarCompass) {
            layerManager?.start()
        }

        // Populate the last known location
        layerManager?.onLocationChanged(gps.location, gps.horizontalAccuracy)

        // Resume navigation
        inBackground {
            destination = navigator.getDestination()
            if (destination != null) {
                onMain { handleShowWhenLocked() }
            }
        }

        val lastDestBearing = cache.getFloat(LAST_DEST_BEARING)
        if (lastDestBearing != null && hasCompass) {
            destinationBearing = lastDestBearing
        }

        updateDeclination()

        binding.beaconBtn.show()

        // Update the UI
        updateNavigator()
    }

    override fun onPause() {
        super.onPause()
        loadBeaconsRunner.cancel()
        sightingCompass?.stop()
        errors.reset()
        layerManager?.stop()
        layerManager = null
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

    private fun updateDeclination() {
        inBackground {
            onIO {
                declination = declinationProvider.getDeclination()
                compass.declination = declination
            }
        }
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            getPosition(),
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

        val selectedBeacon = getSelectedBeacon(nearbyBeacons)

        if (selectedBeacon != null) {
            binding.navigationSheet.show(
                getPosition(),
                selectedBeacon,
                declination,
                useTrueNorth
            )
        } else {
            binding.navigationSheet.hide()
        }

        gpsStatusBadge?.let {
            binding.gpsStatus.setStatusText(it.name)
            binding.gpsStatus.setBackgroundTint(it.color)
        }

        compassStatusBadge?.let {
            binding.compassStatus.setStatusText(it.name)
            binding.compassStatus.setBackgroundTint(it.color)
        }

        // Speed
        binding.speed.title = formatService.formatSpeed(speedometer.speed.speed)

        // Azimuth
        if (hasCompass) {
            val azimuthText = formatService.formatDegrees(compass.bearing.value, replace360 = true)
                .padStart(4, ' ')
            val directionText = formatService.formatDirection(compass.bearing.direction)
                .padStart(2, ' ')
            @SuppressLint("SetTextI18n")
            binding.navigationTitle.title.text = "$azimuthText   $directionText"
        } else {
            binding.navigationTitle.title.text = getString(R.string.dash)
        }
        // Altitude
        binding.altitude.title = formatService.formatDistance(
            Distance.meters(altimeter.altitude).convertTo(baseDistanceUnits)
        )

        layerManager?.onBearingChanged(compass.bearing)

        // Compass
        listOf<ICompassView>(
            binding.roundCompass,
            binding.radarCompass,
            binding.linearCompass
        ).forEach {
            it.azimuth = compass.bearing
            it.declination = declination
            it.compassCenter = gps.location
        }

        // Location
        binding.navigationTitle.subtitle.text = formatService.formatLocation(gps.location)

        updateNavigationButton()

        // show on lock screen
        if (lockScreenPresence && (destination != null || destinationBearing != null)) {
            activity?.let {
                tryOrNothing {
                    Screen.setShowWhenLocked(it, true)
                }
            }
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
            beaconLayer.setBeacons(nearbyBeacons)
            beaconCompassLayer.setBeacons(nearbyBeacons)
            beaconCompassLayer.highlight(destination)
            beaconLayer.highlight(destination)

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

    private fun getPosition(): Position {
        return Position(
            gps.location,
            altimeter.altitude,
            compass.bearing,
            speedometer.speed.speed
        )
    }

    private fun showCalibrationDialog() {
        if (userPrefs.navigation.showCalibrationOnNavigateDialog) {
            Alerts.dialog(
                requireContext(),
                getString(R.string.calibrate_compass_dialog_title),
                getString(
                    R.string.calibrate_compass_on_navigate_dialog_content,
                    getString(android.R.string.ok)
                ),
                contentView = CompassCalibrationView.withFrame(
                    requireContext(),
                    height = Resources.dp(requireContext(), 200f).toInt()
                ),
                cancelText = null,
                cancelOnOutsideTouch = false
            )
        }
    }

    private fun onOrientationUpdate(): Boolean {

        if (orientation.orientation == lastOrientation) {
            return true
        }

        lastOrientation = orientation.orientation

        val style = styleChooser.getStyle(orientation.orientation)

        binding.linearCompass.isInvisible = style != CompassStyle.Linear
        binding.sightingCompassBtn.isInvisible = style != CompassStyle.Linear
        binding.roundCompass.isInvisible = style != CompassStyle.Round
        binding.radarCompass.isInvisible = style != CompassStyle.Radar

        if (style == CompassStyle.Linear) {
            if (showSightingCompass && sightingCompass?.isRunning() == false) {
                enableSightingCompass()
            }
        } else {
            disableSightingCompass()
        }
        return true
    }

    private fun onLocationUpdate() {
        layerManager?.onLocationChanged(gps.location, gps.horizontalAccuracy)

        updateNearbyBeacons()
        updateDeclination()

        if (!astronomyDataLoaded) {
            updateAstronomyData()
        }

        if (useRadarCompass) {
            val loadGeofence = Geofence(
                gps.location,
                Distance.meters(nearbyDistance + 10)
            )
            val bounds = CoordinateBounds.from(loadGeofence)
            layerManager?.onBoundsChanged(bounds)
        }
    }

    private fun updateSensorStatus() {
        inBackground {
            compassStatusBadge = compassStatusBadgeProvider.getBadge()
            gpsStatusBadge = gpsStatusBadgeProvider.getBadge()

            val codes = onDefault {
                diagnostics.flatMap { it.scan() }
            }

            onMain {
                errors.update(codes)
            }

        }
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

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityNavigatorBinding {
        return ActivityNavigatorBinding.inflate(layoutInflater, container, false)
    }

    companion object {
        const val LAST_DEST_BEARING = "last_dest_bearing"
        const val CACHE_CAMERA_ZOOM = "sighting_compass_camera_zoom"
    }
}
