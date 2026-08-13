package com.kylecorry.trail_sense.main

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {

    /**
     * The height available to the navigation items, excluding padding. Window insets are added on
     * top of it. Set to null to restore original behavior.
     */
    var contentHeight: Int? = null
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    init {
        val initialPaddingBottom = paddingBottom
        // This replaces the listener added by Material, which also insets by the IME
        // We want the bottom navigation to be below the keyboard so it doesn't take up unnecessary space
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = initialPaddingBottom + systemBars.bottom)
            windowInsets
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = contentHeight
        if (height == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        // The inset padding is applied by the time this runs, so it doesn't need to be recalculated
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                height + paddingTop + paddingBottom,
                MeasureSpec.EXACTLY
            )
        )
    }

    override fun getMaxItemCount(): Int {
        return MAX_ITEM_COUNT
    }

    fun disable() {
        for (i in 0 until menu.size) {
            menu[i].isEnabled = false
        }
    }

    fun enable() {
        for (i in 0 until menu.size) {
            menu[i].isEnabled = true
        }
    }

    companion object {
        const val MAX_ITEM_COUNT = 8
    }

}
