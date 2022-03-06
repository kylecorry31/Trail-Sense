package com.kylecorry.trail_sense.navigation.beacons.domain

import com.kylecorry.trail_sense.shared.grouping.Groupable

interface IBeacon: Groupable {
    val name: String
}