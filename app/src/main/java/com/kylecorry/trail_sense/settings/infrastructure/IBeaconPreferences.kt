package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod

interface IBeaconPreferences {
    val showLastSignalBeacon: Boolean
    var beaconSort: BeaconSortMethod
    var defaultBeaconColor: AppColor
}