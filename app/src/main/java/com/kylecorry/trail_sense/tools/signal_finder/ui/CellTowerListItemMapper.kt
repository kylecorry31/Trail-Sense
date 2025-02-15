package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.toRelativeDistance

enum class CellTowerListItemAction {
    Navigate,
    CreateBeacon
}

class CellTowerListItemMapper(
    private val context: Context,
    private val location: Coordinate,
    private val onAction: (Pair<Coordinate, CellNetwork>, CellTowerListItemAction) -> Unit
) :
    ListItemMapper<Pair<Coordinate, CellNetwork>> {

    private val formatter = AppServiceRegistry.get<FormatService>()
    private val prefs = AppServiceRegistry.get<UserPreferences>()

    override fun map(value: Pair<Coordinate, CellNetwork>): ListItem {
        val towerLocation = value.first
        val network = value.second
        val distance =
            Distance.meters(location.distanceTo(towerLocation))
                .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val direction = location.bearingTo(towerLocation)
        val formattedDistance =
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units))
        val formattedBearing = formatter.formatDegrees(direction.value, replace360 = true)
        val formattedDirection = formatter.formatDirection(direction.direction)
        return ListItem(
            value.hashCode().toLong(),
            formatter.formatCellNetwork(network),
            formatter.join(
                context.getString(R.string.cell_tower),
                formattedDistance,
                "$formattedBearing $formattedDirection",
                separator = FormatService.Separator.Dot
            ),
            icon = ResourceListIcon(
                R.drawable.cell_tower,
                Resources.androidTextColorSecondary(context)
            ),
            menu = listOf(
                ListMenuItem(context.getString(R.string.navigate)) {
                    onAction(value, CellTowerListItemAction.Navigate)
                },
                ListMenuItem(context.getString(R.string.create_beacon)) {
                    onAction(value, CellTowerListItemAction.CreateBeacon)
                }
            )
        )
    }
}