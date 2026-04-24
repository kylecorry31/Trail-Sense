package com.kylecorry.trail_sense.tools.navigation.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.ui.setTextDistinct
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyle
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyleChooser
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.tools.navigation.ui.commands.CreateBeaconHereCommand
import com.kylecorry.trail_sense.tools.navigation.ui.commands.OpenBeaconsCommand
import com.kylecorry.trail_sense.tools.navigation.ui.commands.ShareLocationCommand
import com.kylecorry.trail_sense.tools.navigation.ui.commands.ShowAltitudeSheetCommand
import com.kylecorry.trail_sense.tools.navigation.ui.commands.ShowLocationSheetCommand
import com.kylecorry.trail_sense.tools.navigation.ui.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.navigation.ui.managers.CompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.managers.MapLayerSheetManager
import com.kylecorry.trail_sense.tools.navigation.ui.managers.NavigationCompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.managers.NorthReferenceBadgeManager
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import java.time.Duration

class ToolNavigationFragment : BoundFragment<ActivityNavigatorBinding>() {

    // Sensors
    private val sensorService by lazy { SensorService(requireContext()) }
    private val orientation by lazy { sensorService.getOrientation() }
    private val compass by lazy { sensorService.getCompass(orientation) }
    private val gps by lazy { sensorService.getGPS(frequency = Duration.ofMillis(200)) }
    private val clinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val altimeter by lazy { sensorService.getAltimeter(gps = gps) }
    private val speedometer by lazy { sensorService.getSpeedometer(gps = gps) }
    private val hasCompass by lazy { sensorService.hasCompass() }

