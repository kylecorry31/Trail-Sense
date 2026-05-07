package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.trail_sense.shared.grouping.Groupable

interface IOfflineMapFile : Groupable {
    val name: String
}
