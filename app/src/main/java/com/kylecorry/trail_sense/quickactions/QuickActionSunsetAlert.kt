package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class QuickActionSunsetAlert(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }

    private fun isOn(): Boolean {
        return prefs.astronomy.sendSunsetAlerts
    }

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ic_sunset_notification)
        CustomUiUtils.setButtonState(button, false)

        button.setOnClickListener {
            if (isOn()){
                prefs.astronomy.sendSunsetAlerts = false
                updateState()
            } else if (fragment is IPermissionRequester) {
                SunsetAlarmReceiver.enable(fragment, true)
                val alertTime = Duration.ofMinutes(prefs.astronomy.sunsetAlertMinutesBefore)
                val formattedAlertTime = formatter.formatDuration(alertTime)
                fragment.toast(context.getString(R.string.sunset_alert_scheduled, formattedAlertTime))
                updateState()
            }
        }
    }

    private fun updateState(){
        CustomUiUtils.setButtonState(button, isOn())
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }
}