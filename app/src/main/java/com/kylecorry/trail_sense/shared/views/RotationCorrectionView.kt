package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.tools.maps.infrastructure.BitmapUtils2
import com.kylecorry.trail_sense.tools.maps.infrastructure.resize
import com.kylecorry.trail_sense.tools.maps.infrastructure.rotate
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService

class RotationCorrectionView : CanvasView {

    private var image: Bitmap? = null
    private var imagePath: String? = null
    @DrawableRes
    private var imageDrawable: Int? = null
    private var linesLoaded = false
    private var scale = 0.8f
    private val localFileService by lazy { LocalFileService(context) }

    var angle = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var imageX = 0f
    private var imageY = 0f

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
        if (image == null && imagePath != null){
            imagePath?.let {
                val file = localFileService.getFile(it, false)
                val bitmap = BitmapUtils2.decodeBitmapScaled(
                    file.path,
                    width,
                    height
                )
                image = bitmap.resize(width, height)
                bitmap.recycle()
            }
        } else if (image == null && imageDrawable != null){
            imageDrawable?.let {
                val img = loadImage(it)
                image = img.resize(width, height)
                img.recycle()
            }
        }

        val bitmap = image?.rotate(angle) ?: return
        imageX = (width - bitmap.width * scale) / (2f)
        imageY = (height - bitmap.height * scale) / (2f)

        push()
        translate(imageX, imageY)
        scale(scale)
        image(bitmap, 0f, 0f)
        pop()
        bitmap.recycle()
    }

    fun setImage(bitmap: Bitmap) {
        image = bitmap
        imagePath = null
        imageDrawable = null
        linesLoaded = false
        invalidate()
    }

    fun setImage(@DrawableRes id: Int) {
        imageDrawable = id
        imagePath = null
        image = null
        linesLoaded = false
        invalidate()
    }

    fun setImage(path: String) {
        imagePath = path
        imageDrawable = null
        image = null
        linesLoaded = false
        invalidate()
    }
}