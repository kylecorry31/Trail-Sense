package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter

class ARCalibrateDisclaimer(private val context: Context) : IAlerter {
    override fun alert() {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.calibrate),
            context.getString(R.string.ar_calibration_disclaimer),
            "ar_calibration_disclaimer",
        )
    }
}