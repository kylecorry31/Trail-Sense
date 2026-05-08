package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.mappers

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

class IMapMapper(
    gps: IGPS,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    mapActionHandler: (PhotoMap, MapAction) -> Unit,
    mapGroupActionHandler: (MapGroup, MapGroupAction) -> Unit
) : ListItemMapper<IMap> {

    private val mapMapper = MapMapper(gps, context, lifecycleOwner, mapActionHandler)
    private val mapGroupMapper = MapGroupMapper(context, mapGroupActionHandler)

    override fun map(value: IMap): ListItem {
        return if (value is PhotoMap) {
            mapMapper.map(value)
        } else {
            mapGroupMapper.map(value as MapGroup)
        }
    }
}
