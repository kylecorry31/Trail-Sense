package com.kylecorry.trail_sense.shared.grouping

data class MockGroup(
    override val id: Long,
    override val isGroup: Boolean,
    override val parentId: Long? = null,
    override val count: Int? = null
) : Groupable
