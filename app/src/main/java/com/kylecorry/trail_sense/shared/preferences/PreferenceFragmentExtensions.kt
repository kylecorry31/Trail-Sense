package com.kylecorry.trail_sense.shared.preferences

import androidx.preference.Preference
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.notificationSettings

fun AndromedaPreferenceFragment.setupNotificationSetting(key: String, channelId: String, channelName: String){
    val pref = preferenceManager.findPreference<Preference>(key)
    val summaryProvider = {
        val isBlocked = Notify.isChannelBlocked(requireContext(), channelId)
        val status = if (isBlocked) getString(R.string.off) else getString(R.string.on)
        "$channelName: $status"
    }

    pref?.summary = summaryProvider.invoke()

    pref?.setOnPreferenceClickListener {
        val intent = Intents.notificationSettings(requireContext(), channelId)
        getResult(intent){ _, _ ->
            pref.summary = summaryProvider.invoke()
        }
        true
    }
}