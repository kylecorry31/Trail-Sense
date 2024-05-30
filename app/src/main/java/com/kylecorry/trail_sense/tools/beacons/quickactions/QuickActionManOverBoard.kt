package com.kylecorry.trail_sense.tools.beacons.quickactions

import android.widget.ImageButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class QuickActionManOverBoard(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val navigator by lazy { Navigator.getInstance(fragment.requireContext()) }

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_lifebuoy)
    }

    override fun onClick() {
        super.onClick()
        fragment.inBackground {

            var wasSuccessful = false
            var id = 0L
            val job = launch {
                val sensors = SensorService(fragment.requireContext())
                val gps = sensors.getGPS()
                val altimeter = sensors.getAltimeter(gps = gps)
                readAll(listOf(gps, altimeter))

                // Create a beacon
                val formatter = FormatService.getInstance(fragment.requireContext())
                val time = formatter.formatDateTime(ZonedDateTime.now())
                val location = gps.location

                val beaconService = BeaconService(fragment.requireContext())
                id = beaconService.add(
                    Beacon(
                        0,
                        fragment.requireContext().getString(R.string.mob),
                        location,
                        elevation = altimeter.altitude,
                        color = AppColor.Red.color,
                        icon = BeaconIcon.Lifebuoy,
                        comment = "$time\n${formatter.formatLocation(location)}"
                    )
                )

                navigator.navigateTo(id)

                wasSuccessful = true
            }


            Alerts.withCancelableLoading(fragment.requireContext(),
                context.getString(R.string.creating_mob),
                onCancel = { job.cancel() }) {
                job.join()
                if (wasSuccessful) {
                    CustomUiUtils.snackbar(
                        fragment,
                        fragment.getString(R.string.mob_created),
                        duration = Snackbar.LENGTH_LONG,
                        action = fragment.getString(R.string.view)
                    ) {
                        fragment.findNavController().navigateWithAnimation(
                            R.id.beaconDetailsFragment,
                            bundleOf(
                                "beacon_id" to id
                            )
                        )
                    }
                }
            }

        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.beacon_list)
        return true
    }

}