    // Declination
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            userPrefs,
            gps
        )
    }
    private var declination = 0f


    // Beacons
    private val beaconService = getAppService<BeaconService>()
    private var beacons: Collection<Beacon> = listOf()
    private var nearbyBeacons: List<Beacon> = listOf()

    // Navigation
    private var destination: Destination? = null
    private val navigator = getAppService<Navigator>()
    private val navigationService = NavigationService()

    // Formatting
    private val formatter = getAppService<FormatService>()
    private val navigationFormatter = NavigationFormatter()

    // Diagnostics
    private val errors by lazy { NavigatorUserErrors(this) }
    private var diagnosticResults by state<Map<Int, List<String>>>(emptyMap())

    // Data commands
    private val loadBeaconsRunner = CoroutineQueueRunner()

    // Map layers
    private val layers = NavigationCompassLayerManager()
    private val layerSheetManager = MapLayerSheetManager(this, layers)

    // Compass layers
    private val compassLayerManager: CompassLayerManager by lazy { CompassLayerManager(gps) { declination } }

    // Preferences
    private val userPrefs = getAppService<UserPreferences>()
    private val baseDistanceUnits by lazy { userPrefs.baseDistanceUnits }
    private val isNearbyEnabled by lazy { userPrefs.navigation.showMultipleBeacons }
    private val nearbyCount by lazy { userPrefs.navigation.numberOfVisibleBeacons }
    private val nearbyDistance by lazy { userPrefs.navigation.maxBeaconDistance }
    private val useTrueNorth by lazy { userPrefs.compass.useTrueNorth }

    // Interactivity
    private val styleChooser by lazy { CompassStyleChooser(userPrefs.navigation, hasCompass) }
    private val screenLock by lazy { NavigationScreenLock(userPrefs.navigation.keepScreenUnlockedWhileOpen) }
    private val northReferenceBadgeManager = NorthReferenceBadgeManager()

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
            CalibrateCompassAlert(requireContext()).alert()
            navigator.navigateTo(beaconId)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeFlow(navigator.destination2) {
            destination = it
            compassLayerManager.destination = it
        }

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

        binding.speed.setShowDescription(false)
        binding.altitude.setShowDescription(false)

        NavigationQuickActionBinder(
            this,
            binding,
            userPrefs.navigation
        ).bind()

        observeFlow(beaconService.getBeacons()) {
            beacons = it
            updateNearbyBeacons()
        }

        observe(compass) { }
        observe(clinometer) { }
        observe(altimeter) { }
        observe(gps) { }
        observe(speedometer) { }

        binding.navigationTitle.subtitle.setOnLongClickListener {
            ShareLocationCommand(this, gps).execute()
            true
        }

        binding.navigationTitle.subtitle.setOnClickListener {
            ShowLocationSheetCommand(this, gps).execute()
        }

        binding.altitude.setOnClickListener {
            ShowAltitudeSheetCommand(this, altimeter).execute()
        }

        binding.beaconBtn.setOnClickListener {
            OpenBeaconsCommand().execute(findNavController())
        }

        binding.beaconBtn.setOnLongClickListener {
            CreateBeaconHereCommand(gps, altimeter).execute(findNavController())
            true
        }

        binding.accuracyView.setSensors(gps, compass)
        binding.accuracyView.setOnClickListener { displayAccuracyTips() }

        binding.mapAttribution.movementMethod = LinkMovementMethod.getInstance()

        binding.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.radarCompassMap.setOnSingleTapListener {
            toggleDestinationBearing()
        }

        binding.radarCompassMap.setOnLongPressListener {
            layerSheetManager.open(binding.radarCompassMap)
        }

        binding.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        binding.radarCompassMap.mapCenter = gps.location
        binding.radarCompassMap.projection = AzimuthalEquidistantProjection(gps.location)
        binding.radarCompassMap.resolutionPixels = userPrefs.navigation.radarCompassScale
        binding.radarCompassMap.setOnScaleChangeListener(true) { resolutionPixels ->
            val radiusMeters = resolutionPixels * binding.radarCompassMap.width / 2f
            binding.radarCompass.setRadiusDistance(Distance.meters(radiusMeters))
            userPrefs.navigation.radarCompassScale = resolutionPixels
        }

        if (!hasCompass) {
            binding.radarCompass.shouldDrawDial = userPrefs.navigation.showDialTicksWhenNoCompass
            binding.radarCompass.shouldDrawAzimuthIndicator = false
            binding.navigationTitle.title.isVisible = false
            binding.northReferenceIndicator.isVisible = false
        } else {
            binding.radarCompass.shouldDrawAzimuthIndicator =
                userPrefs.navigation.showAzimuthIndicator
        }

        scheduleUpdates(INTERVAL_30_FPS)
    }

    private fun toggleDestinationBearing() {
        inBackground {
            if (destination is Destination.Beacon) {
                // TODO: Prompt to cancel navigation?
                // Don't set destination bearing while navigating to a beacon
                return@inBackground
            }

            if (destination == null && hasCompass) {
                // TODO: Wait for GPS location to be up to date (show a loading indicator)
                navigator.navigateToBearing(compass.rawBearing, gps.location)
            } else {
                onMain {
                    if (isBound) {
                        binding.navigationSheet.requestCancelNavigation()
                    }
                }
            }
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

    override fun onResume() {
        super.onResume()

        compassLayerManager.resume(binding.linearCompass, binding.radarCompass)

        binding.radarCompassMap.useDensityPixelsForZoom =
            !userPrefs.navigation.highDetailMode
        layers.resume(requireContext(), binding.radarCompassMap)
        binding.radarCompass.bindMapView(binding.radarCompassMap)
        northReferenceBadgeManager.resume(binding.northReferenceIndicator)
    }

    override fun onPause() {
        super.onPause()
        compassLayerManager.pause()
        loadBeaconsRunner.cancel()
        errors.reset()
        layerSheetManager.close()
        layers.pause(binding.radarCompassMap)
        northReferenceBadgeManager.pause()
    }

    private fun updateNearbyBeacons() {
        inBackground {
            onIO {
                loadBeaconsRunner.skipIfRunning {
                    val destinationBeacon = (destination as? Destination.Beacon)?.beacon
                    if (!isNearbyEnabled) {
                        nearbyBeacons = listOfNotNull(destinationBeacon)
                        return@skipIfRunning
                    }

                    nearbyBeacons = (navigationService.getNearbyBeacons(
                        gps.location,
                        beacons,
                        nearbyCount,
                        8f,
                        nearbyDistance
                    ) + listOfNotNull(destinationBeacon)).distinctBy { it.id }
                    compassLayerManager.nearbyBeacons = nearbyBeacons
                }
            }
        }
    }

    private fun getSelectedBeacon(nearby: Collection<Beacon>): Beacon? {
        return (destination as? Destination.Beacon)?.beacon ?: getFacingBeacon(nearby)
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
            val currentDestination =
                destination ?: getSelectedBeacon(nearbyBeacons)?.let { Destination.Beacon(it) }
            if (currentDestination != null) {
                binding.navigationSheet.updateNavigationSensorValues(
                    gps.location,
                    altimeter.altitude,
                    speedometer.speed.speed,
                    declination
                )
                binding.navigationSheet.show(currentDestination, destination != null)
            } else {
                binding.navigationSheet.hide()
            }
        }

        effect("speed", speedometer.speed.speed, lifecycleHookTrigger.onResume()) {
            binding.speed.title = formatter.formatSpeed(speedometer.speed.speed)
        }

        effect("azimuth", compass.rawBearing, lifecycleHookTrigger.onResume()) {
            updateCompassBearing()
        }

        useEffect(triggers.frequency("compass_invalidation", Duration.ofSeconds(1))) {
            binding.radarCompass.invalidate()
        }

        effect("altitude", altimeter.altitude, lifecycleHookTrigger.onResume()) {
            binding.altitude.title = formatter.formatDistance(
                Distance.meters(altimeter.altitude).convertTo(baseDistanceUnits)
            )
        }

        effect("location", gps.location, layers.key, lifecycleHookTrigger.onResume()) {
            updateLocation()
        }

        effect("navigation", destination, lifecycleHookTrigger.onResume()) {
            handleShowWhenLocked()
        }

        effect(
            "attribution",
            layers.key,
            binding.radarCompass.isVisible
        ) {
            inBackground {
                if (binding.radarCompass.isVisible) {
                    val attribution = binding.radarCompassMap.getAttribution(requireContext())
                    onMain {
                        binding.mapAttribution.text = attribution
                        binding.mapAttribution.isVisible = attribution != null
                    }
                } else {
                    onMain {
                        binding.mapAttribution.isVisible = false
                    }
                }
            }
        }

        effect("device_orientation", clinometer.incline.toInt(), lifecycleHookTrigger.onResume()) {
            val style = styleChooser.getStyle(clinometer.incline)
            binding.linearCompass.isInvisible = style != CompassStyle.Linear
            binding.radarCompass.isInvisible = style != CompassStyle.Radar
            binding.radarCompassMap.isInvisible = style != CompassStyle.Radar
        }

        effect("error_messages", diagnosticResults, lifecycleHookTrigger.onResume()) {
            val codes = diagnosticResults.values.flatten().distinct()
            errors.update(codes)
        }
    }

    private fun updateCompassBearing() {
        val bearing = compass.rawBearing

        // Azimuth
        if (hasCompass) {
            val titleText = memo("azimuth_title", bearing.safeRoundToInt()) {
                navigationFormatter.formatAzimuth(bearing)
            }
            binding.navigationTitle.title.setTextDistinct(titleText)
        }

        // Compass
        listOf<ICompassView>(
            binding.radarCompass,
            binding.linearCompass
        ).forEach {
            it.azimuth = bearing
            it.declination = declination
        }

        val actualBearing = if (useTrueNorth) {
            compass.bearing
        } else {
            compass.bearing.withDeclination(declination)
        }
        binding.radarCompassMap.userAzimuth = actualBearing
        binding.radarCompassMap.mapAzimuth = actualBearing.value
    }

    private fun updateLocation() {
        val location = gps.location

        declination = declinationProvider.getDeclination()
        compass.declination = declination

        binding.navigationTitle.subtitle.setTextDistinct(
            formatter.formatLocation(location)
        )

        binding.radarCompassMap.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        // Compass center point
        listOf<ICompassView>(
            binding.radarCompass,
            binding.linearCompass
        ).forEach {
            it.compassCenter = location
        }

        binding.radarCompassMap.mapCenter = location
        binding.radarCompassMap.userLocation = location

        binding.radarCompassMap.projection = AzimuthalEquidistantProjection(location)

        updateNearbyBeacons()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityNavigatorBinding {
        return ActivityNavigatorBinding.inflate(layoutInflater, container, false)
    }

    companion object {
        const val CACHE_CAMERA_ZOOM = "sighting_compass_camera_zoom"
    }
}
