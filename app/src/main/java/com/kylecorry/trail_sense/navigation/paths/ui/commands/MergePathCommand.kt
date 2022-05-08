package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.infrastructure.PathPickers
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MergePathCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleScope.launch {
            val other = PathPickers.pickPath(
                context,
                context.getString(R.string.append_onto),
                scope = lifecycleScope,
                filter = { it.filter { it !is Path || it.id != path.id } }
            ) ?: return@launch

            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, context.getString(R.string.merging))
            }
            withContext(Dispatchers.IO) {
                pathService.mergePaths(other.id, path.id)
            }
            withContext(Dispatchers.Main) {
                loading.dismiss()
            }

        }
    }
}