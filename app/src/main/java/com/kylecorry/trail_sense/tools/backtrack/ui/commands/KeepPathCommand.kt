package com.kylecorry.trail_sense.tools.backtrack.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.trail_sense.navigation.infrastructure.IPathService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.paths.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class KeepPathCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                pathService.addPath(path.copy(temporary = false))
            }
        }
    }
}