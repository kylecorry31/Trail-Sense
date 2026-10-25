package com.kylecorry.trail_sense.plugins.infrastructure

import android.content.Intent

const val PLUGIN_RESOURCE_SERVICE_ACTION = "com.kylecorry.trail_sense.PLUGIN_SERVICE"

fun pluginResourceServiceIntent(
    packageId: String
): Intent {
    return Intent(PLUGIN_RESOURCE_SERVICE_ACTION).apply {
        setPackage(packageId)
    }
}
