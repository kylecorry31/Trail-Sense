package com.kylecorry.trail_sense.shared.beacons

import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate

data class Beacon(
    override val id: Long,
    override val name: String,
    val coordinate: Coordinate,
    val visible: Boolean = true,
    val comment: String? = null,
    val beaconGroupId: Long? = null,
    val elevation: Float? = null,
    val temporary: Boolean = false,
    val owner: BeaconOwner = BeaconOwner.User,
    @ColorInt val color: Int
) : IBeacon