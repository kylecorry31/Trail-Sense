package com.kylecorry.trail_sense.tools.navigation.ui

import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.ui.setTextDistinct
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors
import com.kylecorry.trail_sense.shared.extensions.useTimer
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.useMapLayersSheet
import com.kylecorry.trail_sense.shared.map_layers.ui.useMapAttribution
import com.kylecorry.trail_sense.shared.map_layers.ui.useMapLayers
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.getTrueNorthBearing
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.DataPointView
import com.kylecorry.trail_sense.shared.views.NorthReferenceBadge
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationCompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.Instant

class NavigatorFragment : TrailSenseReactiveFragment(R.layout.activity_navigator) {

    override fun update() {
        // Views
        val title = useView<Toolbar>(R.id.navigation_title)
        val altitudeView = useView<DataPointView>(R.id.altitude)
        val speedView = useView<DataPointView>(R.id.speed)
        val radarCompass = useView<RadarCompassMapView>(R.id.radar_compass)
        val linearCompass = useView<LinearSightingCompassView>(R.id.linear_compass)
        val attributionView = useView<TextView>(R.id.map_attribution)
        val northReferenceBadge = useView<NorthReferenceBadge>(R.id.north_reference_indicator)
        val beaconButton = useView<FloatingActionButton>(R.id.beaconBtn)
        val sensorStatusBadges = useView<SensorStatusBadgeView>(R.id.accuracy_view)
        val navigationSheet = useView<NavigationSheetView>(R.id.navigation_sheet)

        // Services
        val context = useAndroidContext()
        val activity = useActivity()
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val sensorService = useService<SensorService>()
        val navigator = useService<Navigator>()
        val hasCompass = useMemo(sensorService) { sensorService.hasCompass() }

        // Sensors
        // The compass is recreated when its settings change
        val compassSettings = useMemo(prefs, resetOnResume) {
            listOf(
                prefs.compass.source,
                prefs.compass.compassSmoothing,
                prefs.compass.useTrueNorth,
                prefs.compass.updateFrequency
            )
        }
        val useTrueNorth = useMemo(compassSettings) { prefs.compass.useTrueNorth }
        val compassDelay = useMemo(compassSettings) { prefs.compass.updateFrequency.sensorDelay }
        // The compass and clinometer share an orientation sensor
        val orientationSensor = useMemo(sensorService, compassSettings) {
            sensorService.getOrientation(compassDelay)
        }
        val sensors = useNavigationSensors(
            SensorService.NAVIGATION_GPS_FREQUENCY,
            compassDelay = compassDelay,
            orientationSensor = orientationSensor
        )
        val navigation = sensors.values
        val gps = sensors.gps
        val compass = sensors.compass
        val altimeter = sensors.altimeter
        val location = navigation.location
        val altitude = navigation.elevation.value
        val speed = navigation.speed.value
        val declination = navigation.declination
        val azimuth = navigation.bearing.value

        val destination = useFlow(navigator.destination)
        val nearbyBeacons = useNearbyBeacons(location, destination)
        val compassStyle = useCompassStyle(orientationSensor, hasCompass)

        // Radar map
        val radarMap = radarCompass.map
        useEffect(radarMap, prefs, resetOnResume) {
            radarMap.useDensityPixelsForZoom = !prefs.navigation.highDetailMode
        }

        val layerManager = useMemo { NavigationCompassLayerManager() }
        val layers = useMapLayers(layerManager, radarMap, resetOnResume)
        val showLayersSheet = useMapLayersSheet(
            NavigationToolRegistration.MAP_ID,
            onShow = layers.pause,
            onDismiss = layers.resume
        )
        useMapAttribution(radarMap, attributionView, layers.key, compassStyle == CompassStyle.Radar)

        // Compass style
        useEffect(linearCompass, radarCompass, compassStyle) {
            linearCompass.isInvisible = compassStyle != CompassStyle.Linear
            radarCompass.isInvisible = compassStyle != CompassStyle.Radar
        }

        useEffect(radarCompass, title, northReferenceBadge, hasCompass, prefs) {
            if (!hasCompass) {
                radarCompass.shouldDrawDial = prefs.navigation.showDialTicksWhenNoCompass
                radarCompass.shouldDrawAzimuthIndicator = false
                title.title.isVisible = false
                northReferenceBadge.isVisible = false
            } else {
                radarCompass.shouldDrawAzimuthIndicator = prefs.navigation.showAzimuthIndicator
            }
        }

        useEffect(northReferenceBadge, useTrueNorth, resetOnResume) {
            northReferenceBadge.showDetailsOnClick = true
            northReferenceBadge.useTrueNorth = useTrueNorth
            northReferenceBadge.showLabelTemporarily(Duration.ofSeconds(5))
        }

        // Toolbar
        useEffect(title, prefs) {
            NavigationQuickActionBinder(this, title, prefs.navigation).bind()
        }

        useFlashlightLockout(linearCompass, title)

        useEffect(title, gps) {
            title.subtitle.setOnLongClickListener {
                Share.shareLocation(this, gps.location)
                true
            }

            title.subtitle.setOnClickListener {
                val sheet = LocationBottomSheet()
                sheet.gps = gps
                sheet.show(this)
            }
        }

        // Altitude and speed
        useEffect(altitudeView, speedView) {
            altitudeView.setShowDescription(false)
            speedView.setShowDescription(false)
        }

        useEffect(altitudeView, altimeter) {
            altitudeView.setOnClickListener {
                val sheet = AltitudeBottomSheet()
                sheet.currentAltitude = Reading(altimeter.altitude, Instant.now())
                sheet.show(this)
            }
        }

        useEffect(altitudeView, altitude, formatter, prefs, resetOnResume) {
            altitudeView.title = formatter.formatDistance(
                Distance.meters(altitude).convertTo(prefs.baseDistanceUnits)
            )
        }

        useEffect(speedView, speed, formatter, resetOnResume) {
            speedView.title = formatter.formatSpeed(speed)
        }

        useBeaconButton(beaconButton, gps, altimeter)

        // Sensor status
        val showAccuracyTips = useCallback<Unit>(context, gps, compass) {
            ImproveAccuracyAlerter(context).alert(listOf(gps, compass))
        }

        useEffect(sensorStatusBadges, gps, compass, showAccuracyTips) {
            sensorStatusBadges.setSensors(gps, compass)
            sensorStatusBadges.setOnClickListener { showAccuracyTips() }
        }

        useNavigatorErrors(gps, showAccuracyTips, resetOnResume)

        val wasNavigationStarted = useMemo {
            arguments?.getBoolean(ARG_NAVIGATION_STARTED) ?: false
        }
        useCompassCalibrationPrompt(wasNavigationStarted, compass)

        // Navigation
        val toggleDestinationBearing =
            useToggleDestinationBearing(destination, hasCompass, compass, gps, navigationSheet)

        useEffect(linearCompass, radarCompass, toggleDestinationBearing, showLayersSheet) {
            linearCompass.setOnClickListener { toggleDestinationBearing() }
            radarCompass.setOnSingleTapListener(toggleDestinationBearing)
            radarCompass.setOnLongPressListener(showLayersSheet)
        }

        val screenLock = useMemo(prefs) {
            NavigationScreenLock(prefs.navigation.keepScreenUnlockedWhileOpen)
        }

        useEffect(screenLock, activity, destination, resetOnResume) {
            screenLock.updateLock(activity, destination != null)
        }

        useDestroyEffect(screenLock, activity) {
            screenLock.releaseLock(activity)
        }

        useNavigationSheet(navigationSheet, destination, nearbyBeacons, navigation)

        // Compass
        val azimuthText = useMemo(azimuth.safeRoundToInt(), formatter) {
            val degrees = formatter.formatDegrees(azimuth, replace360 = true).padStart(4, ' ')
            val direction = formatter.formatDirection(CompassDirection.nearest(azimuth)).padStart(2, ' ')
            "$degrees   $direction"
        }

        useEffect(title, azimuthText, hasCompass) {
            if (hasCompass) {
                title.title.setTextDistinct(azimuthText)
            }
        }

        val compassViews = useMemo(radarCompass, linearCompass) {
            listOf<ICompassView>(radarCompass, linearCompass)
        }

        useEffect(compassViews, radarCompass, compass, azimuth, declination, resetOnResume) {
            compassViews.forEach {
                it.azimuth = azimuth
                it.declination = declination
            }
            radarCompass.trueNorthAzimuth = compass.getTrueNorthBearing()
        }

        useEffect(
            title,
            compassViews,
            radarCompass,
            location,
            navigation.locationAccuracy,
            layers.key,
            formatter,
            resetOnResume
        ) {
            title.subtitle.setTextDistinct(formatter.formatLocation(location))
            compassViews.forEach { it.compassCenter = location }
            radarCompass.locationAccuracy = navigation.locationAccuracy
        }

        useCompassLayers(linearCompass, radarCompass, destination, nearbyBeacons, navigation, resetOnResume)

        // The radar's layers can change without the compass moving
        useTimer(RADAR_INVALIDATION_INTERVAL) {
            radarCompass.redraw()
        }
    }

