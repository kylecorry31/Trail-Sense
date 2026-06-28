package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

class PrintMapCommand(private val context: Context) {

    suspend fun execute(map: PhotoMap) = onIO {
        val printer = Printer(context)
        val files = FileSubsystem.getInstance(context)

        val uri = if (map.pdfFile != null) {
            files.uri(map.pdfFile.path)
        } else {
            files.uri(map.imageFile.path)
        }

        printer.setColorMode(ColorMode.Color)
        printer.setScaleMode(ScaleMode.Fit)

        // If width is greater than height, print in landscape
        if (map.georeference.size.width > map.georeference.size.height) {
            printer.setOrientation(Orientation.Landscape)
        } else {
            printer.setOrientation(Orientation.Portrait)
        }
        printer.print(uri)
    }

}
