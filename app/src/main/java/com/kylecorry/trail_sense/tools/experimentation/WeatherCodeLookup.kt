package com.kylecorry.trail_sense.tools.experimentation

import androidx.annotation.IdRes
import com.kylecorry.trail_sense.R

object WeatherCodeLookup {

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing rime fog"
            51 -> "Drizzle: Light intensity"
            53 -> "Drizzle: Moderate intensity"
            55 -> "Drizzle: Dense intensity"
            56 -> "Freezing Drizzle: Light intensity"
            57 -> "Freezing Drizzle: Dense intensity"
            61 -> "Rain: Slight intensity"
            63 -> "Rain: Moderate intensity"
            65 -> "Rain: Heavy intensity"
            66 -> "Freezing Rain: Light intensity"
            67 -> "Freezing Rain: Heavy intensity"
            71 -> "Snow fall: Slight intensity"
            73 -> "Snow fall: Moderate intensity"
            75 -> "Snow fall: Heavy intensity"
            77 -> "Snow grains"
            80 -> "Rain showers: Slight intensity"
            81 -> "Rain showers: Moderate intensity"
            82 -> "Rain showers: Violent intensity"
            85 -> "Snow showers slight intensity"
            86 -> "Snow showers heavy intensity"
            95 -> "Thunderstorm: Slight or moderate"
            96 -> "Thunderstorm with slight hail"
            99 -> "Thunderstorm with heavy hail"
            else -> "Unknown weather code $code"
        }
    }

    // TODO: Replace sun icons with moon at night
    @IdRes
    fun getWeatherImage(code: Int): Int {
        return when (code) {
            0, 1 -> R.drawable.sunny
            2 -> R.drawable.cloudy
            3 -> R.drawable.cloudy
            45, 48 -> R.drawable.cloudy
            51, 53, 61, 63, 80, 81, 85 -> R.drawable.light_rain
            55, 65, 82, 86 -> R.drawable.light_rain
            56, 57, 66, 67 -> R.drawable.ic_precipitation
            71, 73, 75, 77 -> R.drawable.ic_precipitation_snow
            95, 96, 99 -> R.drawable.storm
            else -> R.drawable.ic_info
        }
    }

}