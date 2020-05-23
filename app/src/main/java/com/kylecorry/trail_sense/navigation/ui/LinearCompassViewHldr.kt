package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.kylecorry.trail_sense.shared.Alignment
import com.kylecorry.trail_sense.shared.alignTo
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
        indicator.rotation = 0f

        val delta = deltaAngle(azimuth.roundToInt().toFloat(), bearing.roundToInt().toFloat())

        when {
            delta < -90 -> {
                alignTo(compass, indicator, Alignment.StartToStart, Alignment.StartToStart)
                indicator.rotation = -90f
            }
            delta > 90 -> {
                alignTo(compass, indicator, Alignment.StartToStart, Alignment.EndToEnd)
                indicator.rotation = 90f
            }
            else -> {
                val pct = (delta + 90) / 180f
                alignTo(compass, indicator, Alignment.StartToStart, Alignment.StartToStart, 0f, pct * compass.width - indicator.width / 2f)
            }
        }

        indicator.visibility = View.VISIBLE
    }

    private fun hideBeacon(indicator: ImageView){
        indicator.visibility = View.INVISIBLE
    }
}