package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.navigation.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.badge.CeresBadge
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class NorthReferenceBadge(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val badge = CeresBadge(context, attrs)
    private val formatter = FormatService.getInstance(context)

    var useTrueNorth: Boolean = false
        set(value) {
            field = value
            updateBadge()
        }

    var showLabel: Boolean = true
        set(value) {
            field = value
            updateBadge()
        }

    var showDetailsOnClick: Boolean = false
        set(value) {
            field = value
            if (value){
                badge.setOnClickListener {
                    showDescription()
                }
            } else {
                badge.setOnClickListener(null)
            }
        }

    init {
        badge.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        badge.setBackgroundTint(Color.TRANSPARENT)
        badge.setForegroundTint(Resources.androidTextColorSecondary(context))
        useTrueNorth = false
        addView(badge)
    }

    private fun updateBadge() {
        badge.setImageResource(formatter.getCompassReferenceIcon(useTrueNorth))
        val text = when {
            !showLabel -> null
            useTrueNorth -> context.getString(R.string.true_north)
            else -> context.getString(R.string.magnetic_north)
        }
        badge.setStatusText(text)
    }

    private fun showDescription(){
        val title = if (useTrueNorth){
            context.getString(R.string.true_north)
        } else {
            context.getString(R.string.magnetic_north)
        }

        val message = if (useTrueNorth){
            context.getString(R.string.true_north_description)
        } else {
            context.getString(R.string.magnetic_north_description)
        }

        Alerts.dialog(context, title, message, okText = context.getString(R.string.settings)){ cancelled ->
            if (!cancelled){
                findNavController().navigate(R.id.calibrateCompassFragment)
            }
        }
    }

}