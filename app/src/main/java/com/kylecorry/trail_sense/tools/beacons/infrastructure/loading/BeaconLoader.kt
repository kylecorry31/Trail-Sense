package com.kylecorry.trail_sense.tools.beacons.infrastructure.loading

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService

class BeaconLoader(
    private val beaconService: IBeaconService,
    private val prefs: IBeaconPreferences
) : ISearchableGroupLoader<IBeacon> {

    override suspend fun load(search: String?, group: Long?): List<IBeacon> = onIO {
        if (search.isNullOrBlank()) {
            getBeaconsByGroup(group)
        } else {
            getBeaconsBySearch(search, group)
        }
    }

    private suspend fun getBeaconsBySearch(search: String, groupFilter: Long?) = onIO {
        beaconService.search(search, groupFilter)
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

    override suspend fun getGroup(id: Long): IBeacon? {
        return beaconService.getGroup(id)
    }
}