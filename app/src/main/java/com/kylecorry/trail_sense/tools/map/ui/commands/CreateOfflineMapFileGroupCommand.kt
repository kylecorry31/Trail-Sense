package com.kylecorry.trail_sense.tools.map.ui.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFileService

class CreateOfflineMapFileGroupCommand(
    private val context: Context
) : CoroutineCommand<Long?> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: Long?) {
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.name),
                hint = context.getString(R.string.name)
            )
        }?.trim()?.takeIf { it.isNotBlank() } ?: return
        service.add(OfflineMapFileGroup(0, name, value))
    }
}
