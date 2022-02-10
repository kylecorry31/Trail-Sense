package com.kylecorry.trail_sense.navigation.beacons.infrastructure

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BeaconNavigator(
    private val beaconService: IBeaconService,
    private val navController: NavController
) : IBeaconNavigator {

    override suspend fun navigateTo(beacon: Beacon) {
        val id = if (beacon.id == 0L) {
            withContext(Dispatchers.IO) {
                beaconService.addBeacon(beacon)
            }
        } else {
            beacon.id
        }

        withContext(Dispatchers.Main) {
            navController.navigate(
                R.id.action_navigation,
                bundleOf("destination" to id)
            )
        }
    }

}