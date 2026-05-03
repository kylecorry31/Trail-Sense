package com.kylecorry.trail_sense.tools.map.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo

class DeleteOfflineMapCommand(private val context: Context) : CoroutineCommand<OfflineMapFile> {
    private val repo = getAppService<OfflineMapFileRepo>()

    override suspend fun execute(value: OfflineMapFile) {
        val shouldDelete = onMain {
            !CoroutineAlerts.dialog(
                context,
                context.getString(R.string.delete),
                value.name
            )
        }

        if (!shouldDelete) {
            return
        }
        repo.delete(value)
    }
}
