package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context
import android.content.Intent
import java.io.Closeable
import java.time.Duration

open class IpcServicePlugin(
    private val packageId: String?,
    protected val context: Context,
    private val actionId: String = "com.kylecorry.trail_sense.PLUGIN_SERVICE",
    private val stayConnected: Boolean = false
) : Closeable {

    private val connection = IpcServiceConnection(context, getIntent())

    private fun getIntent(): Intent {
        return Intent(actionId).apply {
            setPackage(packageId)
        }
    }

    suspend fun send(
        route: String,
        payload: ByteArray? = null,
        timeout: Duration = Duration.ofSeconds(10)
    ): ByteArray? {
        if (packageId == null) {
            throw Exception("Plugin not installed")
        }

        return try {
            val success = connection.connect()
            if (!success) {
                throw Exception("Could not connect to service")
            }
            connection.waitUntilConnected(timeout.toMillis())
            connection.send(route, payload)
        } finally {
            if (!stayConnected) {
                connection.close()
            }
        }
    }

    override fun close() {
        connection.close()
    }
}