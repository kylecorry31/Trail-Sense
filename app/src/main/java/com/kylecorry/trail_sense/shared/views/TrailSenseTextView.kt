package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TrailSenseTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    override fun performLongClick(): Boolean {
        // WHY: Android throws "Drag shadow dimensions must be positive" when it can't calculate a drag shadow
        return try {
            super.performLongClick()
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

}
