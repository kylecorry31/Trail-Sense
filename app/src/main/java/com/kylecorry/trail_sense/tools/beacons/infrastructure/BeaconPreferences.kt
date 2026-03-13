package com.kylecorry.trail_sense.tools.beacons.infrastructure

import android.content.Context
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod

class BeaconPreferences(private val context: Context) : IBeaconPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    override val showLastSignalBeacon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_last_signal_beacon)) ?: true

    override var beaconSort: BeaconSortMethod by IntEnumPreference(
        cache,
        context.getString(R.string.pref_beacon_sort),
        BeaconSortMethod.values().associateBy { it.id.toInt() },
        BeaconSortMethod.Closest
    )

    override var defaultBeaconColor: AppColor
        get() {
            val id = cache.getLong(context.getString(R.string.pref_beacon_default_color))
            return AppColor.entries.firstOrNull { it.id == id } ?: AppColor.Orange
        }
        set(value) {
            cache.putLong(context.getString(R.string.pref_beacon_default_color), value.id)
        }
}
