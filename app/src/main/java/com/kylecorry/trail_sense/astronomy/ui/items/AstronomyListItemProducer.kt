package com.kylecorry.trail_sense.astronomy.ui.items

import com.kylecorry.ceres.list.ListItem
import com.kylecorry.sol.units.Coordinate
import java.time.LocalDate

interface AstronomyListItemProducer {
    suspend fun getListItem(date: LocalDate, location: Coordinate): ListItem?
}