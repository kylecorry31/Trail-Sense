package com.kylecorry.trail_sense.navigation.beacons.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.NameBeaconSort
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.IBeaconSort
import com.kylecorry.trail_sense.navigation.beacons.ui.list.IBeaconListItemMapper
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.shared.lists.GroupListManager
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object BeaconPickers {

    suspend fun pickBeacon(
        context: Context,
        title: String? = null,
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IBeaconSort = NameBeaconSort(),
        filter: (List<IBeacon>) -> List<IBeacon> = { it }
    ): Beacon? = suspendCoroutine { cont ->
        val loader = BeaconLoader(BeaconService(context), UserPreferences(context).navigation)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it)) }
        )
        val mapper = IBeaconListItemMapper(
            context,
            SensorService(context).getGPS(),
            { _, _ -> },
            { _, _ -> })
        val titleProvider = { beacon: IBeacon? ->
            beacon?.name ?: context.getString(R.string.beacons)
        }
        GroupablePickers.item(
            context,
            title,
            manager,
            mapper,
            titleProvider,
            context.getString(R.string.no_beacons),
            initialGroup,
            searchEnabled = true
        ) {
            cont.resume(it as Beacon?)
        }
    }

    suspend fun pickGroup(
        context: Context,
        title: String? = null,
        okText: String = context.getString(android.R.string.ok),
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IBeaconSort = NameBeaconSort(),
        filter: (List<BeaconGroup>) -> List<BeaconGroup> = { it }
    ): Pair<Boolean, BeaconGroup?> = suspendCoroutine { cont ->
        val loader = BeaconLoader(BeaconService(context), UserPreferences(context).navigation)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it.filterIsInstance<BeaconGroup>())) }
        )
        val mapper = IBeaconListItemMapper(
            context,
            SensorService(context).getGPS(),
            { _, _ -> },
            { _, _ -> })
        val titleProvider = { beacon: IBeacon? ->
            beacon?.name ?: context.getString(R.string.beacons)
        }
        GroupablePickers.group(
            context,
            title,
            okText,
            manager,
            mapper,
            titleProvider,
            context.getString(R.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as BeaconGroup?)
        }
    }

}