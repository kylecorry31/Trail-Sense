package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.OfflineMapFileGroupListItemMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object OfflineMapFilePickers {

    suspend fun pickGroup(
        context: Context,
        title: String? = null,
        okText: String = context.getString(android.R.string.ok),
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        filter: (List<MapGroup>) -> List<MapGroup> = { it }
    ): Pair<Boolean, MapGroup?> = suspendCoroutine { cont ->
        val loader = OfflineMapFileGroupLoader(getAppService<OfflineMapFileService>().loader)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { filter(it.filterIsInstance<MapGroup>()) }
        )
        val mapper = OfflineMapFileGroupListItemMapper(context) { _, _ -> }
        GroupablePickers.group(
            context,
            title,
            okText,
            manager,
            mapper,
            { it?.name ?: context.getString(R.string.offline_maps) },
            context.getString(R.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as MapGroup?)
        }
    }
}
