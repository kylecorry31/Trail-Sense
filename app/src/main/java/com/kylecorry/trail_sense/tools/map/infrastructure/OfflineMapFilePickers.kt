package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup
import com.kylecorry.trail_sense.tools.map.ui.OfflineMapFileGroupListItemMapper
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
        filter: (List<OfflineMapFileGroup>) -> List<OfflineMapFileGroup> = { it }
    ): Pair<Boolean, OfflineMapFileGroup?> = suspendCoroutine { cont ->
        val loader = OfflineMapFileGroupLoader(getAppService<OfflineMapFileService>().loader)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { filter(it.filterIsInstance<OfflineMapFileGroup>()) }
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
            cont.resume(cancelled to item as OfflineMapFileGroup?)
        }
    }
}
