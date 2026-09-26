package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.flatten
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.views.DataPointView
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

class NavigationSheetView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {


    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val formatter = DependencyRegistry.get<FormatService>()
    private val navigator = DependencyRegistry.get<Navigator>()

    private val useTrueNorth = prefs.compass.useTrueNorth

    private val navigationService = NavigationService()
    private val hikingService = HikingService()

    private var destination: Destination? = null
    private var isNavigating: Boolean = false
    private var sensorValues: NavigationSensorValues? = null
    private var useTrueNorthOverride: Boolean? = null

    // VIEWS
    private val toolbar: Toolbar
    private val distanceDataView: DataPointView
    private val bearingDataView: DataPointView
    private val elevationDataView: DataPointView
    private val etaDataView: DataPointView

    init {
        inflate(context, R.layout.view_navigation_sheet, this)
        toolbar = findViewById(R.id.navigation_sheet_title)
        distanceDataView = findViewById(R.id.navigation_distance)
        bearingDataView = findViewById(R.id.navigation_bearing)
        elevationDataView = findViewById(R.id.navigation_elevation)
        etaDataView = findViewById(R.id.navigation_eta)
        bearingDataView.setShowDescription(false)
        toolbar.leftButton.flatten()
    }


    // TODO: Listen for navigation and automatically hide/show with override ability (for navigation tool)
    fun hide() {
        isVisible = false
        destination = null
        updateNavigation()
    }

    fun show(destination: Destination, isNavigating: Boolean = false) {
        this.destination = destination
        this.isNavigating = isNavigating
        updateNavigation()
    }

    fun setTrueNorthOverride(useTrueNorth: Boolean?) {
        useTrueNorthOverride = useTrueNorth
        updateNavigation()
    }


    // TODO: This should be tracked by a service (for background navigation)
    fun updateNavigationSensorValues(values: NavigationSensorValues) {
        sensorValues = values
        updateNavigation()
    }

