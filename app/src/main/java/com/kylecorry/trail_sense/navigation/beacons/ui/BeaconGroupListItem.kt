package com.kylecorry.trail_sense.navigation.beacons.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.lists.ListItem
import com.kylecorry.trail_sense.shared.lists.ListMenuItem
import com.kylecorry.trail_sense.shared.lists.ResourceListIcon

enum class BeaconGroupAction {
    Open,
    Edit,
    Delete,
    Move
}

fun BeaconGroup.toListItem(
    context: Context,
    action: (BeaconGroupAction) -> Unit
): ListItem {
    return ListItem(
        title = name,
        icon = ResourceListIcon(R.drawable.ic_beacon_group, AppColor.Orange.color),
        subtitle = context.resources.getQuantityString(
            R.plurals.beacon_group_summary,
            count,
            count
        ),
        menu = getMenu(context, action)
    ) {
        action(BeaconGroupAction.Open)
    }
}

private fun BeaconGroup.getMenu(
    context: Context,
    action: (BeaconGroupAction) -> Unit
): List<ListMenuItem> {
    return listOf(
        ListMenuItem(context.getString(R.string.rename)) { action(BeaconGroupAction.Edit) },
        ListMenuItem(context.getString(R.string.move_to)) { action(BeaconGroupAction.Move) },
        ListMenuItem(context.getString(R.string.delete)) { action(BeaconGroupAction.Delete) },
    )
}