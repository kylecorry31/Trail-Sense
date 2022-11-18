package com.kylecorry.trail_sense.shared.commands.generic

class ComposedCommand<T>(vararg val commands: Command<T>) : Command<T> {
    override fun execute(value: T) {
        commands.forEach { it.execute(value) }
    }
}