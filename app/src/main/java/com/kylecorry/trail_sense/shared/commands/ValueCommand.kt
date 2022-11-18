package com.kylecorry.trail_sense.shared.commands

interface ValueCommand<T> {
    fun execute(): T
}