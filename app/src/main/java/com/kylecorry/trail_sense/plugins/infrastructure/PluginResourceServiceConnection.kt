package com.kylecorry.trail_sense.plugins.infrastructure

import android.content.Context
import com.kylecorry.andromeda.ipc.InterprocessCommunicationRequest
import com.kylecorry.andromeda.ipc.InterprocessCommunicationResponse
import com.kylecorry.andromeda.ipc.client.InterprocessCommunicationClient
import com.kylecorry.andromeda.json.toJsonBytes
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import java.io.Closeable
import java.time.Duration

class PluginResourceServiceConnection(private val context: Context, private val packageId: String) :
    Closeable {
    private val service =
        InterprocessCommunicationClient(context, pluginResourceServiceIntent(packageId))
    private val plugins = getAppService<PluginSubsystem>()

    suspend fun send(
        route: String,
        payload: Any? = null,
        requiredPermissions: List<String> = emptyList(),
        timeout: Duration = Duration.ofSeconds(10),
        stayConnected: Boolean = true
    ): InterprocessCommunicationResponse? {
        if (!canInteractWithPlugin(requiredPermissions)) {
            return null
        }

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

        return service.connectAndSend(
            route,
            InterprocessCommunicationRequest(payload = bytes),
            timeout,
            stayConnected = stayConnected
        )
    }

    private suspend fun canInteractWithPlugin(requiredPermissions: List<String>): Boolean {
        if (!plugins.isConnected(packageId)) {
            return false
        }

        return requiredPermissions.isEmpty() || requiredPermissions.all {
            Permissions.hasPermission(context, packageId, it)
        }
    }

    override fun close() {
        service.close()
    }
}
