package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigateWithAnimation

object FieldGuideDeepLinks {

    fun navigateToSighting(navController: NavController, pageId: Long, sightingId: Long? = null) {
        navController.navigateWithAnimation(
            R.id.fieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", pageId)
            }
        )

        navController.navigateWithAnimation(
            R.id.sightingListFragment,
            Bundle().apply {
                putLong("page_id", pageId)
            }
        )

        navController.navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
                sightingId?.let { putLong("sighting_id", it) }
            }
        )
    }

}
