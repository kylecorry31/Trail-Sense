package com.kylecorry.trail_sense.astronomy.ui.format

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.Eclipse
import com.kylecorry.trail_sense.shared.FormatService

object EclipseFormatter {

    fun type(context: Context, eclipse: Eclipse): CharSequence {
        val formatService = FormatService.getInstance(context)
        return if (eclipse.isTotal) context.getString(R.string.total) else context.getString(
            R.string.partial,
            formatService.formatPercentage(eclipse.obscuration * 100)
        )
    }

}