    private fun useFlashlightLockout(linearCompass: LinearSightingCompassView, title: Toolbar) {
        val prefs = useService<UserPreferences>()
        useEffect(linearCompass, title, prefs) {
            linearCompass.setOnCameraActiveChangeListener { isCameraActive ->
                // TODO: Extract this logic to the flashlight (if camera is in use)
                if (prefs.navigation.rightButton == Tools.QUICK_ACTION_FLASHLIGHT) {
                    title.rightButton.isClickable = !isCameraActive
                }
                if (prefs.navigation.leftButton == Tools.QUICK_ACTION_FLASHLIGHT) {
                    title.leftButton.isClickable = !isCameraActive
                }
            }
        }
    }

    private fun useBeaconButton(button: FloatingActionButton, gps: IGPS, altimeter: IAltimeter) {
        val navController = useNavController()
        useEffect(button, navController, gps, altimeter) {
            button.setOnClickListener {
                navController.openTool(Tools.BEACONS)
            }

            button.setOnLongClickListener {
                val bundle = if (gps.hasValidReading) {
                    Bundle().apply {
                        putParcelable(
                            "initial_location",
                            GeoUri(
                                gps.location,
                                if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
                            )
                        )
                    }
                } else {
                    null
                }
                navController.openTool(Tools.BEACONS, bundle)
                true
            }
        }
    }

