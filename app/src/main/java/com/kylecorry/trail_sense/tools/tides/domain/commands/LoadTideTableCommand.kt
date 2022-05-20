package com.kylecorry.trail_sense.tools.tides.domain.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory

class LoadTideTableCommand(private val context: Context) {

    suspend fun execute(): TideTable? = onIO {
        val loader = TideLoaderFactory().getTideLoader(context)
        loader.getTideTable()
    }

}