package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.science.oceanography.Tide

class TideTableIsDirtySpecification(private val original: TideTable?) :
    Specification<TideTable?>() {
    override fun isSatisfiedBy(value: TideTable?): Boolean {
        if (original == null && value == null) {
            return false
        }

        if (original == null || value == null) {
            return true
        }

        return !(original.name == value.name &&
                original.location == value.location &&
                tidesEqual(original.tides, value.tides))
    }

    private fun tidesEqual(original: List<Tide>, updated: List<Tide>): Boolean {
        return original.size == updated.size && original.containsAll(updated)
    }
}