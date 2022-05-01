package com.kylecorry.trail_sense.shared.grouping

import com.kylecorry.trail_sense.shared.database.Identifiable

interface Groupable : Identifiable {
    val parentId: Long?
    val isGroup: Boolean
    val count: Int?
}