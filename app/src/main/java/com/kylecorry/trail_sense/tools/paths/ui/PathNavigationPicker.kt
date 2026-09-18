package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.navigation.domain.PathNavigationMode
import com.kylecorry.trail_sense.tools.navigation.domain.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

internal object PathNavigationPicker {
    fun show(
        context: Context,
        points: List<PathPoint>,
        onFollow: (PathNavigationMode) -> Unit,
        onNearestPoint: () -> Unit
    ) {
        val options = mutableListOf(
            R.string.path_navigation_follow to PathNavigationMode.TO_END,
            R.string.path_navigation_reverse to PathNavigationMode.REVERSED_TO_END
        )
        if (PathRouteBuilder.isLoop(points.sortedBy { it.id }.map { it.coordinate })) {
            options.add(R.string.path_navigation_loop to PathNavigationMode.FULL_LOOP)
            options.add(R.string.path_navigation_reverse_loop to PathNavigationMode.REVERSED_FULL_LOOP)
        }
        val labels = options.map { context.getString(it.first) } +
            context.getString(R.string.navigate_back_onto_path)
        Pickers.item(context, context.getString(R.string.navigation), labels) { index ->
            if (index != null) {
                val mode = options.getOrNull(index)?.second
                if (mode == null) onNearestPoint() else onFollow(mode)
            }
        }
    }

    fun showToPoint(context: Context, onFollow: () -> Unit, onDirect: () -> Unit) {
        Pickers.item(
            context,
            context.getString(R.string.navigation),
            listOf(context.getString(R.string.path_navigation_to_point), context.getString(R.string.path_navigation_direct))
        ) { index ->
            when (index) {
                0 -> onFollow()
                1 -> onDirect()
            }
        }
    }
}
