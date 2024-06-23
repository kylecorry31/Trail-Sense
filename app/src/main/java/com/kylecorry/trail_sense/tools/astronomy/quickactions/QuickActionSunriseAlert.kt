package com.kylecorry.trail_sense.tools.astronomy.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import java.time.Duration

class QuickActionSunriseAlert(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }

    private fun isOn(): Boolean {
        return prefs.astronomy.sendSunriseAlerts
    }

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_sunrise_quick_action)
    }

    override fun onClick() {
        super.onClick()
        if (isOn()) {
            prefs.astronomy.sendSunriseAlerts = false
            updateState()
        } else if (fragment is IPermissionRequester) {
            SunriseAlarmReceiver.enable(fragment, true)
            val alertTime = Duration.ofMinutes(prefs.astronomy.sunriseAlertMinutesBefore)
            val formattedAlertTime = formatter.formatDuration(alertTime)
            fragment.toast(context.getString(R.string.sunrise_alert_scheduled, formattedAlertTime))
            updateState()
        }
    }

    private fun updateState() {
        setState(isOn())
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }
}