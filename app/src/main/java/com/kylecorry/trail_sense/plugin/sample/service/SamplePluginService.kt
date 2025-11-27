package com.kylecorry.trail_sense.plugin.sample.service

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.ipc.client.InterprocessCommunicationClient
import com.kylecorry.trail_sense.plugins.plugins.Plugins
import com.kylecorry.trail_sense.plugins.plugins.payloadAsString
import com.kylecorry.trail_sense.plugins.plugins.pluginResourceServiceIntent
import java.io.Closeable

class SamplePluginService(private val context: Context) : Closeable {

    private fun getIntent(): Intent {
        return pluginResourceServiceIntent(Plugins.getPackageId(context, Plugins.PLUGIN_SAMPLE)!!)
    }

    private val service = InterprocessCommunicationClient(context, getIntent())

    // Example of a custom endpoint that stays connected (for faster communication)
    suspend fun ping(): String? {
        return service.connectAndSend("/ping", null, stayConnected = true).payloadAsString()
    }

    override fun close() {
        service.close()
    }
}