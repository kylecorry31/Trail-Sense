package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.trail_sense.shared.grouping.Groupable

interface OfflineMapCatalogItem : Groupable {
    val name: String
}
