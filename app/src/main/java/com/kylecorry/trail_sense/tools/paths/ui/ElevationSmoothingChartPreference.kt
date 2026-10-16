package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.LineChartLayer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.chart.label.DistanceChartLabelFormatter
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

class ElevationSmoothingChartPreference(context: Context, attributeSet: AttributeSet) :
    Preference(context, attributeSet) {

    private val units = UserPreferences(context).baseDistanceUnits
    private val formatService = FormatService.getInstance(context)

    private val rawLine = LineChartLayer(emptyList(), AppColor.Gray.color.withAlpha(100))
    private val smoothedLine = LineChartLayer(emptyList(), Resources.getPrimaryColor(context))
    private var chart: Chart? = null

    init {
        layoutResource = R.layout.preference_elevation_smoothing_chart
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        val chart = holder.findViewById(R.id.chart) as Chart
        chart.configureXAxis(
            labelCount = 4,
            drawGridLines = false,
            labelFormatter = DistanceChartLabelFormatter(
                formatService,
                DistanceUnits.Meters,
                units,
                true
            )
        )
        chart.configureYAxis(
            labelCount = 3,
            drawGridLines = true,
            labelFormatter = DistanceChartLabelFormatter(
                formatService,
                DistanceUnits.Meters,
                units,
                false
            )
        )
        chart.plot(rawLine, smoothedLine)
        chart.setShouldRerenderEveryCycle(false)
        this.chart = chart
    }

    fun releaseChart() {
        chart = null
    }

    fun setPath(path: List<PathPoint>) {
        rawLine.data = getPlotPoints(path)
        chart?.invalidate()
    }

    fun plot(smoothedPath: List<PathPoint>) {
        smoothedLine.data = getPlotPoints(smoothedPath)
        chart?.invalidate()
    }

    private fun getPlotPoints(path: List<PathPoint>): List<Vector2> {
        var distance = 0f
        return path.mapIndexed { index, point ->
            if (index != 0) {
                distance += path[index - 1].coordinate.distanceTo(point.coordinate)
            }
            Vector2(distance, point.elevation ?: 0f)
        }
    }
}
