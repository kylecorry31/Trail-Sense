package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.net.toUri
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo

class CreateBlankMapCommand(
    private val context: Context,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)
    private val location = LocationSubsystem.getInstance(context)

    override suspend fun execute(): Map? = onIO {

        val center = location.location
        val distance = Distance.kilometers(2f)
        val topLeft = center.plus(distance, Bearing.from(CompassDirection.NorthWest))

        onMain {
            loadingIndicator.show()
        }

        val repo = MapRepo.getInstance(context)

        val file = files.createTemp(".webp")
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        files.save(files.getLocalPath(file), bitmap, 10, true)

        try {
            val map = CreateMapFromUriCommand(
                context,
                repo,
                file.toUri(),
                loadingIndicator
            ).execute()
            DeleteTempFilesCommand(context).execute()

            val calibrationPoints = listOf(
                MapCalibrationPoint(center, PercentCoordinate(0.5f, 0.5f)),
                MapCalibrationPoint(topLeft, PercentCoordinate(0f, 0f)),
            )

            val calibrated = map?.copy(
                calibration = MapCalibration(
                    true,
                    true,
                    0,
                    calibrationPoints
                )
            )

            calibrated?.let { repo.addMap(it) }

            calibrated
        } finally {
            onMain {
                loadingIndicator.hide()
            }
        }
    }
}