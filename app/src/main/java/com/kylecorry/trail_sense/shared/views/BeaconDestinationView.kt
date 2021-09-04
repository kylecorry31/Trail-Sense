package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.ui.DestinationPanel
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.Position

class BeaconDestinationView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val destinationPanel: DestinationPanel

    init {
        inflate(context, R.layout.view_beacon_destination, this)
        destinationPanel = DestinationPanel(findViewById(R.id.navigation_sheet))
    }

    fun show(
        position: Position,
        destination: Beacon,
        declination: Float,
        usingTrueNorth: Boolean = true
    ) {
        destinationPanel.show(position, destination, declination, usingTrueNorth)
    }

    fun hide() {
        destinationPanel.hide()
    }

}