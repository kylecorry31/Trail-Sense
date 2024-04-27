package com.kylecorry.trail_sense.tools.maps.quickactions

import android.widget.ImageButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService
import kotlinx.coroutines.launch
import java.time.Duration

class QuickActionOpenPhotoMap(button: ImageButton, fragment: Fragment) : QuickActionButton(
    button, fragment
) {
    private val mapService = MapService.getInstance(fragment.requireContext())

    override fun onCreate() {
        super.onCreate()

        button.setImageResource(R.drawable.maps)
        CustomUiUtils.setButtonState(button, false)

        button.setOnClickListener {
            fragment.inBackground {
                var wasSuccessful = false
                var id = 0L
                val job = launch {
                    val sensors = SensorService(fragment.requireContext())
                    val gps = sensors.getGPS()
                    readAll(listOf(gps), onlyIfInvalid = true)
                    val maps = mapService.getAllMaps()
                    val activeMaps = maps.filter {
                        it.boundary()?.contains(gps.location) == true
                    }

                    val mostZoomedIn =
                        activeMaps.minByOrNull {
                            it.distancePerPixel()?.meters()?.distance ?: Float.MAX_VALUE
                        }
                    id = mostZoomedIn?.id ?: 0
                    wasSuccessful = true
                }


                Alerts.withCancelableLoading(fragment.requireContext(),
                    context.getString(R.string.loading),
                    onCancel = { job.cancel() }) {
                    job.join()
                    if (wasSuccessful) {
                        if (id != 0L) {
                            fragment.findNavController()
                                .navigate(R.id.mapsFragment, bundleOf("mapId" to id))
                        } else {
                            fragment.findNavController().navigate(R.id.mapListFragment)
                        }
                    }
                }
            }
        }

        button.setOnLongClickListener {
            fragment.findNavController().navigate(R.id.mapListFragment)
            true
        }

    }
}