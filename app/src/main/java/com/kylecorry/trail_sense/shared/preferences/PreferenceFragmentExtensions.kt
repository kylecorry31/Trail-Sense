package com.kylecorry.trail_sense.shared.preferences

import androidx.preference.Preference
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units

fun AndromedaPreferenceFragment.setupNotificationSetting(
    key: String,
    channelId: String,
    channelName: String
) {
    val pref = preferenceManager.findPreference<Preference>(key)
    val summaryProvider = {
        val isBlocked = Notify.isChannelBlocked(requireContext(), channelId)
        if (isBlocked) getString(R.string.off) else getString(R.string.on)
    }

    pref?.title = getString(R.string.notifications_channel, channelName)

    pref?.summary = summaryProvider.invoke()

    pref?.setOnPreferenceClickListener {
        val intent = Intents.notificationSettings(requireContext(), channelId)
        getResult(intent) { _, _ ->
            pref.summary = summaryProvider.invoke()
        }
        true
    }
}

fun AndromedaPreferenceFragment.setupDistanceSetting(
    holderKey: String,
    getDistance: () -> Distance?,
    setDistance: (Distance?) -> Unit,
    units: List<DistanceUnits>,
    showFeetAndInches: Boolean = false,
    decimalPlacesOverride: Int? = null,
    description: String? = null
) {
    val pref = preferenceManager.findPreference<Preference>(holderKey)
    val formatter = FormatService.getInstance(requireContext())
    val sortedUnits = formatter.sortDistanceUnits(units)
    pref?.setOnPreferenceClickListener {
        CustomUiUtils.pickDistance(
            requireContext(),
            sortedUnits,
            getDistance(),
            pref.title.toString(),
            showFeetAndInches = showFeetAndInches,
            description = description
        ) { distance, cancelled ->
            if (cancelled) {
                return@pickDistance
            }
            setDistance(distance)

            // Reload the current because it will be in relative units
            val current = getDistance()
            pref.summary = current?.let {
                formatter.formatDistance(
                    current,
                    decimalPlacesOverride ?: Units.getDecimalPlaces(current.units)
                )
            } ?: getString(R.string.dash)
        }
        true
    }
    val current = getDistance()
    pref?.summary = current?.let {
        formatter.formatDistance(
            current,
            decimalPlacesOverride ?: Units.getDecimalPlaces(current.units)
        )
    } ?: getString(R.string.dash)
}