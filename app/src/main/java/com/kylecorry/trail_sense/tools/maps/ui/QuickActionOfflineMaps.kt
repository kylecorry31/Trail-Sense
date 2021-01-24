package com.kylecorry.trail_sense.tools.maps.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.maps.infrastructure.TrailSenseMaps
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickActionOfflineMaps(
    button: FloatingActionButton,
    fragment: Fragment
) : QuickActionButton(button, fragment) {

    private val cache by lazy { Cache(context) }
    private val beaconRepo by lazy { BeaconRepo.getInstance(context) }

    override fun onCreate() {
        button.setImageResource(R.drawable.maps)
        CustomUiUtils.setButtonState(button, false)
        button.setOnClickListener {
            if (cache.contains(NavigatorFragment.LAST_BEACON_ID)) {
                fragment.lifecycleScope.launch {
                    val beacon = withContext(Dispatchers.IO) {
                        beaconRepo.getBeacon(cache.getLong(NavigatorFragment.LAST_BEACON_ID) ?: 0L)
                    }?.toBeacon()
                    withContext(Dispatchers.Main) {
                        beacon?.let {
                            TrailSenseMaps.navigateTo(context, it.coordinate)
                        }
                    }
                }
            } else {
                TrailSenseMaps.open(context)
            }
        }

    }

    override fun onResume() {
        button.visibility = if (TrailSenseMaps.isInstalled(context)) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }


}