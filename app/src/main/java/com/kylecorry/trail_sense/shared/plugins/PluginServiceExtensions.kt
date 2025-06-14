package com.kylecorry.trail_sense.shared.plugins

suspend fun <T : PluginService<S>, S, U> T.connectAndRun(block: suspend T.() -> U): U {
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