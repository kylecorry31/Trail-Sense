package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.views.list.ListItem

interface WeatherField {

    fun getListItem(context: Context): ListItem?

}