package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.CompassCalibrationView
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences

class CompassCalibrationPrompt(
    private val context: Context,
    private val prefs: NavigationPreferences
) {

    fun show() {
        if (!prefs.showCalibrationOnNavigateDialog) {
            return
        }

        val calibrationView = CompassCalibrationView(context)
        val doNotAskAgain = CheckBox(context).apply {
            text = context.getString(R.string.do_not_ask_again)
        }
        val contentView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                calibrationView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Resources.dp(context, 200f).toInt()
                )
            )
            addView(
                doNotAskAgain,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = Resources.dp(context, 8f).toInt()
                }
            )
        }
        Alerts.dialog(
            context,
            context.getString(R.string.calibrate_compass_dialog_title),
            context.getString(
                R.string.calibrate_compass_on_navigate_dialog_content,
                context.getString(android.R.string.ok)
            ),
            contentView = contentView,
            cancelText = null,
            cancelOnOutsideTouch = false,
            scrollable = true
        ) { cancelled ->
            if (!cancelled && doNotAskAgain.isChecked) {
                prefs.showCalibrationOnNavigateDialog = false
            }
        }
    }
}
