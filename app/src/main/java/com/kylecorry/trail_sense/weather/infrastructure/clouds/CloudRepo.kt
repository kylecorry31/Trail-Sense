package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.content.Context
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.sol.science.meteorology.clouds.*

class CloudRepo(private val context: Context) {

    fun getClouds(): List<CloudType> {
        return CloudType.values().toList()
    }

    fun getCloudDescription(type: CloudType): String {
        return when (type) {
            CloudType.Cirrocumulus -> context.getString(R.string.cirrocumulus_desc)
            CloudType.Cirrostratus -> context.getString(R.string.cirrostratus_desc)
            CloudType.Altocumulus -> context.getString(R.string.altocumulus_desc)
            CloudType.Altostratus -> context.getString(R.string.altostratus_desc)
            CloudType.Nimbostratus -> context.getString(R.string.nimbostratus_desc)
            CloudType.Stratus -> context.getString(R.string.stratus_desc)
            CloudType.Stratocumulus -> context.getString(R.string.stratocumulus_desc)
            CloudType.Cumulus -> context.getString(R.string.cumulus_desc)
            CloudType.Cumulonimbus -> context.getString(R.string.cumulonimbus_desc)
            CloudType.Cirrus -> context.getString(R.string.cirrus_desc)
        }
    }

    fun getCloudName(type: CloudType): String {
        return when (type) {
            CloudType.Cirrus -> context.getString(R.string.cirrus)
            CloudType.Cirrocumulus -> context.getString(R.string.cirrocumulus)
            CloudType.Cirrostratus -> context.getString(R.string.cirrostratus)
            CloudType.Altocumulus -> context.getString(R.string.altocumulus)
            CloudType.Altostratus -> context.getString(R.string.altostratus)
            CloudType.Nimbostratus -> context.getString(R.string.nimbostratus)
            CloudType.Stratus -> context.getString(R.string.stratus)
            CloudType.Stratocumulus -> context.getString(R.string.stratocumulus)
            CloudType.Cumulus -> context.getString(R.string.cumulus)
            CloudType.Cumulonimbus -> context.getString(R.string.cumulonimbus)
        }
    }

    fun getCloudWeatherString(weather: CloudWeather): String {
        return when (weather) {
            CloudWeather.Fair -> context.getString(R.string.cloud_fair)
            CloudWeather.PrecipitationPossible -> context.getString(R.string.cloud_possible_rain)
            CloudWeather.PrecipitationLikely -> context.getString(R.string.cloud_likely_rain)
            CloudWeather.StormLikely -> context.getString(R.string.cloud_likely_storm)
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