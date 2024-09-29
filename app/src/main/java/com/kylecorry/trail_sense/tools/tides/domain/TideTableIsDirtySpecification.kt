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
                original.isSemidiurnal == value.isSemidiurnal &&
                original.estimator == value.estimator &&
                tidesEqual(original.tides, value.tides) &&
                original.lunitidalInterval == value.lunitidalInterval &&
                original.lunitidalIntervalIsUtc == value.lunitidalIntervalIsUtc)
    }

    private fun tidesEqual(original: List<Tide>, updated: List<Tide>): Boolean {
        return original.size == updated.size && original.containsAll(updated)
    }
}