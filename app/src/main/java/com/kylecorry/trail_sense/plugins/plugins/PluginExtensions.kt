package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.ipc.InterprocessCommunicationResponse
import com.kylecorry.andromeda.ipc.client.InterprocessCommunicationClient
import com.kylecorry.andromeda.json.fromJson
import com.kylecorry.andromeda.json.toJsonBytes

const val PLUGIN_RESOURCE_SERVICE_ACTION = "com.kylecorry.trail_sense.PLUGIN_SERVICE"

fun pluginResourceServiceIntent(
    packageId: String
): Intent {
    return Intent(PLUGIN_RESOURCE_SERVICE_ACTION).apply {
        setPackage(packageId)
    }
}

suspend fun ipcSend(
    context: Context,
    packageId: String,
    route: String,
    payload: Any? = null,
    actionId: String = PLUGIN_RESOURCE_SERVICE_ACTION
): InterprocessCommunicationResponse {

    val bytes = when (payload) {
        null -> {
            null
        }

        is ByteArray -> {
            payload
        }

        is String -> {
            payload.toByteArray()
        }

        else -> {
            payload.toJsonBytes()
        }
    }

    val intent = Intent(actionId).apply {
        setPackage(packageId)
    }
    return InterprocessCommunicationClient(context, intent).use {
        it.connectAndSend(route, bytes)
    }
}

inline fun <reified T> InterprocessCommunicationResponse.payloadAsJson(): T? {
    return payload?.fromJson()
}

fun InterprocessCommunicationResponse.payloadAsString(): String? {
    return payload?.toString(Charsets.UTF_8)
}