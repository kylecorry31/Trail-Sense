package com.kylecorry.trail_sense.tools.comms.messaging.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment

class FragmentToolLocalMessaging : TrailSenseReactiveFragment(R.layout.fragment_comms_plugin) {

    override fun update() {
        val context = useAndroidContext()

        useEffect(context) {
            openApp(
                context,
                "com.kylecorry.trail_sense_comms",
                "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48"
            )
        }
    }

    private fun openApp(context: Context, packageName: String, tool: String) {
        if (!Package.isPackageInstalled(context, packageName)) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtras(Bundle().apply {
            putString("tool", tool)
        })
        context.startActivity(intent)
    }
}
