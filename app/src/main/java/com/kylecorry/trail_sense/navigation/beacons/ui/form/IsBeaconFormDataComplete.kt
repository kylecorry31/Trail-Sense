package com.kylecorry.trail_sense.navigation.beacons.ui.form

import com.kylecorry.andromeda.core.specifications.Specification

class IsBeaconFormDataComplete : Specification<CreateBeaconData>() {
    override fun isSatisfiedBy(value: CreateBeaconData): Boolean {
        return value.name.isNotBlank() &&
                value.coordinate != null &&
                hasValidDistanceTo(value)
    }

    private fun hasValidDistanceTo(data: CreateBeaconData): Boolean {
        if (!data.createAtDistance) {
            return true
        }

        if (data.distanceTo == null) {
            return false
        }

        return data.bearingTo != null
    }
}