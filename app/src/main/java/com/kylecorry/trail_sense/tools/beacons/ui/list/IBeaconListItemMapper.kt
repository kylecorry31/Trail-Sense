package com.kylecorry.trail_sense.tools.beacons.ui.list

import android.content.Context
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon

class IBeaconListItemMapper(
    context: Context,
    gps: IGPS,
    beaconHandler: (Beacon, BeaconAction) -> Unit,
    groupHandler: (BeaconGroup, BeaconGroupAction) -> Unit
) : ListItemMapper<IBeacon> {

    private val beaconMapper = BeaconListItemMapper(context, gps, beaconHandler)
    private val groupMapper = BeaconGroupListItemMapper(context, groupHandler)

    override fun map(value: IBeacon): ListItem {
        return if (value is Beacon) {
            beaconMapper.map(value)
        } else {
            groupMapper.map(value as BeaconGroup)
        }
    }
}