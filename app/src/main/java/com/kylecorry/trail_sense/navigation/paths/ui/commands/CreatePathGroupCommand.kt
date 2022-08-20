package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain


class CreatePathGroupCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) {

    suspend fun execute(parentId: Long?) {
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.group),
                hint = context.getString(R.string.name)
            )
        } ?: return

        onIO {
            pathService.addGroup(PathGroup(0, name, parentId))
        }
    }
}