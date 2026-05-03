package com.kylecorry.trail_sense.tools.map.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo

class RenameOfflineMapCommand(private val context: Context) : CoroutineCommand<OfflineMapFile> {

    private val repo = getAppService<OfflineMapFileRepo>()

    override suspend fun execute(value: OfflineMapFile) {
        val name = CoroutinePickers.text(
            context,
            context.getString(R.string.name),
            hint = context.getString(R.string.name),
            default = value.name
        )?.trim()?.takeIf { it.isNotBlank() } ?: return
        repo.add(value.copy(name = name))
    }
}
