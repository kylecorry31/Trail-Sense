package com.kylecorry.trail_sense.navigation.beacons.infrastructure

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.shared.navigation.IAppNavigation
import com.kylecorry.trail_sense.shared.extensions.onIO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BeaconNavigator(
    private val beaconService: IBeaconService,
    private val navigation: IAppNavigation,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : IBeaconNavigator {

    override suspend fun navigateTo(beacon: Beacon) {
        val id = if (beacon.id == 0L) {
            onIO {
                beaconService.add(beacon)
            }
        } else {
            beacon.id
        }

        withContext(mainDispatcher) {
            navigation.navigate(
                R.id.action_navigation,
                listOf("destination" to id)
            )
        }
    }

}