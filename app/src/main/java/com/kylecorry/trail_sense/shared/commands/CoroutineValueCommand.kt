package com.kylecorry.trail_sense.shared.commands

interface CoroutineValueCommand<T> {

    suspend fun execute(): T

}