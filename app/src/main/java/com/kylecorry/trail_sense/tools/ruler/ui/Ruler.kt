package com.kylecorry.trail_sense.tools.ruler.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlin.math.ceil

class Ruler(private val view: ConstraintLayout, private var units: DistanceUnits) {

    private val context = view.context
    private val userPrefs = UserPreferences(context)
    private var isRulerSetup = false
    private var areRulerTextViewsAligned = false
    private var tapBar: View? = null
    private var lastTap: Float? = null

    var onTap: ((centimeters: Float) -> Unit)? = null

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

    fun setUnits(newUnits: DistanceUnits){
        if (newUnits == units){
            return
        }
        units = newUnits
        isRulerSetup = false
        areRulerTextViewsAligned = false
        onUpdate()
    }

    fun show(){
        view.visibility = View.VISIBLE
        onUpdate()
    }

    fun hide(){
        view.visibility = View.GONE
    }

    fun clearTap(){
        tapBar?.visibility = View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun update() {
        val dpi = Screen.dpi(context)
        val scale = userPrefs.navigation.rulerScale
        val height =
            scale * view.height / dpi.toDouble() * if (units == DistanceUnits.Centimeters) 2.54 else 1.0

        if (height == 0.0 || context == null) {
            return
        }

        if (!isRulerSetup) {
            view.removeAllViews()
            val primaryColor = Resources.androidTextColorPrimary(context)

            val divisions = if (units == DistanceUnits.Inches) 8 else 10

            for (i in 0..ceil(height).toInt() * divisions) {
                val inches = i / divisions.toFloat()
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
                    units == DistanceUnits.Inches && inches % 0.25 == 0.0 -> {
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
            tapBar = View(context)
            tapBar?.let {
                it.setBackgroundColor(Resources.getAndroidColorAttr(context, R.attr.colorPrimary))
                val layoutParams = ConstraintLayout.LayoutParams(1, 4)
                it.layoutParams = layoutParams
                it.layoutParams.width = view.width
                it.visibility = View.INVISIBLE
                view.addView(it)
                if (lastTap != null){
                    tapBar?.visibility = View.VISIBLE
                    tapBar?.y = lastTap!!
                }
            }
            view.setOnTouchListener { _, event ->
                val tapPosition = event.y
                val tapOffsetPosition = tapPosition - context.resources.getDimensionPixelSize(R.dimen.ruler_top)
                val tapCm = scale * tapOffsetPosition / dpi * 2.54f
                if (onTap != null) {
                    onTap?.invoke(tapCm)
                    tapBar?.visibility = View.VISIBLE
                    tapBar?.y = tapPosition
                    lastTap = tapPosition
                } else {
                    tapBar?.visibility = View.INVISIBLE
                    lastTap = null
                }
                true
            }
        }

        isRulerSetup = true
    }

    companion object {
        fun measure(context: Context, pixels: Float): Distance {
            val prefs = UserPreferences(context)
            val dpi = Screen.dpi(context)
            val scale = prefs.navigation.rulerScale
            return Distance(scale * pixels / dpi, DistanceUnits.Inches).meters()
        }
    }

}