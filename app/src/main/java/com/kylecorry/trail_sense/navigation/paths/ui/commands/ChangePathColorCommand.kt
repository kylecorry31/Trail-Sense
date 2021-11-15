package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChangePathColorCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        CustomUiUtils.pickColor(
            context,
            AppColor.values().firstOrNull { it.color == path.style.color } ?: AppColor.Gray,
            context.getString(R.string.path_color)
        ) {
            if (it != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(style = path.style.copy(color = it.color)))
                    }
                }
            }
        }
    }
}