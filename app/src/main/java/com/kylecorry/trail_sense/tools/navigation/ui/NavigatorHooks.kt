package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.useTimer
import com.kylecorry.trail_sense.shared.extensions.useTrigger
import com.kylecorry.trail_sense.shared.map_layers.IMappableReferencePoint
import com.kylecorry.trail_sense.shared.map_layers.MappableBearing
import com.kylecorry.trail_sense.shared.map_layers.MappableReferencePoint
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.domain.astronomy.NavAstronomyService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.BeaconCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.MarkerCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.NavigationCompassLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import java.time.Duration

private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f)
private val ASTRONOMY_UPDATE_FREQUENCY = Duration.ofMinutes(1)
private const val SUN_MARKER_ID = 1L
private const val MOON_MARKER_ID = 2L
private const val BELOW_HORIZON_OPACITY = 0.5f

fun ReactiveComponent.useNearbyBeacons(location: Coordinate, destination: Destination?): List<Beacon> {
    val context = useAndroidContext()
    val prefs = useService<UserPreferences>()
    val repo = useMemo(context) { BeaconRepo.getInstance(context) }
    val navigationService = useMemo { NavigationService() }
    val beaconFlow = useMemo(repo) { repo.getBeacons() }
    val beacons = useFlow(beaconFlow, beaconFlow) ?: emptyList()
    val destinationBeacon = (destination as? Destination.Beacon)?.beacon

    val nearby = useBackgroundMemo(location, beacons, destinationBeacon, cancelWhenRerun = true) {
        onIO {
            navigationService.getVisibleBeacons(
                location,
                beacons,
                destinationBeacon,
                prefs.navigation.showMultipleBeacons,
                prefs.navigation.numberOfVisibleBeacons,
                prefs.navigation.maxBeaconDistance
            )
        }
    }

    return nearby ?: emptyList()
}

fun TrailSenseReactiveFragment.useAstronomyCompassMarkers(
    location: Coordinate,
    declination: Float,
    vararg values: Any?
): List<IMappableReferencePoint> {
    val context = useAndroidContext()
    val prefs = useService<UserPreferences>()
    val astronomyService = useMemo { NavAstronomyService() }
    val hasLocation = location != Coordinate.zero

    val (refreshKey, triggerRefresh) = useTrigger()
    useTimer(ASTRONOMY_UPDATE_FREQUENCY.toMillis()) {
        triggerRefresh()
    }

    val markers = useBackgroundMemo(
        distance(location, ASTRONOMY_UPDATE_DISTANCE, highAccuracy = false),
        refreshKey,
        hasLocation,
        *values,
        cancelWhenRerun = true
    ) {
        if (!hasLocation || !prefs.astronomy.showOnCompass) {
            return@useBackgroundMemo emptyList()
        }

        onDefault {
            val astro = astronomyService.getData(location)
            val showWhenDown = prefs.astronomy.showOnCompassWhenDown
            val useTrueNorth = prefs.compass.useTrueNorth
            val moonSize = Resources.dp(context, 24f).toInt()
            val moonImage = MoonPhaseImageMapper(context).getPhaseImage(
                astro.moonPhaseAngle,
                moonSize,
                moonSize,
                astro.moonTilt
            )

            listOfNotNull(
                if (astro.isSunUp || showWhenDown) {
                    MappableReferencePoint(
                        SUN_MARKER_ID,
                        R.drawable.ic_sun,
                        DeclinationUtils.fromTrueNorthBearing(astro.sunBearing, declination, useTrueNorth),
                        opacity = if (astro.isSunUp) 1f else BELOW_HORIZON_OPACITY
                    )
                } else {
                    null
                },
                if (astro.isMoonUp || showWhenDown) {
                    MappableReferencePoint(
                        MOON_MARKER_ID,
                        R.drawable.ic_moon,
                        DeclinationUtils.fromTrueNorthBearing(astro.moonBearing, declination, useTrueNorth),
                        opacity = if (astro.isMoonUp) 1f else BELOW_HORIZON_OPACITY,
                        bitmap = moonImage
                    )
                } else {
                    null
                }
            )
        }
    }

    return markers ?: emptyList()
}

