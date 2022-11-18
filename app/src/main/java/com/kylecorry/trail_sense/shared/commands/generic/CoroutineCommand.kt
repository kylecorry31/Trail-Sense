package com.kylecorry.trail_sense.shared.commands.generic

interface CoroutineCommand<T> {

    suspend fun execute(value: T)

}