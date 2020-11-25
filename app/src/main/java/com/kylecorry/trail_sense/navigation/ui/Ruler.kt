package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ViewMeasurementUtils
import kotlin.math.ceil

class Ruler(private val view: ConstraintLayout) {

    private val context = view.context
    private val userPrefs = UserPreferences(context)
    private var isRulerSetup = false
    private var areRulerTextViewsAligned = false

    val visible: Boolean
        get() = view.visibility == View.VISIBLE

    private val runnable = Runnable { onUpdate() }

    init {
        view.post(runnable)
    }

    private fun onUpdate(){
        if (!isRulerSetup || !areRulerTextViewsAligned){
            update()
            if (view.visibility == View.VISIBLE) {
                view.post(runnable)
            }
        }
    }

    fun show(){
        view.visibility = View.VISIBLE
        onUpdate()
    }

    fun hide(){
        view.visibility = View.GONE
    }

    private fun update() {
        val dpi = ViewMeasurementUtils.dpi(context)
        val scale = userPrefs.navigation.rulerScale
        val height =
            scale * view.height / dpi.toDouble() * if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters) 2.54 else 1.0

        if (height == 0.0 || context == null) {
            return
        }

        if (!isRulerSetup) {
            val primaryColor = UiUtils.androidTextColorPrimary(context)

            for (i in 0..ceil(height).toInt() * 8) {
                val inches = i / 8.0
                val tv = TextView(context)
                val bar = View(context)
                bar.setBackgroundColor(primaryColor)
                val layoutParams = ConstraintLayout.LayoutParams(1, 4)
                bar.layoutParams = layoutParams
                when {
                    inches % 1.0 == 0.0 -> {
                        bar.layoutParams.width = 48
                        tv.text = inches.toInt().toString()
                    }
                    inches % 0.5 == 0.0 -> {
                        bar.layoutParams.width = 36
                    }
                    inches % 0.25 == 0.0 -> {
                        bar.layoutParams.width = 24
                    }
                    else -> {
                        bar.layoutParams.width = 12
                    }
                }
                bar.y =
                    view.height * (inches / height).toFloat() + context.resources.getDimensionPixelSize(R.dimen.ruler_top)
                if (!tv.text.isNullOrBlank()) {
                    tv.setTextColor(primaryColor)
                    view.addView(tv)
                    tv.y = bar.y
                    tv.x =
                        bar.layoutParams.width.toFloat() + context.resources.getDimensionPixelSize(R.dimen.ruler_label)
                }

                view.addView(bar)
            }
        } else if (!areRulerTextViewsAligned) {
            for (view in view.children) {
                if (view.height != 0) {
                    areRulerTextViewsAligned = true
                }
                view.y -= view.height / 2f
            }
        }

        isRulerSetup = true
    }

}