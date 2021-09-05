package com.kylecorry.trail_sense.shared.commands

import com.kylecorry.sol.units.Coordinate

interface LocationCommand {

    fun execute(location: Coordinate)

}