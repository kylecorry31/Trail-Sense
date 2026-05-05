package com.kylecorry.trail_sense.tools.map.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFileService

class EditOfflineMapAttributionCommand(
    private val context: Context
) : CoroutineCommand<OfflineMapFile> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: OfflineMapFile) {
        val attribution = CoroutinePickers.text(
            context,
            context.getString(R.string.attribution),
            hint = context.getString(R.string.attribution),
            default = value.attribution.orEmpty()
        ) ?: return

        service.add(value.copy(attribution = attribution.trim()))
    }
}
