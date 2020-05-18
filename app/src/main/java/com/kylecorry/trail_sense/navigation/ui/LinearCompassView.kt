package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.kylecorry.trail_sense.shared.math.deltaAngle
import com.redinput.compassview.CompassView
import kotlin.math.roundToInt

class LinearCompassView(private val compass: CompassView, private val beaconIndicator: ImageView) :
    ICompassView {

    override var visibility: Int
        get() = compass.visibility
        set(value){
            compass.visibility = value
            beaconIndicator.visibility = value
            if (beacon == null) {
                beaconIndicator.visibility = View.INVISIBLE
            }
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.setDegrees(value)
            field = value
        }

    override var beacon: Float? = 0f
        set(value) {
            if (value == null){
                hideBeacon()
            } else {
                showBeacon(value)
            }
            field = value
        }

    private fun showBeacon(bearing: Float){
        beaconIndicator.rotation = 0f
        beaconIndicator.y = compass.top.toFloat()

        val w = compass.width

        val delta = deltaAngle(azimuth.roundToInt().toFloat(), bearing.roundToInt().toFloat())

        when {
            delta < -90 -> {
                beaconIndicator.x = compass.left.toFloat() - beaconIndicator.height / 2f
                beaconIndicator.rotation = -90f
            }
            delta > 90 -> {
                beaconIndicator.x = compass.right - beaconIndicator.height.toFloat()
                beaconIndicator.rotation = 90f
            }
            else -> {
                val pct = (delta + 90) / 180f
                beaconIndicator.x = pct * w - beaconIndicator.width / 2f
            }
        }
    }

    private fun hideBeacon(){
        beaconIndicator.visibility = View.INVISIBLE
    }
}