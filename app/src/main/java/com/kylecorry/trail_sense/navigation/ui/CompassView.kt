package com.kylecorry.trail_sense.navigation.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.kylecorry.trailsensecore.infrastructure.system.alignToVector

class CompassView(
    private val compass: ImageView,
    private val azimuthIndicator: ImageView
) :
    ICompassView {

    private val imageViews = mutableListOf<ImageView>()

    override var visibility: Int
        get() = compass.visibility
        set(value) {
            compass.visibility = value
            azimuthIndicator.visibility = value
            if (azimuthIndicator.height == 0) {
                azimuthIndicator.visibility = View.INVISIBLE
            }
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.rotation = -value
            if (azimuthIndicator.height != 0) {
                azimuthIndicator.visibility = visibility
            }
            alignToVector(
                compass,
                azimuthIndicator,
                compass.width / 2f + azimuthIndicator.height / 4f,
                90f
            )
            field = value
        }
    override var beacons: List<Float>
        get() = listOf()
        set(value) {}

    private fun showBeacon(indicator: ImageView, bearing: Float, offset: Float) {
        val adjBearing = bearing - azimuth + 90
        indicator.visibility = visibility
        displayDestinationBearing(indicator, adjBearing, offset)
    }

    private fun hideBeacon(indicator: ImageView) {
        indicator.visibility = View.INVISIBLE
    }

    override fun setOnClickListener(fn: () -> Unit) {
        compass.setOnClickListener { fn.invoke() }
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        // TODO: Switch to canvas drawing
        while (imageViews.size < indicators.size) {
            val imageView = ImageView(compass.context)
            (compass.parent as ConstraintLayout).addView(imageView)
            imageViews.add(imageView)
        }

        for (i in indicators.indices) {
            imageViews[i].setImageResource(indicators[i].icon)
            if (indicators[i].tint != null) {
                imageViews[i].imageTintList = ColorStateList.valueOf(indicators[i].tint!!)
            } else {
                imageViews[i].imageTintList = null
            }
            imageViews[i].alpha = indicators[i].opacity
            showBeacon(imageViews[i], indicators[i].bearing.value, indicators[i].verticalOffset)
        }

        if (imageViews.size > indicators.size) {
            for (i in indicators.size until imageViews.size) {
                hideBeacon(imageViews[i])
            }
        }

    }

    /**
     * Displays the destination bearing indicator around the compass
     * @param bearing the bearing in degrees to display the indicator at
     */
    private fun displayDestinationBearing(beaconIndicator: ImageView, bearing: Float, offset: Float) {
        alignToVector(
            compass,
            beaconIndicator,
            compass.width / 2f + beaconIndicator.height / 2f + 16f + offset,
            bearing
        )
        beaconIndicator.rotation = bearing - 90
    }

}