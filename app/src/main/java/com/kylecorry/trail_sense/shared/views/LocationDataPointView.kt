package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units

/**
 * A horizontal row of three DataPointViews showing distance, direction, and elevation difference
 * from the user's location to a selected point on the map.
 */
class LocationDataPointView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val formatter = DependencyRegistry.get<FormatService>()

    private val distanceView: DataPointView
    private val directionView: DataPointView
    private val elevationView: DataPointView

    init {
        orientation = HORIZONTAL
        val padding = Resources.dp(context, 8f).toInt()
        setPadding(padding, 0, padding, padding)

        distanceView = addDataPoint(R.drawable.ruler, context.getString(R.string.distance))
        directionView = addDataPoint(R.drawable.ic_compass_icon, context.getString(R.string.direction))
        elevationView = addDataPoint(R.drawable.ic_altitude, context.getString(R.string.elevation))
    }

    private fun addDataPoint(icon: Int, description: String): DataPointView {
        val dp = DataPointView(context, null)
        dp.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        dp.setImageResource(icon)
        dp.description = description
        addView(dp)
        return dp
    }

    /**
     * Sets the horizontal distance from user to selected point.
     */
    fun setDistance(distance: Distance) {
        val relative = distance.toRelativeDistance()
        distanceView.title = formatter.formatDistance(
            relative,
            Units.getDecimalPlaces(relative.units),
            strict = false
        )
    }

    /**
     * Sets the direction (bearing) from user to selected point.
     */
    fun setDirection(bearing: Bearing) {
        val azimuth = bearing.value
        directionView.title = formatter.formatDegrees(azimuth, replace360 = true) +
                " " + formatter.formatDirection(CompassDirection.nearest(azimuth))
    }

    /**
     * Sets the signed elevation difference (selected point elevation minus user elevation).
     * Positive = uphill, negative = downhill.
     */
    fun setElevationDiff(elevationDiff: Distance) {
        val sign = if (elevationDiff.value > 0) context.getString(R.string.increase) else ""
        elevationView.title = context.getString(
            R.string.elevation_diff_format,
            sign,
            formatter.formatElevation(elevationDiff)
        )
    }
}
