package com.kylecorry.trail_sense.tools.field_guide.quickactions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

class QuickActionRecordSighting(btn: QuickActionButtonView, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val service = getAppService<FieldGuideService>()
    private val sensors = getAppService<SensorService>()

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.field_guide)
    }

    override fun onClick() {
        super.onClick()
        FieldGuideUtils.showPageList(fragment) {
            fragment.inBackground {
                recordSighting(this, it)
            }

        }
    }

    private suspend fun recordSighting(scope: CoroutineScope, page: FieldGuidePage) {
        var wasSuccessful = false
        var wasCancelled = false
        var id = 0L
        val job = scope.launch {
            val gps = sensors.getGPS()
            val altimeter = sensors.getAltimeter(gps = gps)
            readAll(listOf(gps, altimeter))

            if (!gps.hasValidReading) {
                return@launch
            }

            // Record the sighting
            id = service.recordSighting(
                Sighting(
                    0,
                    page.id,
                    Instant.now(),
                    gps.location,
                    altimeter.altitude,
                )
            ).id

            wasSuccessful = true
        }


        Alerts.withCancelableLoading(
            fragment.requireContext(),
            context.getString(R.string.recording_sighting_for_page, page.name),
            onCancel = {
                wasCancelled = true
                job.cancel()
            }) {
            job.join()

            if (wasSuccessful) {
                CustomUiUtils.snackbar(
                    fragment,
                    fragment.getString(R.string.created),
                    duration = Snackbar.LENGTH_LONG,
                    action = fragment.getString(R.string.view)
                ) {
                    fragment.findNavController().navigateWithAnimation(
                        R.id.createFieldGuideSightingFragment,
                        Bundle().apply {
                            putLong("page_id", page.id)
                            putLong("sighting_id", id)
                        }
                    )
                }
            } else if (!wasCancelled) {
                fragment.findNavController().navigateWithAnimation(
                    R.id.createFieldGuideSightingFragment,
                    Bundle().apply {
                        putLong("page_id", page.id)
                    }
                )
            }
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.FIELD_GUIDE)
        return true
    }
}
