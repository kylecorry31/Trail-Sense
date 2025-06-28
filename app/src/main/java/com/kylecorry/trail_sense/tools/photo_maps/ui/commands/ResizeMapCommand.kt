package com.kylecorry.trail_sense.tools.photo_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.reduce.LowQualityMapReducer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.reduce.MediumQualityMapReducer

class ResizeMapCommand(
    private val context: Context,
    private val loadingIndicator: ILoadingIndicator
) : CoroutineCommand<PhotoMap> {
    override suspend fun execute(value: PhotoMap) {
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