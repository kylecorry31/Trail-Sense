package com.kylecorry.trail_sense.plugin.sample.service

import android.content.Context
import com.kylecorry.trail_sense.plugins.plugins.IpcServicePlugin
import com.kylecorry.trail_sense.plugins.plugins.Plugins
import java.io.Closeable

class SamplePluginService(context: Context) : Closeable {

    private val service =
        IpcServicePlugin(Plugins.getPackageId(context, Plugins.PLUGIN_SAMPLE), context)

    // Example of a custom endpoint
    suspend fun ping(): String? {
        return service.send("/ping")?.toString(Charsets.UTF_8)
    }

    override fun close() {
        service.close()
    }
}