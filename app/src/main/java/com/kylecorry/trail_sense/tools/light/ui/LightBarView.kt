package com.kylecorry.trail_sense.tools.light.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.core.math.MathUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.light.domain.LightService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.*

class LightBarView : View {
    private lateinit var paint: Paint
    private var tree: Bitmap? = null
    private var isInit = false

    var gradient: List<Int> = listOf()
    private var candela: Float = 0f
    var units: DistanceUnits = DistanceUnits.Meters
    private val lightService = LightService()
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
            imageSize = min(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 64f,
                resources.displayMetrics
            ).toInt(), height)
            isInit = true
            val drawable = UiUtils.drawable(context, R.drawable.tree)
            drawable?.setTint(Color.WHITE)
            tree = drawable?.toBitmap(imageSize, imageSize)
        }
        canvas.drawColor(Color.BLACK)
        drawGradient(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    fun setCandela(candela: Float){
        this.candela = candela
        updateGradients()
    }

    fun updateGradients(){
        // TODO: Factor in units
        val intensities = (1..100).map {
            lightService.luxAtDistance(candela, Distance(it.toFloat(), DistanceUnits.Meters))
        }

        // TODO: Calculate distance of each intensity description


        gradient = getColors(intensities)
    }

    private fun drawGradient(canvas: Canvas) {
        if (gradient.isEmpty()){
            return
        }

        val gradWidth = width / gradient.size.toFloat()
        var start = 0f

        val meters = listOf(9, 24, 49, 74)

        for (i in gradient.indices){
            paint.color = gradient[i]
            if (meters.contains(i)) {
                val centerGrad = (start + gradWidth / 2f)
                val imageTop = height.toFloat() - imageSize - 14f
                canvas.drawBitmap(tree!!, centerGrad - (imageSize / 2f), imageTop, paint)
                val meter = (i + 1).toString()
                val rect = Rect()
                paint.getTextBounds(meter, 0, meter.length, rect)


                canvas.drawText(meter, centerGrad - (rect.width() / 2f), imageTop / 2f + rect.height() / 2f, paint)
            }
            canvas.drawRect(start, height.toFloat() - 14f - imageSize * (1 / 8f), start + gradWidth, height.toFloat(), paint)
            start += gradWidth
        }

    }

    private fun getColors(lux: List<Float>): List<Int> {
        val minLux = ln(0.25f)
        val maxLux = ln(200f)

        val pcts = lux.map { MathUtils.clamp((ln(it) - minLux) / (maxLux - minLux), 0f, 1f) }

        return pcts.map {
            val a = floor(it * 255).toInt()
            Color.argb(a, 255, 255, 255)
        }
    }


}