package com.kylecorry.trail_sense.tools.beacons.domain

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableLocation

data class Beacon(
    override val id: Long,
    override val name: String,
    override val coordinate: Coordinate,
    val visible: Boolean = true,
    val comment: String? = null,
    override val parentId: Long? = null,
    override val elevation: Float? = null,
    val temporary: Boolean = false,
    val owner: BeaconOwner = BeaconOwner.User,
    @ColorInt override val color: Int = Color.BLACK,
    override val icon: BeaconIcon? = null
) : IBeacon, IMappableLocation {
    override val isGroup = false
    override val count: Int? = null

    companion object {
        fun temporary(
            coordinate: Coordinate,
            id: Long = 0L,
            name: String = "",
            visible: Boolean = true,
            comment: String? = null,
            parentId: Long? = null,
            elevation: Float? = null,
            owner: BeaconOwner = BeaconOwner.User,
            @ColorInt color: Int = AppColor.Orange.color,
            icon: BeaconIcon? = null
        ): Beacon {
            return Beacon(
                id,
                name,
                coordinate,
                visible,
                comment,
                parentId,
                elevation,
                true,
                owner,
                color,
                icon
            )
        }
    }

}