package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.shared.lists.GroupListManager
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.maps.ui.mappers.MapGroupMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object MapPickers {

    // TODO: Add sort
    suspend fun pickGroup(
        context: Context,
        title: String? = null,
        okText: String = context.getString(android.R.string.ok),
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        filter: (List<MapGroup>) -> List<MapGroup> = { it }
    ): Pair<Boolean, MapGroup?> = suspendCoroutine { cont ->
        val loader = MapGroupLoader(MapService.getInstance(context).loader)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { filter(it.filterIsInstance<MapGroup>()) }
        )
        val mapper = MapGroupMapper(context) { _, _ -> }
        GroupablePickers.group(
            context,
            title,
            okText,
            manager,
            mapper,
            { it?.name ?: context.getString(R.string.photo_maps) },
            context.getString(R.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as MapGroup?)
        }
    }

}