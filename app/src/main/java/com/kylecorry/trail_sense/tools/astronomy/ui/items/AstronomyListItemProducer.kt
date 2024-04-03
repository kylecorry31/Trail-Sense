package com.kylecorry.trail_sense.tools.astronomy.ui.items

import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.sol.units.Coordinate
import java.time.LocalDate

interface AstronomyListItemProducer {
    suspend fun getListItem(date: LocalDate, location: Coordinate, declination: Float): ListItem?
}