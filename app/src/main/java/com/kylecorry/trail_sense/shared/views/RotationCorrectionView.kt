package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class RotationCorrectionView : CanvasView {

    private var image: Bitmap? = null
    private var imagePath: String? = null

    private var linesLoaded = false
    private var scale = 0.8f

    var angle = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var imageX = 0f
    private var imageY = 0f

    private val files = FileSubsystem.getInstance(context)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
    }

    override fun setup() {
    }

    override fun draw() {
        if (image == null && imagePath != null) {
            imagePath?.let { loadImage(it) }
        }

        val image = image ?: return

        // Center the image
        imageX = (width - image.width * scale) / 2f
        imageY = (height - image.height * scale) / 2f

        push()
        rotate(angle)
        translate(imageX, imageY)
        scale(scale)
        image(image, 0f, 0f)
        pop()
    }

    private fun loadImage(path: String) {
        val bitmap = files.bitmap(path, width, height) ?: return
        image = bitmap.resizeToFit(width, height)
        if (image != bitmap) {
            bitmap.recycle()
        }
    }

    fun setImage(path: String) {
        imagePath = path
        image = null
        linesLoaded = false
        invalidate()
    }

    fun clearImage() {
        imagePath = null
        val oldImage = image
        image = null
        oldImage?.recycle()
        linesLoaded = false
        invalidate()
    }

}