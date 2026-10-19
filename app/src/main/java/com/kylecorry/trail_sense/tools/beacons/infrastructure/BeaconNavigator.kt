package com.kylecorry.trail_sense.tools.beacons.infrastructure

import android.os.Bundle
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.IAppNavigation
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BeaconNavigator(
    private val beaconService: IBeaconService,
    private val navigation: IAppNavigation,
    private val navigator: Navigator,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val bundleFactory: () -> Bundle = { Bundle() }
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
            navigator.navigateTo(id)
            navigation.navigate(
                R.id.action_navigation,
                bundleFactory().apply {
                    putBoolean(NavigatorFragment.ARG_NAVIGATION_STARTED, true)
                }
            )
        }
    }

}
