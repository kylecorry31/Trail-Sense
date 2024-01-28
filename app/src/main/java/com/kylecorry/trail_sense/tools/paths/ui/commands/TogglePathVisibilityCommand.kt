package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService


class TogglePathVisibilityCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleOwner.inBackground {
            onIO {
                pathService.addPath(path.copy(style = path.style.copy(visible = !path.style.visible)))
            }
        }
    }
}