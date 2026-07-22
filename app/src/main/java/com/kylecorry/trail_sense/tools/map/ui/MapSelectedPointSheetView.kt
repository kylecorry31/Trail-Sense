package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.DataPointView
import com.kylecorry.trail_sense.shared.views.Toolbar

class MapSelectedPointSheetView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val formatter = DependencyRegistry.get<FormatService>()
    private val prefs = DependencyRegistry.get<UserPreferences>()

    private val toolbar: Toolbar
    private val distanceView: DataPointView
    private val directionView: DataPointView
    private val elevationView: DataPointView

    var onDismiss: (() -> Unit)? = null

    init {
        inflate(context, R.layout.view_map_selected_point_sheet, this)
        toolbar = findViewById(R.id.selected_point_sheet_title)
        distanceView = findViewById(R.id.selected_point_distance)
        directionView = findViewById(R.id.selected_point_direction)
        elevationView = findViewById(R.id.selected_point_elevation)

        toolbar.leftButton.isVisible = false
        toolbar.subtitle.isVisible = false
        toolbar.rightButton.setOnClickListener {
            hide()
            onDismiss?.invoke()
        }

        // Set static description labels
        distanceView.description = context.getString(R.string.distance)
        directionView.description = context.getString(R.string.direction)
        elevationView.description = context.getString(R.string.elevation)

        isVisible = false
    }

    fun hide() {
        isVisible = false
    }

    /**
     * Shows the sheet and populates it with distance, direction, and vertical distance
     * from [userLocation] (at [userElevationMeters]) to [selectedLocation] (at [selectedElevationMeters]).
     */
    fun show(
        selectedLocation: Coordinate,
        userLocation: Coordinate,
        userElevationMeters: Float,
        selectedElevationMeters: Float,
        formattedLocation: String
    ) {
        isVisible = true

        // Title = formatted coordinate of the selected point
        toolbar.title.text = formattedLocation

        // --- Horizontal distance ---
        val horizontalDistMeters = userLocation.distanceTo(selectedLocation)
        val horizontalDist = Distance.meters(horizontalDistMeters)
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        distanceView.title = formatter.formatDistance(
            horizontalDist,
            Units.getDecimalPlaces(horizontalDist.units),
            strict = false
        )

        // --- Direction ---
        val azimuth = userLocation.bearingTo(selectedLocation).value
        directionView.title = formatter.formatDegrees(azimuth, replace360 = true) +
                " " + formatter.formatDirection(CompassDirection.nearest(azimuth))

        // --- Vertical distance (elevation diff, always in base elevation units m/ft) ---
        val elevationChange = selectedElevationMeters - userElevationMeters
        val sign = if (elevationChange > 0) context.getString(R.string.increase) else ""
        elevationView.title = context.getString(
            R.string.elevation_diff_format,
            sign,
            formatter.formatElevation(Distance.meters(elevationChange))
        )
    }
}
