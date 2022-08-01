package com.kylecorry.trail_sense.shared

import android.content.Context
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

abstract class QuickActionButton(
    protected val button: ImageButton,
    protected val fragment: Fragment
) {
    protected val context: Context by lazy { fragment.requireContext() }

    private val observer = LifecycleEventObserver { _, event ->
        when(event){
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> {} // Do nothing
        }
    }

    fun bind(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun unbind(lifecycleOwner: LifecycleOwner){
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    open fun onCreate() {
        button.isVisible = true
    }
    open fun onResume() {}
    open fun onPause() {}
    open fun onDestroy() {}
}