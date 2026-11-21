package com.kylecorry.trail_sense.tools.tools.infrastructure

import java.time.Duration

interface ToolService {

    val id: String

    val name: String

    fun getFrequency(): Duration

    fun isRunning(): Boolean

    /**
     * Determines if the service is enabled. It is possible for a service to be enabled but not running.
     */
    fun isEnabled(): Boolean

    /**
     * Determines if the service is blocked from running (ex. low power mode)
     */
    fun isBlocked(): Boolean

    /**
     * Enable the service and attempt to start it (will not start if blocked)
     */
    suspend fun enable()

    /**
     * Disable the service (will not run again until re-enabled)
     */
    suspend fun disable()

    /**
     * Attempt to restart the service if it is enabled (will not start if blocked)
     */
    suspend fun restart()

    /**
     * Stop the service temporarily (will attempt to restart when the app is opened again)
     */
    suspend fun stop()
}
