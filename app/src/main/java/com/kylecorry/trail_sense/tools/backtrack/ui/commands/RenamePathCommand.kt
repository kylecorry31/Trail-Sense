package com.kylecorry.trail_sense.tools.backtrack.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.IPathService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.paths.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RenamePathCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        Pickers.text(
            context,
            context.getString(R.string.rename),
            default = path.name,
            hint = context.getString(R.string.name)
        ) {
            if (it != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(name = if (it.isBlank()) null else it))
                    }
                }

            }
        }
    }
}