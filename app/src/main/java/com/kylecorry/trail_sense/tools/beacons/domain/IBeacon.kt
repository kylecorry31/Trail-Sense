package com.kylecorry.trail_sense.tools.beacons.domain

import com.kylecorry.trail_sense.shared.grouping.Groupable

interface IBeacon: Groupable {
    val name: String
}