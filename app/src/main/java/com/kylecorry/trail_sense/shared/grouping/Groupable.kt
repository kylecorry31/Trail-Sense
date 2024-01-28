package com.kylecorry.trail_sense.shared.grouping

import com.kylecorry.trail_sense.shared.data.Identifiable

interface Groupable : Identifiable {
    val parentId: Long?
    val isGroup: Boolean
    val count: Int?
}