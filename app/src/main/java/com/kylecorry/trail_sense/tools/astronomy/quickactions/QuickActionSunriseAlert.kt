package com.kylecorry.trail_sense.tools.astronomy.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class QuickActionSunriseAlert(btn: ImageButton, fragment: Fragment) :
    ToolServiceQuickAction(
        btn,
        fragment,
        AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS,
        AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_STATE_CHANGED
    ) {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }


    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_sunrise_quick_action)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.ASTRONOMY)
        return true
    }

    override fun onClick() {
        super.onClick()
        fragment.inBackground {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    if (fragment is IPermissionRequester) {
                        SunriseAlarmReceiver.enable(fragment, true)
                        val alertTime =
                            Duration.ofMinutes(prefs.astronomy.sunriseAlertMinutesBefore)
                        val formattedAlertTime = formatter.formatDuration(alertTime)
                        fragment.toast(
                            context.getString(
                                R.string.sunrise_alert_scheduled,
                                formattedAlertTime
                            )
                        )
                    }
                }

                else -> return@inBackground
            }
        }
    }
}