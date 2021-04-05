package com.kylecorry.trail_sense.shared

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

object CustomUiUtils {

    fun setButtonState(button: ImageButton, state: Boolean) {
        UiUtils.setButtonState(
            button,
            state,
            UiUtils.color(button.context, R.color.colorPrimary),
            UiUtils.color(button.context, R.color.colorSecondary)
        )
    }

    fun setButtonState(
        button: Button,
        isOn: Boolean
    ) {
        if (isOn) {
            button.setTextColor(UiUtils.color(button.context, R.color.colorSecondary))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.color(button.context, R.color.colorPrimary))
        } else {
            button.setTextColor(UiUtils.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(button.context))
        }
    }

    @ColorInt
    fun getQualityColor(context: Context, quality: Quality): Int {
        return when (quality) {
            Quality.Poor, Quality.Unknown -> UiUtils.color(context, R.color.red)
            Quality.Moderate -> UiUtils.color(context, R.color.yellow)
            Quality.Good -> UiUtils.color(context, R.color.green)
        }
    }

    fun promptIfUnsavedChanges(activity: FragmentActivity, owner: LifecycleOwner, hasChanges: () -> Boolean){
        activity.onBackPressedDispatcher.addCallback(owner) {
            if (hasChanges()) {
                UiUtils.alertWithCancel(
                    activity,
                    activity.getString(R.string.unsaved_changes),
                    activity.getString(R.string.unsaved_changes_message),
                    activity.getString(R.string.dialog_leave),
                    activity.getString(R.string.dialog_cancel)
                ) { cancelled ->
                    if (!cancelled) {
                        remove()
                        activity.onBackPressed()
                    }
                }
            } else {
                remove()
                activity.onBackPressed()
            }
        }
    }

    fun pickDistance(context: Context, units: List<DistanceUnits>, default: Distance? = null, title: String, onDistancePick: (distance: Distance?) -> Unit) {
        val view = View.inflate(context, R.layout.view_distance_entry_prompt, null)
        var distance: Distance? = default
        val distanceInput = view.findViewById<DistanceInputView>(R.id.prompt_distance)
        distanceInput?.setOnDistanceChangeListener {
            distance = it
        }
        distanceInput?.units = units
        distanceInput?.updateDistance(default)

        UiUtils.alertViewWithCancel(context, title, view, context.getString(R.string.dialog_ok), context.getString(R.string.dialog_cancel)){ cancelled ->
            if (cancelled){
                onDistancePick.invoke(null)
            } else {
                onDistancePick.invoke(distance)
            }
        }
    }

    fun openMenu(anchorView: View, @MenuRes menu: Int, onSelection: (itemId: Int) -> Boolean){
        val popup = PopupMenu(anchorView.context, anchorView)
        val inflater = popup.menuInflater
        inflater.inflate(menu, popup.menu)
        popup.setOnMenuItemClickListener {
            onSelection.invoke(it.itemId)
        }
        popup.show()
    }

    fun dp(context: Context, size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, size,
            context.resources.displayMetrics
        )
    }

    fun sp(context: Context, size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, size,
            context.resources.displayMetrics
        )
    }

    fun decodeBitmapScaled(
        path: String,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            inSampleSize = calculateInSampleSize(this, maxWidth, maxHeight)
            inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

}