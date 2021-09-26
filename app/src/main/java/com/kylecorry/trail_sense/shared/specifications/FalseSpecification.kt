package com.kylecorry.trail_sense.shared.specifications

import com.kylecorry.andromeda.core.specifications.Specification

class FalseSpecification<T> : Specification<T>() {
    override fun isSatisfiedBy(value: T): Boolean {
        return false
    }
}