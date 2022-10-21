package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.ceres.list.ListItem

interface WeatherField {

    fun getListItem(context: Context): ListItem?

}