fun TrailSenseReactiveFragment.useCompassLayers(
    linearCompass: ICompassView,
    radarCompass: ICompassView,
    destination: Destination?,
    nearbyBeacons: List<Beacon>,
    navigation: NavigationSensorValues,
    vararg values: Any?
) {
    val prefs = useService<UserPreferences>()
    val navigator = useService<Navigator>()
    val astronomyLayer = useMemo { MarkerCompassLayer() }
    val beaconLayer = useMemo { BeaconCompassLayer() }
    val navigationLayer = useMemo { NavigationCompassLayer() }
    val location = navigation.location
    val declination = navigation.declination

    useEffect(linearCompass, radarCompass, astronomyLayer, beaconLayer, navigationLayer, prefs) {
        linearCompass.setCompassLayers(listOf(astronomyLayer, beaconLayer, navigationLayer))
        radarCompass.setCompassLayers(
            listOfNotNull(
                astronomyLayer,
                if (prefs.navigation.showNearbyBeaconsOnlyOnLinearCompass) null else beaconLayer,
                navigationLayer
            )
        )
    }

    val astronomyMarkers = useAstronomyCompassMarkers(location, declination, *values)
    useEffect(astronomyLayer, astronomyMarkers) {
        astronomyLayer.setMarkers(astronomyMarkers)
    }

    useBackgroundEffect(
        beaconLayer,
        navigationLayer,
        linearCompass,
        radarCompass,
        destination,
        location,
        declination,
        nearbyBeacons,
        *values
    ) {
        val destinationBeacon = (destination as? Destination.Beacon)?.beacon

        beaconLayer.setBeacons(nearbyBeacons)
        beaconLayer.highlight(destinationBeacon)

        if (destinationBeacon != null) {
            navigationLayer.setDestination(destinationBeacon)
        } else {
            val bearing = destination?.let { navigator.getBearing(location, it, declination).value }
            navigationLayer.setDestination(bearing?.let { MappableBearing(it, NavigationColors.bearing) })
        }

        onMain {
            linearCompass.redraw()
            radarCompass.redraw()
        }
    }
}

fun <T> T.useNavigatorErrors(
    gps: IGPS,
    onShowAccuracyTips: () -> Unit,
    vararg values: Any?
) where T : Fragment, T : ReactiveComponent {
    val context = useAndroidContext()
    val onShowAccuracyTipsRef = useRef(onShowAccuracyTips)
    onShowAccuracyTipsRef.current = onShowAccuracyTips
    val errors = useMemo(context) { NavigatorUserErrors(this) { onShowAccuracyTipsRef.current() } }

    val gpsFlow = useMemo(gps, context) { GPSDiagnosticScanner(gps).fullScan(context) }
    val compassFlow = useMemo(context) { MagnetometerDiagnosticScanner().fullScan(context) }
    val gpsResults = useFlow(gpsFlow, gpsFlow, state = BackgroundMinimumState.Resumed)
    val compassResults = useFlow(compassFlow, compassFlow, state = BackgroundMinimumState.Resumed)

    val codes = useMemo(gpsResults, compassResults) {
        (gpsResults.orEmpty() + compassResults.orEmpty()).map { it.id }.distinct()
    }

    useEffect(errors, codes, *values) {
        errors.update(codes)
    }

    usePauseEffect(errors) {
        errors.reset()
    }
}

fun ReactiveComponent.useCompassCalibrationPrompt(shouldCheck: Boolean, compass: ICompass) {
    val context = useAndroidContext()
    val prefs = useService<UserPreferences>()
    val hasChecked = useRef(false)
    val quality = useTopic(compass, compass.quality) { it.quality }

    useEffect(shouldCheck, quality) {
        // The compass quality is unknown until it reports its first reading
        if (!shouldCheck || hasChecked.current || quality == Quality.Unknown) {
            return@useEffect
        }
        hasChecked.current = true
        if (quality == Quality.Poor) {
            CompassCalibrationPrompt(context, prefs.navigation).show()
        }
    }
}

fun ReactiveComponent.useCompassStyle(orientationSensor: IOrientationSensor, hasCompass: Boolean): CompassStyle {
    val prefs = useService<UserPreferences>()
    val chooser = useMemo(prefs, hasCompass) { CompassStyleChooser(prefs.navigation, hasCompass) }
    val clinometer = useMemo(orientationSensor) { Clinometer(orientationSensor, isAugmentedReality = true) }
    return useTopic(clinometer, chooser.getStyle(clinometer.incline)) { chooser.getStyle(it.incline) }
}
