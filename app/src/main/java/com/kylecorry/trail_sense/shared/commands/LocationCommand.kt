package com.kylecorry.trail_sense.shared.commands

import com.kylecorry.andromeda.core.units.Coordinate

interface LocationCommand {

    fun execute(location: Coordinate)

}