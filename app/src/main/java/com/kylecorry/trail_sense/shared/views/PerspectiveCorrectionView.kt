package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.*
import android.graphics.Shader.TileMode
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.LocalFileService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.maps.domain.PercentBounds
import com.kylecorry.trail_sense.tools.maps.domain.PixelBounds
import com.kylecorry.trail_sense.tools.maps.infrastructure.fixPerspective
import com.kylecorry.trail_sense.tools.maps.infrastructure.resize
import com.kylecorry.trailsensecore.domain.pixels.PercentCoordinate
import com.kylecorry.trailsensecore.infrastructure.images.BitmapUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils


class PerspectiveCorrectionView : CanvasView {

    private var image: Bitmap? = null
    private var imagePath: String? = null
    @DrawableRes
    private var imageDrawable: Int? = null
    private var linesLoaded = false
    private var scale = 0.9f
    private val localFileService by lazy { LocalFileService(context) }
    private var topLeft = PixelCoordinate(0f, 0f)
    private var topRight = PixelCoordinate(0f, 0f)
    private var bottomLeft = PixelCoordinate(0f, 0f)
    private var bottomRight = PixelCoordinate(0f, 0f)
    private var movingCorner: Corner? = null

    var isPreview = false
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    private var primaryColor = Color.BLACK

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
        primaryColor = UiUtils.color(context, R.color.colorPrimary)
    }

    override fun draw() {
        if (image == null && imagePath != null){
            imagePath?.let {
                val file = localFileService.getFile(it, false)
                val bitmap = BitmapUtils.decodeBitmapScaled(
                    file.path,
                    width,
                    height
                )
                image = bitmap.resize(width, height)
                if (image != bitmap) {
                    bitmap.recycle()
                }
            }
        } else if (image == null && imageDrawable != null){
            imageDrawable?.let {
                val img = loadImage(it)
                image = img.resize(width, height)
                if (img != image) {
                    img.recycle()
                }
            }
        }

        val bitmap = image ?: return

        if (!linesLoaded){
            resetLines()
        }

        imageX = (width - bitmap.width * scale) / (2f)
        imageY = (height - bitmap.height * scale) / (2f)

        push()
        translate(imageX, imageY)
        scale(scale)
        if (isPreview){
            drawPreviewCanvas()
        } else {
            drawEditCanvas()
            drawMagnify()
        }
        pop()
    }

    private fun drawMagnify(){
        val image = image ?: return
        val corner = movingCorner ?: return
        val center = when (corner) {
            Corner.TopLeft -> topLeft
            Corner.TopRight -> topRight
            Corner.BottomLeft -> bottomLeft
            Corner.BottomRight -> bottomRight
        }
        val shader = BitmapShader(image, TileMode.CLAMP, TileMode.CLAMP)
        val matrix = Matrix()
        val shaderPaint = Paint()
        shaderPaint.shader = shader

        matrix.reset()
        matrix.postScale(1f, 1f)
        matrix.postTranslate(-center.x + width / 2f, -center.y + height / 2f)
        shader.setLocalMatrix(matrix)

        canvas.drawCircle(width / 2f, height / 2f, dp(32f), shaderPaint)
        stroke(primaryColor)
        noFill()
        strokeWeight(dp(2f))
        val plusSize = dp(8f)
        line(width / 2f - plusSize / 2f, height / 2f, width / 2f + plusSize / 2f, height / 2f)
        line(width / 2f, height / 2f - plusSize / 2f, width / 2f, height / 2f + plusSize / 2f)
    }

    private fun drawPreviewCanvas(){
        val bitmap = image ?: return
        val warped = bitmap.fixPerspective(getBounds())
        image(warped, 0f, 0f)
        if (warped != bitmap) {
            warped.recycle()
        }
    }

    private fun drawEditCanvas(){
        val bitmap = image ?: return

        image(bitmap, 0f, 0f)

        noStroke()
        fill(primaryColor)
        circle(topLeft.x, topLeft.y, dp(10f))
        circle(topRight.x, topRight.y, dp(10f))
        circle(bottomLeft.x, bottomLeft.y, dp(10f))
        circle(bottomRight.x, bottomRight.y, dp(10f))

        stroke(primaryColor)
        noFill()
        strokeWeight(dp(2f))
        line(topLeft.x, topLeft.y, topRight.x, topRight.y)
        line(topRight.x, topRight.y, bottomRight.x, bottomRight.y)
        line(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y)
        line(bottomLeft.x, bottomLeft.y, topLeft.x, topLeft.y)
    }

    private fun resetLines(){
        val image = image ?: return
        topLeft = PixelCoordinate(0f, 0f)
        topRight = PixelCoordinate(image.width.toFloat(), 0f)
        bottomLeft = PixelCoordinate(0f, image.height.toFloat())
        bottomRight = PixelCoordinate(image.width.toFloat(), image.height.toFloat())
        linesLoaded = true
    }

    fun getBounds(): PixelBounds {
        return PixelBounds(topLeft, topRight, bottomLeft, bottomRight)
    }

    fun getPercentBounds(): PercentBounds? {
        val image = image ?: return null
        return PercentBounds(
            PercentCoordinate(topLeft.x / image.width, topLeft.y / image.height),
            PercentCoordinate(topRight.x / image.width, topRight.y / image.height),
            PercentCoordinate(bottomLeft.x / image.width, bottomLeft.y / image.height),
            PercentCoordinate(bottomRight.x / image.width, bottomRight.y / image.height),
        )
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val position = PixelCoordinate(event.x / scale - imageX / scale, event.y / scale - imageY / scale)

        when (event.action){
            MotionEvent.ACTION_DOWN -> {
                val radius = dp(10f)

                when {
                    topLeft.distanceTo(position) <= radius -> {
                        movingCorner = Corner.TopLeft
                    }
                    topRight.distanceTo(position) <= radius -> {
                        movingCorner = Corner.TopRight
                    }
                    bottomLeft.distanceTo(position) <= radius -> {
                        movingCorner = Corner.BottomLeft
                    }
                    bottomRight.distanceTo(position) <= radius -> {
                        movingCorner = Corner.BottomRight
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (movingCorner) {
                    Corner.TopLeft -> {
                        topLeft = constrain(position, null, bottomLeft.y, null, topRight.x)
                    }
                    Corner.TopRight -> {
                        topRight = constrain(position, null, bottomRight.y, topLeft.x, null)
                    }
                    Corner.BottomLeft -> {
                        bottomLeft = constrain(position, topLeft.y, null, null, bottomRight.x)
                    }
                    Corner.BottomRight -> {
                        bottomRight = constrain(position, topRight.y, null, bottomLeft.x, null)
                    }
                    null -> {}
                }
            }
            MotionEvent.ACTION_UP -> {
                movingCorner = null
            }
        }
        invalidate()
        return true
    }

    private fun constrain(pixel: PixelCoordinate, top: Float?, bottom: Float?, left: Float?, right: Float?): PixelCoordinate {
        var x = pixel.x
        var y = pixel.y

        if (top != null && y < top){
            y = top
        }

        if (bottom != null && y > bottom){
            y = bottom
        }

        if (left != null && x < left){
            x = left
        }

        if (right != null && x > right){
            x = right
        }

        return PixelCoordinate(x, y)
    }


    private enum class Corner {
        TopLeft,
        TopRight,
        BottomLeft,
        BottomRight
    }

}