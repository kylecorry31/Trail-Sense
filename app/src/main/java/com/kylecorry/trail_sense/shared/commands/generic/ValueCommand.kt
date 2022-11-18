package com.kylecorry.trail_sense.shared.commands.generic

interface ValueCommand<T, S> {
    fun execute(value: T): S
}