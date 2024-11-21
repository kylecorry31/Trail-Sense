package com.kylecorry.trail_sense.tools.sensors

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.sensors.widgets.AppWidgetElevation
import com.kylecorry.trail_sense.tools.sensors.widgets.AppWidgetLocation
import com.kylecorry.trail_sense.tools.sensors.widgets.ElevationWidgetView
import com.kylecorry.trail_sense.tools.sensors.widgets.LocationWidgetView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object SensorsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SENSORS,
            context.getString(R.string.sensors),
            R.drawable.ic_sensors,
            R.id.sensorDetailsFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_sensors,
            initialize = {
                LocationSubsystem.getInstance(it)
                SensorSubsystem.getInstance(it)
            },
            diagnostics = listOf(
                ToolDiagnosticFactory.magnetometer(context),
                ToolDiagnosticFactory.accelerometer(context),
                ToolDiagnosticFactory.gyroscope(context),
                ToolDiagnosticFactory.barometer(context),
                ToolDiagnosticFactory.battery(context),
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
            ).distinctBy { it.id },
            widgets = listOf(
                ToolWidget(
                    WIDGET_LOCATION,
                    context.getString(R.string.location),
                    ToolSummarySize.Half,
                    LocationWidgetView(),
                    AppWidgetLocation::class.java,
                    updateBroadcasts = listOf(BROADCAST_LOCATION_CHANGED),
                    usesLocation = true
                ),
                ToolWidget(
                    WIDGET_ELEVATION,
                    context.getString(R.string.elevation),
                    ToolSummarySize.Half,
                    ElevationWidgetView(),
                    AppWidgetElevation::class.java,
                    updateBroadcasts = listOf(BROADCAST_ELEVATION_CHANGED),
                    usesLocation = true
                )
            ),
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_LOCATION_CHANGED,
                    "Location changed"
                ),
                ToolBroadcast(
                    BROADCAST_ELEVATION_CHANGED,
                    "Elevation changed"
                )
            )
        )
    }

    const val BROADCAST_LOCATION_CHANGED = "sensors-broadcast-location-changed"
    const val BROADCAST_ELEVATION_CHANGED = "sensors-broadcast-elevation-changed"

    const val WIDGET_LOCATION = "sensors-widget-location"
    const val WIDGET_ELEVATION = "sensors-widget-elevation"
}