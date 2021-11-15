package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.ui.PathNameFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MergePathCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val paths: List<Path>,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        val items = paths.filter { it.id != path.id }
        val nameFactory = PathNameFactory(context)
        Pickers.item(
            context,
            context.getString(R.string.append_onto),
            items.map { nameFactory.getName(it) }) {
            if (it != null) {
                lifecycleScope.launch {
                    val loading = withContext(Dispatchers.Main) {
                        Alerts.loading(context, context.getString(R.string.merging))
                    }
                    withContext(Dispatchers.IO) {
                        pathService.mergePaths(items[it].id, path.id)
                    }
                    withContext(Dispatchers.Main) {
                        loading.dismiss()
                    }
                }
            }
        }
    }
}