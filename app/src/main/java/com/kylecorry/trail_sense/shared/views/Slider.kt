package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider as MaterialSlider
import com.kylecorry.andromeda.core.system.Resources

class Slider(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val slider = MaterialSlider(context)

    var valueFrom: Float
        get() = slider.valueFrom
        set(value) {
            slider.valueFrom = value
        }

    var valueTo: Float
        get() = slider.valueTo
        set(value) {
            slider.valueTo = value
        }

    var value: Float
        get() = slider.value
        set(value) {
            slider.value = value
        }

    var stepSize: Float
        get() = slider.stepSize
        set(value) {
            slider.stepSize = value
        }

    var labelBehavior: Int
        get() = slider.labelBehavior
        set(value) {
            slider.labelBehavior = value
        }

    var trackHeight: Int
        get() = slider.trackHeight
        set(value) {
            slider.trackHeight = value
        }

    var thumbHeight: Int
        get() = slider.thumbHeight
        set(value) {
            slider.thumbHeight = value
        }

    var trackStopIndicatorSize: Int
        get() = slider.trackStopIndicatorSize
        set(value) {
            slider.trackStopIndicatorSize = value
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        addView(
            slider,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun setLabelFormatter(formatter: LabelFormatter) {
        slider.setLabelFormatter(formatter)
    }

    fun addOnChangeListener(
        listener: (slider: Slider, value: Float, fromUser: Boolean) -> Unit
    ) {
        slider.addOnChangeListener { _, value, fromUser ->
            listener(this, value, fromUser)
        }
    }

    fun applyThinStyling() {
        trackHeight = Resources.dp(context, 8f).toInt()
        thumbHeight = Resources.dp(context, 32f).toInt()
        trackStopIndicatorSize = Resources.dp(context, 4f).toInt()
    }
}
