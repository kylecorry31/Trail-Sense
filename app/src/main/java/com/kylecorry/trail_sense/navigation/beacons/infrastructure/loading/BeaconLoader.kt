package com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.shared.extensions.onIO

class BeaconLoader(
    private val beaconService: IBeaconService,
    private val prefs: IBeaconPreferences
) : IBeaconLoader {

    override suspend fun load(search: String?, group: Long?): List<IBeacon> = onIO {
        if (search.isNullOrBlank()) {
            getBeaconsByGroup(group)
        } else {
            getBeaconsBySearch(search, group)
        }
    }

    private suspend fun getBeaconsBySearch(search: String, groupFilter: Long?) = onIO {
        beaconService.search(search, groupFilter, applyGroupFilterIfNull = false)
    }

    private suspend fun getBeaconsByGroup(group: Long?) = onIO {
        val signal = if (group == null) getLastSignalBeacon() else null
        (beaconService.getBeacons(
            group,
            includeGroups = true
        ) + signal).filterNotNull()
    }

    private suspend fun getLastSignalBeacon(): Beacon? {
        return if (prefs.showLastSignalBeacon) {
            beaconService.getTemporaryBeacon(BeaconOwner.CellSignal)
        } else {
            null
        }
    }
}