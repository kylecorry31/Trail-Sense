package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.content.Context
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import com.kylecorry.trailsensecore.domain.weather.clouds.*

// TODO: Extract these
class CloudRepo(private val context: Context) {

    private val cloudService = WeatherService()

    fun getClouds(): List<CloudType> {
        return CloudType.values().toList()
    }

    fun getCloudDescription(type: CloudType): String {
        return when (type) {
            CloudType.Cirrocumulus -> "Forms a wavy sheet, clouds almost look two dimensional."
            CloudType.Cirrostratus -> "Covers the sky, but the sun can be seen through it."
            CloudType.Altocumulus -> "Looks like puffy cotton balls."
            CloudType.Altostratus -> "Covers the sky, the sun can barely be seen."
            CloudType.Nimbostratus -> "Dark gray and covers the sky. It may be raining."
            CloudType.Stratus -> "Covers the sky, objects don't cast shadows."
            CloudType.Stratocumulus -> "Rolling masses which form a sheet."
            CloudType.Cumulus -> "Looks like puffy cotton balls."
            CloudType.Cumulonimbus -> "Dark gray, tall and anvil shape."
            CloudType.Cirrus -> "High and feathery."
        }
    }

    fun getCloudName(type: CloudType): String {
        return when (type) {
            CloudType.Cirrus -> "Cirrus"
            CloudType.Cirrocumulus -> "Cirrocumulus"
            CloudType.Cirrostratus -> "Cirrostratus"
            CloudType.Altocumulus -> "Altocumulus"
            CloudType.Altostratus -> "Altostratus"
            CloudType.Nimbostratus -> "Nimbostratus"
            CloudType.Stratus -> "Stratus"
            CloudType.Stratocumulus -> "Stratocumulus"
            CloudType.Cumulus -> "Cumulus"
            CloudType.Cumulonimbus -> "Cumulonimbus"
        }
    }

    fun getCloudWeatherString(weather: CloudWeather): String {
        return when (weather) {
            CloudWeather.Fair -> "Fair weather likely"
            CloudWeather.PrecipitationPossible -> "Precipitation possible"
            CloudWeather.PrecipitationLikely -> "Precipitation likely"
            CloudWeather.StormLikely -> "Storm likely"
        }
    }

    @DrawableRes
    fun getCloudWeatherIcon(weather: CloudWeather): Int {
        return when (weather) {
            CloudWeather.Fair -> R.drawable.partially_cloudy
            CloudWeather.PrecipitationPossible -> R.drawable.light_rain
            CloudWeather.PrecipitationLikely -> R.drawable.heavy_rain
            CloudWeather.StormLikely -> R.drawable.storm
        }
    }

    fun getCloudHeightString(height: CloudHeight): String {
        return when (height) {
            CloudHeight.Low -> "Low"
            CloudHeight.Middle -> "Middle"
            CloudHeight.High -> "High"
        }
    }

    @DrawableRes
    fun getCloudImage(type: CloudType): Int {
        return when(type){
            CloudType.Cirrus -> R.drawable.cirrus
            CloudType.Cirrocumulus -> R.drawable.cirrocumulus
            CloudType.Cirrostratus -> R.drawable.cirrostratus
            CloudType.Altocumulus -> R.drawable.altocumulus
            CloudType.Altostratus -> R.drawable.altostratus
            CloudType.Nimbostratus -> R.drawable.nimbostratus
            CloudType.Stratus -> R.drawable.stratus
            CloudType.Stratocumulus -> R.drawable.stratocumulus
            CloudType.Cumulus -> R.drawable.cumulus
            CloudType.Cumulonimbus -> R.drawable.cumulonimbus
        }
    }

}