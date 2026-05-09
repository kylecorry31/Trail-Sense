package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileService

class EditOfflineMapAttributionCommand(
    private val context: Context
) : CoroutineCommand<VectorMap> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: VectorMap) {
        val attribution = CoroutinePickers.text(
            context,
            context.getString(R.string.attribution),
            hint = context.getString(R.string.attribution),
            default = value.attribution.orEmpty()
        ) ?: return

        service.add(value.copy(attribution = attribution.trim()))
    }
}
