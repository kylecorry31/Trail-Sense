package com.kylecorry.trail_sense.navigation.beacons.domain

data class BeaconGroup(override val id: Long, override val name: String, val parentId: Long? = null): IBeacon