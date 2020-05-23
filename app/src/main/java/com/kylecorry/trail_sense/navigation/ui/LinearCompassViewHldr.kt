package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.kylecorry.trail_sense.shared.math.deltaAngle
import kotlin.math.roundToInt

class LinearCompassViewHldr(private val compass: LinearCompassView, private val beaconIndicators: List<ImageView>) :
    ICompassView {

    override var visibility: Int
        get() = compass.visibility
        set(value){
            compass.visibility = value
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.setDegrees(value)
            field = value
        }

    override var beacons: List<Float> = listOf()
        set(value) {
            beaconIndicators.forEachIndexed { index, indicator ->
                if (index < beacons.size){
                    showBeacon(indicator, beacons[index])
                } else {
                    hideBeacon(indicator)
                }
            }
            field = value
        }

    private fun showBeacon(indicator: ImageView, bearing: Float){
        indicator.visibility = View.VISIBLE
        indicator.rotation = 0f
        indicator.y = compass.top.toFloat()

        val w = compass.width

        val delta = deltaAngle(azimuth.roundToInt().toFloat(), bearing.roundToInt().toFloat())

        when {
            delta < -90 -> {
                indicator.x = compass.x
                indicator.rotation = -90f
            }
            delta > 90 -> {
                indicator.x = compass.x + compass.width - indicator.height.toFloat()
                indicator.rotation = 90f
            }
            else -> {
                val pct = (delta + 90) / 180f
                indicator.x = pct * w - indicator.width / 2f
            }
        }
    }

    private fun hideBeacon(indicator: ImageView){
        indicator.visibility = View.INVISIBLE
    }
}