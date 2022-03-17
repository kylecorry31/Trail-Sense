package com.kylecorry.trail_sense.shared.commands

class ComposedCommand(vararg val commands: Command): Command {
    override fun execute() {
        commands.forEach(Command::execute)
    }
}