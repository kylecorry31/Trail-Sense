package com.kylecorry.trail_sense.navigation.beacons.domain

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.IMappableLocation

data class Beacon(
    override val id: Long,
    override val name: String,
    override val coordinate: Coordinate,
    val visible: Boolean = true,
    val comment: String? = null,
    override val parentId: Long? = null,
    val elevation: Float? = null,
    val temporary: Boolean = false,
    val owner: BeaconOwner = BeaconOwner.User,
    @ColorInt override val color: Int = Color.BLACK
) : IBeacon, IMappableLocation {
    override val isGroup = false
    override val count: Int? = null
}