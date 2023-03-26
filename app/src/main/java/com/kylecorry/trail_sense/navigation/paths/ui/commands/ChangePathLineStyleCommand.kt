package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ChangePathLineStyleCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        Pickers.item(
            context, context.getString(R.string.line_style), listOf(
                context.getString(R.string.solid),
                context.getString(R.string.dotted),
                context.getString(R.string.arrow),
                context.getString(R.string.dashed),
                context.getString(R.string.square),
                context.getString(R.string.diamond),
                context.getString(R.string.cross)
            ),
            defaultSelectedIndex = path.style.line.ordinal
        ) {
            if (it != null) {
                val line =
                    LineStyle.values().find { style -> style.ordinal == it } ?: LineStyle.Dotted
                lifecycleOwner.inBackground {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(style = path.style.copy(line = line)))
                    }
                }
            }
        }
    }
}