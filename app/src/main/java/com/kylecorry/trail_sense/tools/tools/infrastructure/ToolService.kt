package com.kylecorry.trail_sense.tools.tools.infrastructure

import java.time.Duration

interface ToolService {

    val id: String

    val name: String

    fun getFrequency(): Duration

    fun isRunning(): Boolean

    fun isEnabled(): Boolean

    fun isBlocked(): Boolean

    suspend fun enable()

    suspend fun disable()

    suspend fun restart()

    suspend fun stop()
}
