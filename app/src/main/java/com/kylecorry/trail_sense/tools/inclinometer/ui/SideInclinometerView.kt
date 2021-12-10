package com.kylecorry.trail_sense.tools.inclinometer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.hypot

class SideInclinometerView : CanvasView {

    private val formatter = FormatService(context)

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

    var angle = 0f
        set(value) {
            field = 90 - value
            invalidate()
        }

    var incline = 0f
        set(value) {
            field = value
            invalidate()
        }

    var color = AppColor.Gray.color
        set(value) {
            field = value
            invalidate()
        }

    var message = ""
        set(value) {
            field = value
            invalidate()
        }

    var locked = false
        set(value) {
            field = value
            invalidate()
        }

    private var lockedIcon: Bitmap? = null
    private var primaryColor: Int = Color.BLACK
    private var secondaryColor: Int = Color.BLACK
    private val groundPath = Path()

    override fun setup() {
        lockedIcon = loadImage(R.drawable.lock, dp(24f).toInt(), dp(24f).toInt())
        primaryColor = Resources.androidTextColorPrimary(context)
        secondaryColor = Resources.androidTextColorSecondary(context)
        groundPath.apply {
            val w = hypot(width.toFloat(), height.toFloat())
            val h = w / 2
            val dw = w - width
            addRect(-dw / 2, height / 2f, -dw / 2 + w, height / 2f + h, Path.Direction.CW)
        }
    }

    override fun draw() {
        push()
        rotate(angle)

        // Draw ground
        fill(color)
        opacity(127)
        path(groundPath)
        opacity(255)

        // Draw text
        noStroke()
        fill(primaryColor)
        textSize(sp(28f))
        textMode(TextMode.Center)
        val inclineText = formatter.formatDegrees(incline)
        val inclineTextHeight = textHeight(inclineText)
        text(inclineText, width / 2f, height / 2f - inclineTextHeight)

        if (locked) {
            lockedIcon?.let {
                val inclineTextWidth = textWidth(inclineText)
                imageMode(ImageMode.Center)
                tint(secondaryColor)
                image(
                    it,
                    width / 2f + inclineTextWidth / 2f + dp(32f),
                    height / 2f - inclineTextHeight
                )
                noTint()
            }
        }

        fill(secondaryColor)
        textSize(sp(14f))
        val messageHeight = textHeight("gt") // This avoid the text jumping around
        text(message, width / 2f, height / 2f + messageHeight)

        pop()
    }

    fun reset() {
        angle = 90f
        incline = 0f
        color = AppColor.Gray.color
        message = ""
        invalidate()
    }


}