package com.kylecorry.trail_sense.tools.beacons.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.core.widget.RemoteViewsCompat.setRemoteAdapter
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.subsystem.BeaconsSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

class NearbyBeaconsToolWidgetView : ToolWidgetView {
    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val views = getView(context)

        val subsystem = BeaconsSubsystem.getInstance(context)
        val prefs = UserPreferences(context)
        val beacons = subsystem.getNearbyBeacons().take(10)
        val location = LocationSubsystem.getInstance(context).location
        val formatter = FormatService.getInstance(context)

        views.setOnClickPendingIntent(
            R.id.widget_frame,
            NavigationUtils.toolPendingIntent(context, Tools.BEACONS)
        )

        views.setPendingIntentTemplate(
            R.id.widget_list,
            PendingIntent.getActivity(
                context,
                1279381,
                MainActivity.intent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )

        val items = RemoteViewsCompat.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(beacons.size)
            .also {
                beacons.forEach { beacon ->
                    val itemViews = RemoteViews(
                        Package.getPackageName(context),
                        R.layout.widget_list_item_simple
                    )
                    val distance = Distance.meters(location.distanceTo(beacon.coordinate))
                        .convertTo(prefs.baseDistanceUnits)
                        .toRelativeDistance()
                    itemViews.setTextViewText(R.id.title, beacon.name)
                    itemViews.setTextViewText(
                        R.id.description,
                        formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units))
                    )
                    itemViews.setImageViewIcon(
                        R.id.icon,
                        Icon.createWithResource(
                            context,
                            beacon.icon?.icon ?: R.drawable.ic_location
                        ).also {
                            it.setTint(
                                Colors.mostContrastingColor(
                                    Color.WHITE,
                                    Color.BLACK,
                                    beacon.color
                                )
                            )
                        })
                    itemViews.setImageViewIcon(
                        R.id.icon_background,
                        Icon.createWithResource(
                            context,
                            R.drawable.circle,
                        ).also { it.setTint(beacon.color) })
                    itemViews.setOnClickFillInIntent(R.id.root, Intent().also {
                        it.putExtra("beacon_id", beacon.id)
                    })
                    it.addItem(beacon.id, itemViews)
                }
            }
            .build()

        val appWidgetIds = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, AppWidgetNearbyBeacons::class.java))
        appWidgetIds.forEach { id ->
            setRemoteAdapter(context, views, id, R.id.widget_list, items)
        }

        if (appWidgetIds.isEmpty()) {
            // Running in app, no widgets exist
            setRemoteAdapter(context, views, 0, R.id.widget_list, items)
        }

        views.setTextViewText(R.id.widget_list_empty, context.getString(R.string.no_beacons))
        views.setEmptyView(R.id.widget_list, R.id.widget_list_empty)

        views.setTextViewText(R.id.widget_title, context.getString(R.string.nearby_beacons))

        return views
    }

    override fun getView(context: Context): RemoteViews {
        return RemoteViews(Package.getPackageName(context), R.layout.widget_list)
    }
}