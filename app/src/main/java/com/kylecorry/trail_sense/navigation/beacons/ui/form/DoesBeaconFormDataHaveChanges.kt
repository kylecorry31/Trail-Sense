package com.kylecorry.trail_sense.navigation.beacons.ui.form

import com.kylecorry.andromeda.core.specifications.Specification

class DoesBeaconFormDataHaveChanges(private val original: CreateBeaconData) :
    Specification<CreateBeaconData>() {
    override fun isSatisfiedBy(value: CreateBeaconData): Boolean {
        return original.copy(
            elevation = original.elevation?.meters(),
            distanceTo = original.distanceTo?.meters()
        ) != value.copy(
            elevation = value.elevation?.meters(),
            distanceTo = value.distanceTo?.meters()
        )
    }
}