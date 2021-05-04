package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.core.math.MathUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trailsensecore.domain.light.LightIntensity
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.canvas.getMaskedBitmap
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.*


class MaskedProgressView : View {
    private lateinit var paint: Paint
    private var progressBackground: Bitmap? = null
    private lateinit var backgroundMask: Bitmap
    private lateinit var progressBitmap: Bitmap
    private var isInit = false

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

    var progressColor: Int = AppColor.Orange.color
        set(value) {
            field = value
            invalidate()
        }

    private var imageSize = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            paint = Paint()
            paint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10f,
                resources.displayMetrics
            )
            imageSize = min(width, height)
            isInit = true
            val drawable = UiUtils.drawable(context, R.drawable.ic_battery)
            drawable?.setTint(UiUtils.androidBackgroundColorSecondary(context))
            progressBackground = drawable?.toBitmap(imageSize, imageSize)

            backgroundMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(backgroundMask)
            paint.color = Color.WHITE
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            tempCanvas.drawBitmap(
                progressBackground!!,
                width / 2f - imageSize / 2f,
                height / 2f - imageSize / 2f,
                paint
            )
            progressBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        canvas.drawColor(Color.TRANSPARENT)
        drawProgress(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }


    private fun drawProgress(canvas: Canvas) {
        progressBackground ?: return
        canvas.drawBitmap(
            progressBackground!!,
            width / 2f - imageSize / 2f,
            height / 2f - imageSize / 2f,
            paint
        )

        val progressBitmap = canvas.getMaskedBitmap(backgroundMask, progressBitmap) {
            it.drawColor(Color.TRANSPARENT)
            paint.color = progressColor
            val left = width / 2f - imageSize / 2f
            val right = width / 2f + imageSize / 2f
            val top = height / 2f - imageSize / 2f
            val bottom = height / 2f + imageSize / 2f

            if (horizontal){
                it.drawRect(left, top, left + progress * imageSize, bottom, paint)
            } else {
                it.drawRect(left, bottom - progress * imageSize, right, bottom, paint)
            }
        }

        canvas.drawBitmap(
            progressBitmap,
            0f,
            0f,
            paint
        )

    }


}