package com.kylecorry.trail_sense.calibration.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class CompassCalibrationView : CanvasView {

    private val figure8Path = Path()
    private var figure8Width: Float = 0f
    private var figure8Height: Float = 0f
    private var phoneHeight: Float = 0f
    private var phoneWidth: Float = 0f

    private val tMin = -(PI / 2).toFloat()
    private val tMax = ((3 * PI) / 2).toFloat()

    private var start = System.currentTimeMillis()
    private val loopTime = 5f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun setup() {
        figure8Width = width.toFloat() * 0.8f / 2f
        figure8Height = height.toFloat() * 0.8f / 2f

        phoneWidth = dp(20f)
        phoneHeight = 2 * phoneWidth

        var j = tMin
        val step = 0.01f
        figure8Path.apply {
            moveTo(0f, 0f)
            while (j < tMax) {
                val pos = figure8(j)
                lineTo(pos.x * figure8Width, pos.y * figure8Height)
                j += step
            }
            close()
        }

        start = System.currentTimeMillis()
    }

    override fun draw() {
        background(AppColor.Blue.color)

        val time = (System.currentTimeMillis() - start) / 1000f

        if (time >= loopTime) {
            // TODO: Pause for a little
            start = System.currentTimeMillis()
        }

        push()
        translate(width / 2f, height / 2f)
        loop()

        val pos = figure8(getT(time)).let {
            Vector2(it.x * figure8Width, it.y * figure8Height)
        }
        val nextPos =
            figure8(getT(time + 0.001f)).let {
                Vector2(it.x * figure8Width, it.y * figure8Height)
            }

        val vec = nextPos.minus(pos)
        val heading = atan2(vec.y, vec.x).toDegrees()

        translate(pos.x, pos.y)
        rotate(heading + 90, 0f, 0f)
        // Simulates tilting the phone
//        scale((time / loopTime).coerceAtLeast(0.2f), 1f)
        phone()
        pop()
    }

    private fun getT(time: Float): Float {
        return SolMath.wrap(SolMath.map(time, 0f, loopTime, tMin, tMax), tMin, tMax)
    }

    private fun loop() {
        noFill()
        stroke(Color.BLACK)
        strokeWeight(dp(5f))
        path(figure8Path)
    }

    private fun phone() {
        fill(Color.BLACK)
        noStroke()
        rect(-phoneWidth / 2f, -phoneHeight / 2f, phoneWidth, phoneHeight)
        fill(Color.WHITE)
        val screenWidth = phoneWidth * 0.8f
        val screenHeight = phoneHeight * 0.8f
        rect(-screenWidth / 2f, -screenHeight / 2f, screenWidth, screenHeight)

        fill(AppColor.Orange.color)
        triangle(-15f, 0f, 0f, -15f, 15f, 0f)
    }

    private fun figure8(t: Float): Vector2 {
        val x = cos(t)
        val y = -sin(t) * cos(t)
        return Vector2(x, y)
    }

    companion object {
        fun withFrame(
            context: Context,
            width: Int = FrameLayout.LayoutParams.MATCH_PARENT,
            height: Int = FrameLayout.LayoutParams.MATCH_PARENT
        ): FrameLayout {
            val view = CompassCalibrationView(context)
            view.layoutParams = FrameLayout.LayoutParams(
                width,
                height
            )

            val holder = inflate(context, R.layout.view_alert_dialog, null) as FrameLayout
            holder.addView(view)
            return holder
        }
    }

}