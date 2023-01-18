package com.kylecorry.trail_sense.tools.maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.maps.infrastructure.reduce.LowQualityMapReducer
import com.kylecorry.trail_sense.tools.maps.infrastructure.reduce.MediumQualityMapReducer

class ResizeMapCommand(
    private val context: Context,
    private val loadingIndicator: ILoadingIndicator
) : CoroutineCommand<Map> {
    override suspend fun execute(value: Map) {
        val resolution = onMain {
            CoroutinePickers.item(
                context,
                context.getString(R.string.change_resolution),
                listOf(
                    context.getString(R.string.low),
                    context.getString(R.string.moderate),
                    context.getString(R.string.high)
                )
            )
        } ?: return

        onMain {
            loadingIndicator.show()
        }

        onIO {
            val reducer = when (resolution) {
                0 -> LowQualityMapReducer(context)
                1 -> MediumQualityMapReducer(context)
                else -> HighQualityMapReducer(context)
            }
            reducer.reduce(value)
        }

        onMain {
            loadingIndicator.hide()
        }

    }
}