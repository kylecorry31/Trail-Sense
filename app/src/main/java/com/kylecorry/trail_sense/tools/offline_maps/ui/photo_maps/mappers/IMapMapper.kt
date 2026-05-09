package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.mappers

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.OfflineMapFileAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.OfflineMapFileListItemMapper

class IMapMapper(
    gps: IGPS,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    photoMapActionHandler: (PhotoMap, MapAction) -> Unit,
    vectorMapActionHandler: (OfflineMapFile, OfflineMapFileAction) -> Unit,
    mapGroupActionHandler: (MapGroup, MapGroupAction) -> Unit,
) : ListItemMapper<IMap> {

    private val mapMapper = MapMapper(gps, context, lifecycleOwner, photoMapActionHandler)
    private val mapGroupMapper = MapGroupMapper(context, mapGroupActionHandler)
    private val vectorMapMapper = OfflineMapFileListItemMapper(context, vectorMapActionHandler)

    override fun map(value: IMap): ListItem {
        return when (value) {
            is PhotoMap -> mapMapper.map(value)
            is OfflineMapFile -> vectorMapMapper.map(value)
            is MapGroup -> mapGroupMapper.map(value)
            else -> error("Unexpected map type")
        }
    }
}
