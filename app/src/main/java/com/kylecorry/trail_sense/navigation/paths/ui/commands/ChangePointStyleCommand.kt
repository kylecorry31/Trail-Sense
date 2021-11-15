package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPointColoringStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChangePointStyleCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        Pickers.item(
            context, context.getString(R.string.point_style), listOf(
                context.getString(R.string.none),
                context.getString(R.string.cell_signal),
                context.getString(R.string.elevation),
                context.getString(R.string.time)
            ),
            defaultSelectedIndex = path.style.point.ordinal
        ) {
            if (it != null) {
                val pointStyle =
                    PathPointColoringStyle.values().find { style -> style.ordinal == it }
                        ?: PathPointColoringStyle.None
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(style = path.style.copy(point = pointStyle)))
                    }
                }
            }
        }
    }
}