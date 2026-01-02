package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperations
import com.kylecorry.sol.math.floorToInt

class ResizePadded(
    private val size: Size,
    private val useBilinearScaling: Boolean = true,
    private val padding: Int = 0
) :
    BitmapOperation {

    override fun execute(bitmap: Bitmap): Bitmap {
        val horizontalGrowFactor = size.width / (bitmap.width - 2 * padding).toFloat()
        val horizontalPadding = (padding * horizontalGrowFactor).floorToInt()

        val verticalGrowFactor = size.height / (bitmap.height - 2 * padding).toFloat()
        val verticalPadding = (padding * verticalGrowFactor).floorToInt()

        return bitmap.applyOperations(
            Resize(
                Size(size.width + horizontalPadding * 2, size.height + verticalPadding * 2),
                true,
                useBilinearScaling
            ),
            Crop(
                horizontalPadding.toFloat(),
                verticalPadding.toFloat(),
                size.width.toFloat(),
                size.height.toFloat()
            ),
            recycleOriginal = false
        )
    }
}