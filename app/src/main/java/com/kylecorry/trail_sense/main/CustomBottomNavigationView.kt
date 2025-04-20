package com.kylecorry.trail_sense.main

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.get
import androidx.core.view.size
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {

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