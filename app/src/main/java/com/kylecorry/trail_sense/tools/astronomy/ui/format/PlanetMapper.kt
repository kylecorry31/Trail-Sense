package com.kylecorry.trail_sense.tools.astronomy.ui.format

import android.content.Context
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.trail_sense.R

class PlanetMapper(private val context: Context) {
    fun getName(planet: Planet): String {
        return when (planet) {
            Planet.Mercury -> context.getString(R.string.planet_mercury)
            Planet.Venus -> context.getString(R.string.planet_venus)
            Planet.Mars -> context.getString(R.string.planet_mars)
            Planet.Jupiter -> context.getString(R.string.planet_jupiter)
            Planet.Saturn -> context.getString(R.string.planet_saturn)
            Planet.Uranus -> context.getString(R.string.planet_uranus)
            Planet.Neptune -> context.getString(R.string.planet_neptune)
            else -> "" // Not including Earth since that isn't going to show up anywhere
        }
    }

    fun getImage(planet: Planet): Int {
        return when (planet) {
            Planet.Mercury -> R.drawable.planet_mercury
            Planet.Venus -> R.drawable.planet_venus
            Planet.Mars -> R.drawable.planet_mars
            Planet.Jupiter -> R.drawable.planet_jupiter
            Planet.Saturn -> R.drawable.planet_saturn
            Planet.Uranus -> R.drawable.planet_uranus
            Planet.Neptune -> R.drawable.planet_netpune
            else -> R.drawable.bubble // Not including Earth since that isn't going to show up anywhere
        }
    }
}