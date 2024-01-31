package com.kylecorry.trail_sense.tools.augmented_reality.ui

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class ARMode(override val id: Long) : Identifiable {
    Normal(1),
    Astronomy(2),
}