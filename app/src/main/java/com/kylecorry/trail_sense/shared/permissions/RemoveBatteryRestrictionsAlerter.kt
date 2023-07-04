package com.kylecorry.trail_sense.shared.permissions

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter

class RemoveBatteryRestrictionsAlerter<T>(private val fragment: T) :
    IAlerter where T : Fragment, T : IPermissionRequester {

    override fun alert() {
        val context = fragment.requireContext()
        CustomUiUtils.snackbar(
            fragment,
            context.getString(R.string.battery_settings_limit_accuracy),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.learn_more)
        ) {
            fragment.requestPermission(
                SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS,
                PermissionRationale(
                    context.getString(R.string.battery_settings_limit_accuracy),
                    MarkdownService(context).toMarkdown(context.getString(R.string.battery_usage_restricted_benefit)),
                    ok = context.getString(R.string.settings),
                )
            ) {
                if (Permissions.hasPermission(context, SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS)) {
                    fragment.toast(context.getString(R.string.battery_usage_allowed))
                } else {
                    fragment.toast(context.getString(R.string.battery_usage_restricted))
                }
            }
        }
    }

}