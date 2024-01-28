package com.kylecorry.trail_sense.tools.astronomy.ui.format

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.astronomy.domain.Eclipse

object EclipseFormatter {

    fun type(context: Context, eclipse: Eclipse): CharSequence {
        val formatService = FormatService.getInstance(context)
        return if (eclipse.isTotal) context.getString(R.string.total) else context.getString(
            R.string.partial,
            formatService.formatPercentage(eclipse.obscuration * 100)
        )
    }

}