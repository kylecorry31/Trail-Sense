package com.kylecorry.trail_sense.shared.commands.generic

interface CoroutineValueCommand<T, S> {

    suspend fun execute(value: T): S

}