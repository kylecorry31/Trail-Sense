package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class QuickActionButton(protected val button: FloatingActionButton, protected val fragment: Fragment) {
    protected val context: Context by lazy { fragment.requireContext() }

    abstract fun onCreate()
    abstract fun onResume()
    abstract fun onPause()
    abstract fun onDestroy()
}