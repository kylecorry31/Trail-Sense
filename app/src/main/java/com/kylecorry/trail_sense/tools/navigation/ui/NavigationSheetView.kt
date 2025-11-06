package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.flatten
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.direction
import com.kylecorry.trail_sense.shared.extensions.NavigationSensorValues
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.DataPointView
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class NavigationSheetView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {


    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()
    private val navigator = AppServiceRegistry.get<Navigator>()

    private val useTrueNorth = prefs.compass.useTrueNorth

    private val navigationService = NavigationService()

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
        toolbar = findViewById<Toolbar>(R.id.navigation_sheet_title)
        distanceDataView = findViewById<DataPointView>(R.id.navigation_distance)
        bearingDataView = findViewById<DataPointView>(R.id.navigation_bearing)
        elevationDataView = findViewById<DataPointView>(R.id.navigation_elevation)
        etaDataView = findViewById<DataPointView>(R.id.navigation_eta)
        bearingDataView.setShowDescription(false)
        CustomUiUtils.setButtonState(toolbar.rightButton, true)
        CustomUiUtils.setButtonState(toolbar.leftButton, false)
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

        if (destination is Destination.Beacon) {
            updateBeaconNavigation(destination, values)
        } else if (destination is Destination.Bearing) {
            updateBearingNavigation(destination, values)
        }
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
            ).speed, beacon
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
            Distance.meters(it).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
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
            bundleOf(
                "beacon_id" to id
            )
        )
    }

    private fun updateDestinationDirection(azimuth: Float, isTitle: Boolean = false) {
        val value = formatter.formatDegrees(
            azimuth,
            replace360 = true
        ) + " " + formatter.formatDirection(Bearing.direction(azimuth))
        distanceDataView.isVisible = !isTitle
        bearingDataView.isVisible = isTitle
        if (isTitle) {
            bearingDataView.title = value
        } else {
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
        val d = Distance.meters(location.distanceTo(beacon.coordinate))
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        distanceDataView.title =
            formatter.formatDistance(d, Units.getDecimalPlaces(d.units), false)

        // ETA
        val eta = navigationService.eta(location, elevation, speed, beacon)
        etaDataView.title = formatter.formatDuration(eta, false)
        etaDataView.description = formatter.formatTime(
            ZonedDateTime.now().plus(eta).toLocalTime(),
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