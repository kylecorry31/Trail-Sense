package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class QuickActionButton(
    protected val button: FloatingActionButton,
    protected val fragment: Fragment
) {
    protected val context: Context by lazy { fragment.requireContext() }

    private val observer = LifecycleEventObserver { _, event ->
        when(event){
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
        }
    }

    fun bind(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun unbind(lifecycleOwner: LifecycleOwner){
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    open fun onCreate() {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onDestroy() {}
}