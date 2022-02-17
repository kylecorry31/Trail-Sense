package com.kylecorry.trail_sense.navigation.beacons.ui.form

import com.kylecorry.andromeda.core.specifications.Specification

class DoesBeaconFormDataHaveChanges(private val original: CreateBeaconData) : Specification<CreateBeaconData>() {
    override fun isSatisfiedBy(value: CreateBeaconData): Boolean {
        // TODO: Handle blanks vs nulls
        return original != value
    }
}