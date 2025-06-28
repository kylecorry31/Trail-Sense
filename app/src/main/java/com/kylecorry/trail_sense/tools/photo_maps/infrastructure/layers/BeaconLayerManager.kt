package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import android.content.Context
import android.graphics.Color
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.layers.BeaconLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BeaconLayerManager(context: Context, private val layer: BeaconLayer) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val beaconRepo = BeaconRepo.getInstance(context)
    private val navigator = Navigator.getInstance(context)

    private var beacons = emptyList<Beacon>()
    private var destination: Beacon? = null

    override fun start() {
        layer.setOutlineColor(Color.WHITE)
        scope.launch {
            // Load beacons
            launch {
                beaconRepo.getBeacons().collect {
                    beacons = it.filter { beacon -> beacon.visible }
                    updateBeacons()
                }
            }

            // Load destination
            launch {
                navigator.destination.collect {
                    destination = it
                    updateBeacons()
                    layer.highlight(it)
                }
            }
        }
    }

    override fun stop() {
        scope.cancel()
    }

    private fun updateBeacons(){
        val beaconsToAdd = (beacons + listOfNotNull(destination)).distinctBy { it.id }
        layer.setBeacons(beaconsToAdd)
    }
}