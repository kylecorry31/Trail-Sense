package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PrintMapCommand(private val context: Context) {

    suspend fun execute(map: PhotoMap) = onIO {
        val printer = Printer(context)
        val files = FileSubsystem.getInstance(context)

        val uri = if (map.hasPdf(context)) {
            files.uri(map.pdfFileName)
        } else {
            files.uri(map.filename)
        }

        printer.setColorMode(ColorMode.Color)
        printer.setScaleMode(ScaleMode.Fit)

        // If width is greater than height, print in landscape
        if (map.metadata.size.width > map.metadata.size.height) {
            printer.setOrientation(Orientation.Landscape)
        } else {
            printer.setOrientation(Orientation.Portrait)
        }
        printer.print(uri)
    }

}