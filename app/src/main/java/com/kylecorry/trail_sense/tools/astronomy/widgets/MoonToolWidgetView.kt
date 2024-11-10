package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoonToolWidgetView : SimpleToolWidgetView() {

    private var lastBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null

    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            populateMoonDetails(context, views)
            onMain {
                commit()
            }
            lastBitmap?.recycle()
            lastBitmap = nextBitmap
        }
    }

    private fun populateMoonDetails(context: Context, views: RemoteViews) {
        val astronomy = AstronomySubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val moon = astronomy.moon
        val image = MoonPhaseImageMapper().getPhaseImage(moon.phase)
        val bitmap = Resources.drawable(context, image)?.toBitmap(
            Resources.dp(context, 32f).toInt(),
            Resources.dp(context, 32f).toInt(),
        )
        val rotated = bitmap?.let { rotate(it, moon.tilt) }
        bitmap?.recycle()
        nextBitmap = rotated
        views.setImageViewBitmap(ICON_IMAGEVIEW, rotated)
        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.moon))
        views.setTextViewText(SUBTITLE_TEXTVIEW, formatter.formatMoonPhase(moon.phase))
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.ASTRONOMY)
        )
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val newBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            bitmap.config ?: Bitmap.Config.ARGB_8888,
        )

        val canvas = Canvas(newBitmap)

        canvas.rotate(degrees, bitmap.width / 2f, bitmap.height / 2f)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        return newBitmap
    }
}