package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.IPathSortStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.NamePathSortStrategy
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.ui.IPathListItemMapper
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.shared.lists.GroupListManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object PathPickers {

    suspend fun pickGroup(
        context: Context,
        title: String? = null,
        okText: String = context.getString(android.R.string.ok),
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IPathSortStrategy = NamePathSortStrategy(),
        filter: (List<PathGroup>) -> List<PathGroup> = { it }
    ): Pair<Boolean, PathGroup?> = suspendCoroutine { cont ->
        val loader = PathGroupLoader(PathService.getInstance(context))
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it.filterIsInstance<PathGroup>())) }
        )
        val mapper = IPathListItemMapper(context, { _, _ -> }, { _, _ -> })
        val titleProvider = { path: IPath? ->
            if (path is PathGroup) {
                path.name
            } else {
                context.getString(R.string.paths)
            }
        }
        GroupablePickers.group(
            context,
            title,
            okText,
            manager,
            mapper,
            titleProvider,
            context.getString(R.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as PathGroup?)
        }
    }

    suspend fun pickPath(
        context: Context,
        title: String? = null,
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IPathSortStrategy = NamePathSortStrategy(),
        filter: (List<IPath>) -> List<IPath> = { it }
    ): Path? = suspendCoroutine { cont ->
        val loader = PathGroupLoader(PathService.getInstance(context))
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it)) }
        )
        val mapper = IPathListItemMapper(context, { _, _ -> }, { _, _ -> })
        val titleProvider = { path: IPath? ->
            if (path is PathGroup) {
                path.name
            } else {
                context.getString(R.string.paths)
            }
        }
        GroupablePickers.item(
            context,
            title,
            manager,
            mapper,
            titleProvider,
            context.getString(R.string.no_paths),
            initialGroup,
            searchEnabled = false
        ) {
            cont.resume(it as Path?)
        }
    }
}