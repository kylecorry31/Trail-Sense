package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.getColorOrThrow
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.min


class MaskedProgressView : CanvasView {
    private lateinit var backgroundBitmap: Bitmap

    var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var horizontal: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    var progressColor: Int = AppColor.Orange.color
        set(value) {
            field = value
            invalidate()
        }

    @DrawableRes
    private var backgroundImageId: Int? = null

    @ColorInt
    private var backgroundColor: Int? = null


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        if (attrs != null) {
            loadAttributes(attrs)
        }
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        if (attrs != null) {
            loadAttributes(attrs)
        }
    }

    init {
        runEveryCycle = false
    }

    private fun loadAttributes(attrs: AttributeSet){
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MaskedProgressView,
            0,
            0
        )
        backgroundImageId = a.getResourceId(R.styleable.MaskedProgressView_android_drawable, -1)
        backgroundColor = try {
            a.getColorOrThrow(R.styleable.MaskedProgressView_android_drawableTint)
        } catch (e: Exception) {
            null
        }

        progressColor = try {
            a.getColorOrThrow(R.styleable.MaskedProgressView_android_progressTint)
        } catch (e: Exception){
            Resources.color(context, R.color.colorPrimary)
        }

        horizontal = a.getBoolean(R.styleable.MaskedProgressView_horizontal, true)

        progress = a.getInt(R.styleable.MaskedProgressView_android_progress, 0) / 100f
        a.recycle()
    }

    override fun setup() {
        val imageSize = min(width, height)
        backgroundBitmap = loadImage(backgroundImageId ?: R.drawable.rectangle, imageSize, imageSize)
        imageMode(ImageMode.Center)
        noStroke()
    }

    override fun draw() {
        clear()
        drawBackground()
        drawProgress()
    }

    private fun drawProgress(){
        val progressBitmap = mask(backgroundBitmap){
            fill(progressColor)
            val w = backgroundBitmap.width.toFloat()
            val h = backgroundBitmap.height.toFloat()

            if (horizontal){
                rect(0f, h, w * progress, h)
            } else {
                rect(0f, h * (1 - progress), w, h * progress)
            }
        }

        image(progressBitmap, width / 2f, height / 2f)
        progressBitmap.recycle()
    }

    private fun drawBackground(){
        if (backgroundColor == null){
            noTint()
        } else {
            tint(backgroundColor ?: Color.BLACK)
        }
        image(backgroundBitmap, width / 2f, height / 2f)
        noTint()
    }


}