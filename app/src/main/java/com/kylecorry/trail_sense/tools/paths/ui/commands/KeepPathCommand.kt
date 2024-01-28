package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class KeepPathCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleOwner.inBackground {
            withContext(Dispatchers.IO) {
                pathService.addPath(path.copy(temporary = false))
            }
        }
    }
}