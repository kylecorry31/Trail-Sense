package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.core.net.toUri
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo

class CreateBlankMapCommand(
    private val context: Context,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)
    private val location = LocationSubsystem.getInstance(context)
    private val prefs = UserPreferences(context)
    private val formatter = FormatService.getInstance(context)

    override suspend fun execute(): PhotoMap? = onIO {
        val calibration = getCalibration() ?: return@onIO null

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

            val calibrated = map?.copy(
                calibration = MapCalibration(
                    warped = true,
                    rotated = true,
                    rotation = 0,
                    calibrationPoints = calibration
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

    private suspend fun getCalibration(): List<MapCalibrationPoint>? {
        val allUnits = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
        
        val defaultDistance =
            Distance.kilometers(2f).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        var distance = defaultDistance
        var center = location.location

        val cancelled = onMain {
            val view = View.inflate(context, R.layout.fragment_blank_map_create_sheet, null)
            val distanceInput = view.findViewById<DistanceInputView>(R.id.distance_input)
            val coordinateInput = view.findViewById<CoordinateInputView>(R.id.coordinate_input)

            coordinateInput.coordinate = center
            coordinateInput.setOnCoordinateChangeListener {
                center = it ?: location.location
            }

            distanceInput.units = allUnits
            distanceInput.value = distance
            distanceInput.setOnValueChangeListener {
                distance = it ?: defaultDistance
            }

            try {
                CoroutineAlerts.dialog(
                    context,
                    context.getString(R.string.create_blank_map),
                    contentView = view
                )
            } finally {
                tryOrNothing { coordinateInput.pause() }
            }
        }

        if (cancelled) {
            return null
        }

        val topLeft = center.plus(distance, Bearing.from(CompassDirection.NorthWest))
        return listOf(
            MapCalibrationPoint(center, PercentCoordinate(0.5f, 0.5f)),
            MapCalibrationPoint(topLeft, PercentCoordinate(0f, 0f)),
        )
    }

}