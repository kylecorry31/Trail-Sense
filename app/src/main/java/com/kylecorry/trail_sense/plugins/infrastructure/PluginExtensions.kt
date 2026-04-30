package com.kylecorry.trail_sense.plugins.infrastructure

import android.content.Intent
import com.kylecorry.andromeda.ipc.InterprocessCommunicationResponse
import com.kylecorry.andromeda.json.fromJson

const val PLUGIN_RESOURCE_SERVICE_ACTION = "com.kylecorry.trail_sense.PLUGIN_SERVICE"

fun pluginResourceServiceIntent(
    packageId: String
): Intent {
    return Intent(PLUGIN_RESOURCE_SERVICE_ACTION).apply {
        setPackage(packageId)
    }
}

inline fun <reified T> InterprocessCommunicationResponse.payloadAsJson(): T? {
    return payload?.fromJson()
}

fun InterprocessCommunicationResponse.payloadAsString(): String? {
    return payload?.toString(Charsets.UTF_8)
}
