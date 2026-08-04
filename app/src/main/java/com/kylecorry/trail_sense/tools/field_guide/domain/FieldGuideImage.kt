package com.kylecorry.trail_sense.tools.field_guide.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

data class FieldGuideImage(
    override val id: Long,
    val path: String,
    val caption: String? = null
) : Identifiable
