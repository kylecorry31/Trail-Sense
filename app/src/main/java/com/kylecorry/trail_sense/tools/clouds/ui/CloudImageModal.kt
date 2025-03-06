package com.kylecorry.trail_sense.tools.clouds.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService

class CloudImageModal(private val context: Context) {

    private val details = CloudDetailsService(context)

    fun show(cloud: CloudGenus?) {
        if (cloud != null) {
            Alerts.image(
                context,
                details.getCloudName(cloud),
                details.getCloudImage(context, cloud)
            )
        }
    }

    private fun Alerts.image(
        context: Context,
        title: CharSequence,
        drawable: Drawable?
    ): AlertDialog {
        val view = LinearLayout(context)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        view.layoutParams = params
        val imageView = ImageView(context)
        val imageParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        imageParams.gravity = Gravity.CENTER
        imageView.layoutParams = imageParams
        imageView.adjustViewBounds = true
        imageView.setImageDrawable(drawable)
        view.addView(imageView)

        return dialog(context, title, contentView = view, cancelText = null)
    }

}