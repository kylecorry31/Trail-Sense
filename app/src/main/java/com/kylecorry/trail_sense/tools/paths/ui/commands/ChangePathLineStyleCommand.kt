package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.getLineStyleNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ChangePathLineStyleCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        val names = getLineStyleNames(context)
        val styles = LineStyle.entries
        Pickers.item(
            context, context.getString(R.string.line_style), styles.map { names.getValue(it) },
            defaultSelectedIndex = styles.indexOf(path.style.line)
        ) {
            if (it != null) {
                val line = styles.getOrElse(it) { LineStyle.Dotted }
                lifecycleOwner.inBackground {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(style = path.style.copy(line = line)))
                    }
                }
            }
        }
    }
}
