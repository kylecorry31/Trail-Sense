package com.kylecorry.trail_sense.plugins.plugins

suspend fun <T : PluginServiceConnection<S>, S, U> T.connectAndRun(block: suspend T.() -> U): U {
    val success = connect()
    if (!success) {
        throw IllegalStateException("Failed to connect to service")
    }
    try {
        waitUntilConnected()
        return block(this)
    } finally {
        disconnect()
    }
}