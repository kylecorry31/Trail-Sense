package com.kylecorry.trail_sense.tools.offline_maps.quickactions

import android.os.Bundle
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.selection.ActiveMapSelector
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapService
import kotlinx.coroutines.launch

class QuickActionOpenPhotoMap(button: ImageButton, fragment: Fragment) : QuickActionButton(
    button, fragment
) {
    private val mapService = MapService.getInstance(fragment.requireContext())
    private val navigator = Navigator.getInstance(fragment.requireContext())
    private val selector = ActiveMapSelector()

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.photo_maps)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigate(R.id.mapListFragment)
        return true
    }

    override fun onClick() {
        super.onClick()
        fragment.inBackground {
            var wasSuccessful = false
            var id = 0L
            val job = launch {
                id = getActiveMap()?.id ?: 0
                wasSuccessful = true
            }


            Alerts.withCancelableLoading(
                fragment.requireContext(),
                context.getString(R.string.loading),
                onCancel = { job.cancel() }) {
                job.join()
                if (wasSuccessful) {
                    if (id != 0L) {
                        fragment.findNavController()
                            .navigate(
                                R.id.photoMapsFragment,
                                Bundle().apply {
                                    putLong("mapId", id)
                                    putBoolean("autoLockLocation", true)
                                }
                            )
                    } else {
                        fragment.findNavController().navigate(R.id.mapListFragment)
                    }
                }
            }
        }
    }

    private suspend fun getActiveMap(): PhotoMap? {
        val sensors = SensorSubsystem.getInstance(fragment.requireContext())
        val location = sensors.getLocation()

        val destination = navigator.getDestination()?.coordinate
        val maps = mapService.getAllPhotoMaps()
        return selector.getActiveMap(maps, location, destination)
    }

}
