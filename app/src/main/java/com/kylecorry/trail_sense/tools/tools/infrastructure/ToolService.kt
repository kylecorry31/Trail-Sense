package com.kylecorry.trail_sense.tools.tools.infrastructure

import java.time.Duration

interface ToolService {

    /**
     * The unique ID of the service
     */
    val id: String

    /**
     * The name of the service
     */
    val name: String

    /**
     * Get the frequency at which the service is running
     */
    fun getFrequency(): Duration

    /**
     * Determines if the service is actively running
     */
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