package com.kylecorry.trail_sense.shared.permissions

import android.Manifest
import android.os.Build
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter

class BackgroundLocationAlerter<T>(private val fragment: T) :
    IAlerter where T : Fragment, T : IPermissionRequester {

    override fun alert() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val context = fragment.requireContext()
        CustomUiUtils.snackbar(
            fragment,
            context.getString(R.string.sunset_alerts_background_location_disclaimer),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.learn_more)
        ) {

            val markdown = AppServiceRegistry.get<MarkdownService>()
            val contents =
                markdown.toMarkdown(context.getString(R.string.access_background_location_rationale))

            fragment.dialog(
                context.getString(R.string.access_background_location),
                contents,
                okText = context.getString(R.string.settings),
                allowLinks = true
            ) { cancelled ->
                if (!cancelled) {
                    fragment.requestPermissions(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        // Do nothing
                    }
                }
            }
        }
    }

}