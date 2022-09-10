package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.BeaconSortMethod

interface IBeaconPreferences {
    val showLastSignalBeacon: Boolean
    var beaconSort: BeaconSortMethod
}