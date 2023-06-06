package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import java.time.LocalDate

class MeteorShowerListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
    ): ListItem? = onDefault {
        val shower = astronomyService.getMeteorShower(location, date) ?: return@onDefault null

        listItem(
            3,
            context.getString(R.string.meteor_shower),
            context.getString(R.string.meteors_per_hour, shower.shower.rate),
            time(shower.peak, date),
            ResourceListIcon(R.drawable.ic_meteor, secondaryColor)
        ) {}
    }


}