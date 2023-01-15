package com.kylecorry.trail_sense.tools.maps.ui.mappers

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup

class IMapMapper(
    gps: IGPS,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    mapActionHandler: (Map, MapAction) -> Unit,
    mapGroupActionHandler: (MapGroup, MapGroupAction) -> Unit
) : ListItemMapper<IMap> {

    private val mapMapper = MapMapper(gps, context, lifecycleOwner, mapActionHandler)
    private val mapGroupMapper = MapGroupMapper(context, mapGroupActionHandler)

    override fun map(value: IMap): ListItem {
        return if (value is Map) {
            mapMapper.map(value)
        } else {
            mapGroupMapper.map(value as MapGroup)
        }
    }
}