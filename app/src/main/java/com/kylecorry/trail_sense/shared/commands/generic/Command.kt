package com.kylecorry.trail_sense.shared.commands.generic

interface Command<T> {
    fun execute(value: T)
}