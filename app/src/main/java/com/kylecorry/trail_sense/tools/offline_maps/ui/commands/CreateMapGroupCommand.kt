package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService

class CreateMapGroupCommand(
    private val context: Context,
    private val mapService: OfflineMapService
) : CoroutineCommand<Long?> {
    override suspend fun execute(value: Long?) {
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.name),
                hint = context.getString(R.string.name)
            )
        } ?: return
        mapService.createGroup(name, value)
    }
}