    fun updateNavigationSensorValues(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        declination: Float
    ) {
        val values = NavigationSensorValues(
            location,
            null,
            Distance.meters(elevation),
            null,
            Bearing.from(0f),
            declination,
            Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds),
            Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
        )
        updateNavigationSensorValues(values)
    }

    fun requestCancelNavigation() {
        if (!isNavigating) {
            return
        }
        Alerts.dialog(
            context,
            context.getString(R.string.cancel_navigation_question),
            okText = context.getString(R.string.yes),
            cancelText = context.getString(R.string.no)
        ) { cancelled ->
            if (!cancelled) {
                CoroutineScope(Dispatchers.Default).launch {
                    navigator.cancelAllNavigation()
                }
            }
        }
    }

    private fun updateNavigation() {
        val values = sensorValues
        val destination = destination
        if (destination == null || values == null) {
            isVisible = false
            return
        }
        isVisible = true
        toolbar.title.maxWidth = Resources.dp(context, 250f).toInt()
        toolbar.rightButton.isVisible = isNavigating
        toolbar.rightButton.setOnClickListener {
            requestCancelNavigation()
        }

        elevationDataView.contentDescription = null
        when (destination) {
            is Destination.Beacon -> {
                updateBeaconNavigation(destination, values)
            }

            is Destination.Bearing -> {
                updateBearingNavigation(destination, values)
            }

            is Destination.Path -> {
                updatePathNavigation(destination, values)
            }
        }
    }

    private fun updatePathNavigation(destination: Destination.Path, values: NavigationSensorValues) {
        val guidance = destination.route.navigate(values.location)
        val gain = guidance.remainingElevationGain.convertTo(prefs.baseDistanceUnits)
        val loss = guidance.remainingElevationLoss.convertTo(prefs.baseDistanceUnits)
        elevationDataView.isVisible = gain.value.absoluteValue > 0 || loss.value.absoluteValue > 0
        val gainText = formatter.formatDistance(gain, Units.getDecimalPlaces(gain.units), false)
        val lossText = formatter.formatDistance(loss, Units.getDecimalPlaces(loss.units), false)
        elevationDataView.setShowDescription(true)
        elevationDataView.title = "↑ $gainText"
        elevationDataView.description = "↓ $lossText"
        elevationDataView.contentDescription =
            "${context.getString(R.string.ascent)} $gainText, ${context.getString(R.string.descent)} $lossText"
        distanceDataView.isVisible = true
        bearingDataView.isVisible = false
        distanceDataView.description = ""
        distanceDataView.setShowDescription(false)
        updateDestinationDistance(guidance.remainingDistance)
        toolbar.title.text = destination.path.name ?: context.getString(R.string.path)
        toolbar.subtitle.isVisible = false
        toolbar.leftButton.isVisible = false
        toolbar.title.setOnClickListener(null)
        toolbar.subtitle.setOnClickListener(null)
        etaDataView.isVisible = true
        val speed = values.speed.convertTo(
            DistanceUnits.Meters, TimeUnits.Seconds
        ).value
        val remainingDuration = hikingService.getHikingDuration(
            Distance.meters(guidance.remainingDistance),
            guidance.remainingElevationGain,
            Speed.from(
                navigationService.getHikingSpeed(speed),
                DistanceUnits.Meters,
                TimeUnits.Seconds
            )
        )
        updateEta(remainingDuration)
    }

    private fun updateBearingNavigation(
        destination: Destination.Bearing,
        values: NavigationSensorValues
    ) {
        val bearing = navigator.getBearing(values.location, destination)
        updateDestinationDirection(bearing.value, true)
        updateDestinationElevation(null, null)
        etaDataView.isVisible = false
        toolbar.title.text = context.getString(R.string.bearing)
        toolbar.subtitle.isVisible = false
        toolbar.leftButton.isVisible = false
        toolbar.title.setOnClickListener(null)
    }

    private fun updateBeaconNavigation(
        destination: Destination.Beacon,
        values: NavigationSensorValues
    ) {
        val beacon = destination.beacon
        val vector = navigationService.navigate(
            values.location,
            values.elevation.meters().value,
            beacon,
            values.declination,
            useTrueNorthOverride ?: useTrueNorth
        )

        updateDestinationDirection(vector.direction.value)
        updateDestinationElevation(beacon.elevation, vector.altitudeChange)
        updateDestinationEta(
            values.location, values.elevation.meters().value, values.speed.convertTo(
                DistanceUnits.Meters, TimeUnits.Seconds
            ).value, beacon
        )

        // TODO: These don't change
        toolbar.title.text = beacon.name
        toolbar.subtitle.isVisible = true

        val hasComment = !beacon.comment.isNullOrEmpty()
        toolbar.leftButton.isVisible = hasComment

        toolbar.leftButton.setOnClickListener {
            Alerts.dialog(context, beacon.name, beacon.comment, cancelText = null)
        }

        val elevationDistance = beacon.elevation?.let {
            Distance.meters(it).convertTo(prefs.baseDistanceUnits)
        }

        toolbar.subtitle.text = formatter.join(
            *listOfNotNull(
                formatter.formatLocation(beacon.coordinate),
                if (elevationDistance != null) context.getString(
                    R.string.elevation_value,
                    formatter.formatDistance(
                        elevationDistance,
                        Units.getDecimalPlaces(elevationDistance.units)
                    )
                ) else null
            ).toTypedArray(),
            separator = FormatService.Separator.NewLine
        )

        toolbar.title.setOnClickListener {
            openBeacon(beacon.id)
        }

        toolbar.subtitle.setOnClickListener {
            openBeacon(beacon.id)
        }
    }

    private fun openBeacon(id: Long) {
        findNavController().navigateWithAnimation(
            R.id.beaconDetailsFragment,
            Bundle().apply {
                putLong("beacon_id", id)
            }
        )
    }

    private fun updateDestinationDirection(azimuth: Float, isTitle: Boolean = false) {
        val value = formatter.formatDegrees(
            azimuth,
            replace360 = true
        ) + " " + formatter.formatDirection(CompassDirection.nearest(azimuth))
        distanceDataView.isVisible = !isTitle
        bearingDataView.isVisible = isTitle
        if (isTitle) {
            bearingDataView.title = value
        } else {
            distanceDataView.setShowDescription(true)
            distanceDataView.description = value
        }
    }

    private fun updateDestinationEta(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        beacon: Beacon
    ) {
        etaDataView.isVisible = true
        updateDestinationDistance(location.distanceTo(beacon.coordinate))

        // ETA
        val eta = navigationService.eta(location, elevation, speed, beacon)
        updateEta(eta)
    }

    private fun updateDestinationDistance(meters: Float) {
        val distance = Distance.meters(meters).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        distanceDataView.title = formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
    }

    private fun updateEta(duration: Duration) {
        etaDataView.title = formatter.formatDuration(duration, false)
        etaDataView.description = formatter.formatTime(
            ZonedDateTime.now().plus(duration).toLocalTime(),
            includeSeconds = false
        )
    }

    private fun updateDestinationElevation(destinationElevation: Float?, elevationChange: Float?) {
        val hasElevation = elevationChange != null && destinationElevation != null
        elevationDataView.isVisible = hasElevation
        if (hasElevation) {
            val direction = when {
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> ""
            }

            val elevationChangeDist =
                Distance.meters(elevationChange).convertTo(prefs.baseDistanceUnits)

            elevationDataView.setShowDescription(false)
            elevationDataView.title = context.getString(
                R.string.elevation_diff_format,
                direction,
                formatter.formatDistance(
                    elevationChangeDist, Units.getDecimalPlaces(elevationChangeDist.units),
                    false
                )
            )
        }
    }
}