    private fun useToggleDestinationBearing(
        destination: Destination?,
        hasCompass: Boolean,
        compass: ICompass,
        gps: IGPS,
        navigationSheet: NavigationSheetView
    ): () -> Unit {
        val navigator = useService<Navigator>()
        return useCallback<Unit>(destination, hasCompass, compass, gps, navigator, navigationSheet) {
            when (destination) {
                null -> if (hasCompass) {
                    inBackground {
                        // TODO: Wait for GPS location to be up to date (show a loading indicator)
                        navigator.navigateToBearing(compass.bearing.value, gps.location)
                    }
                }

                is Destination.Bearing -> navigationSheet.cancelNavigation()

                // Beacons and paths are only canceled with the navigation sheet's cancel button
                else -> {}
            }
        }
    }

    private fun useNavigationSheet(
        navigationSheet: NavigationSheetView,
        destination: Destination?,
        nearbyBeacons: List<Beacon>,
        values: NavigationSensorValues
    ) {
        val prefs = useService<UserPreferences>()
        val navigationService = useMemo { NavigationService() }

        // TODO: Move selected beacon updating to a coroutine
        // The sheet only needs to update when the rounded bearing changes
        useEffect(
            navigationSheet,
            destination,
            nearbyBeacons,
            values.location,
            values.elevation,
            values.speed,
            values.declination,
            values.bearing.value.safeRoundToInt(),
            resetOnResume
        ) {
            val currentDestination = destination ?: navigationService.getFacingBeacon(
                values.location,
                values.bearing.value,
                nearbyBeacons,
                values.declination,
                prefs.compass.useTrueNorth
            )?.let { Destination.Beacon(it) }

            if (currentDestination != null) {
                navigationSheet.updateNavigationSensorValues(values)
                navigationSheet.show(currentDestination, destination != null)
            } else {
                navigationSheet.hide()
            }
        }
    }

    companion object {
        const val ARG_NAVIGATION_STARTED = "navigation_started"
        private const val RADAR_INVALIDATION_INTERVAL = 1000L
    }
}
