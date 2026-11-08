package com.kylecorry.trail_sense.tools.beacons.domain

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.colors.AppColor

data class Beacon(
    override val id: Long,
    override val name: String,
    val coordinate: Coordinate,
    val visible: Boolean = true,
    val comment: String? = null,
    override val parentId: Long? = null,
    val elevation: Float? = null,
    val temporary: Boolean = false,
    val owner: BeaconOwner = BeaconOwner.User,
    @ColorInt val color: Int = Color.BLACK,
    val icon: BeaconIcon? = null
) : IBeacon {
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
