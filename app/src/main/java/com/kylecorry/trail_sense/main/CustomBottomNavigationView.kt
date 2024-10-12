package com.kylecorry.trail_sense.main

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {

    override fun getMaxItemCount(): Int {
        return MAX_ITEM_COUNT
    }

    companion object {
        const val MAX_ITEM_COUNT = 8
    }

}