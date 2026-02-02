package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperations
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import kotlin.math.roundToInt

class CropTile(
    private val imageBounds: CoordinateBounds,
    private val tileBounds: CoordinateBounds,
    private val tileSize: Size,
    private val getResizeOperations: (size: Size) -> List<BitmapOperation> = {
        listOf(Resize(it, exact = true, useBilinearScaling = true))
    }
) : BitmapOperation {

    override fun execute(bitmap: Bitmap): Bitmap {
        val pixelsPerDegreeX = bitmap.width / imageBounds.widthDegrees()
        val pixelsPerDegreeY = bitmap.height / imageBounds.heightDegrees()

        val cropX = SolMath.normalizeAngle(tileBounds.west - imageBounds.west) * pixelsPerDegreeX
        val cropY = (imageBounds.north - tileBounds.north) * pixelsPerDegreeY
        val cropWidth = tileBounds.widthDegrees() * pixelsPerDegreeX
        val cropHeight = tileBounds.heightDegrees() * pixelsPerDegreeY

        val scaleX = tileSize.width / cropWidth
        val scaleY = tileSize.height / cropHeight

        val newWidth = bitmap.width * scaleX
        val newHeight = bitmap.height * scaleY

        val newCropX = cropX * scaleX
        val newCropY = cropY * scaleY

        val desiredSize = Size(newWidth.roundToInt(), newHeight.roundToInt())

        return bitmap.applyOperations(
            *getResizeOperations(desiredSize).toTypedArray(),
            Crop(
                newCropX.toFloat(),
                newCropY.toFloat(),
                tileSize.width.toFloat(),
                tileSize.height.toFloat()
            )
        )
    }
}
