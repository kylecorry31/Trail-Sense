package com.kylecorry.trail_sense.navigation.beacons.ui.list

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.ceres.list.*
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils

class BeaconListItemMapper(
    private val context: Context,
    private val gps: IGPS,
    private val actionHandler: (Beacon, BeaconAction) -> Unit
) : ListItemMapper<Beacon> {

    private val prefs by lazy { UserPreferences(context) }
    private val showVisibilityToggle by lazy { prefs.navigation.showMultipleBeacons || prefs.navigation.areMapsEnabled }

    override fun map(value: Beacon): ListItem {
        return value.toListItem(
            context,
            prefs.baseDistanceUnits,
            gps.location,
            showVisibilityToggle
        ) {
            actionHandler(value, it)
        }
    }

    private fun Beacon.toListItem(
        context: Context,
        units: DistanceUnits,
        myLocation: Coordinate,
        showVisibilityToggle: Boolean,
        action: (BeaconAction) -> Unit
    ): ListItem {

        val hasTrailingIcon = showVisibilityToggle && !temporary

        return ListItem(
            id,
            title = name,
            icon = getListIcon(context),
            subtitle = getDescription(context, units, myLocation),
            trailingIcon = if (hasTrailingIcon) {
                ResourceListIcon(
                    if (visible) {
                        R.drawable.ic_visible
                    } else {
                        R.drawable.ic_not_visible
                    },
                    Resources.androidTextColorSecondary(context)
                )
            } else {
                null
            },
            trailingIconAction = {
                action(BeaconAction.ToggleVisibility)
            },
            longClickAction = { action(BeaconAction.Navigate) },
            menu = getMenu(context, action)
        ) {
            action(BeaconAction.View)
        }

    }

    private fun Beacon.getMenu(
        context: Context,
        action: (BeaconAction) -> Unit
    ): List<ListMenuItem> {
        return if (temporary) {
            listOf(
                ListMenuItem(context.getString(R.string.navigate)) { action(BeaconAction.Navigate) },
                ListMenuItem(context.getString(R.string.share_ellipsis)) { action(BeaconAction.Share) },
            )
        } else {
            listOf(
                ListMenuItem(context.getString(R.string.navigate)) { action(BeaconAction.Navigate) },
                ListMenuItem(context.getString(R.string.share_ellipsis)) { action(BeaconAction.Share) },
                ListMenuItem(context.getString(R.string.edit)) { action(BeaconAction.Edit) },
                ListMenuItem(context.getString(R.string.move_to)) { action(BeaconAction.Move) },
                ListMenuItem(context.getString(R.string.delete)) { action(BeaconAction.Delete) },
            )
        }
    }

    private fun Beacon.getDescription(
        context: Context,
        units: DistanceUnits,
        myLocation: Coordinate
    ): String {
        val formatService = FormatService.getInstance(context)
        val navigationService = NavigationService()
        val distance = navigationService.navigate(coordinate, myLocation, 0f).distance
        val d = Distance.meters(distance).convertTo(units).toRelativeDistance()
        return formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
    }

    private fun Beacon.getListIcon(context: Context): ListIcon {
        val foregroundScale = 0.6f
        if (owner == BeaconOwner.User) {
            return if (icon != null) {
                val foregroundColor = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, color)
                ResourceListIcon(
                    icon.icon,
                    foregroundColor,
                    backgroundId = R.drawable.bubble,
                    backgroundTint = color,
                    foregroundSize = foregroundScale * 24f
                )
            } else {
                ResourceListIcon(
                    R.drawable.bubble,
                    color,
                    scaleType = ImageView.ScaleType.CENTER
                )
            }
        }

        val formatService = FormatService.getInstance(context)

        // Cell signal is the only other shown beacon owner
        val quality = when {
            name.contains(formatService.formatQuality(Quality.Poor)) -> Quality.Poor
            name.contains(formatService.formatQuality(Quality.Moderate)) -> Quality.Moderate
            name.contains(formatService.formatQuality(Quality.Good)) -> Quality.Good
            else -> Quality.Unknown
        }
        val backgroundColor = CustomUiUtils.getQualityColor(quality)
        val foregroundColor = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, backgroundColor)
        return ResourceListIcon(
            CellSignalUtils.getCellQualityImage(quality),
            foregroundColor,
            backgroundId = R.drawable.bubble,
            backgroundTint = backgroundColor,
            foregroundSize = foregroundScale * 24f
        )
    }
}