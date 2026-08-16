package com.kylecorry.trail_sense.tools.field_guide.quickactions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideUtils
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideDeepLinks
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideFormatService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class QuickActionRecordSighting(btn: QuickActionButtonView, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val service = getAppService<FieldGuideService>()
    private val formatter = getAppService<FieldGuideFormatService>()
    private val sensors = getAppService<SensorSubsystem>()

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
            val (location, elevation) = sensors.getLocationAndElevation(
                SensorSubsystem.SensorRefreshPolicy.Refresh,
                timeout = Duration.ofMinutes(1)
            )

            if (location == Coordinate.zero) {
                return@launch
            }

            // Record the sighting
            id = service.recordSighting(
                Sighting(
                    0,
                    page.id,
                    Instant.now(),
                    location,
                    elevation.meters().value,
                )
            ).id

            wasSuccessful = true
        }


        Alerts.withCancelableLoading(
            fragment.requireContext(),
            context.getString(R.string.recording_sighting_for_page, formatter.formatName(page)),
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
                    FieldGuideDeepLinks.navigateToSighting(fragment.findNavController(), page.id, id)
                }
            } else if (!wasCancelled) {
                FieldGuideDeepLinks.navigateToSighting(fragment.findNavController(), page.id)
            }
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.FIELD_GUIDE)
        return true
    }
}
