package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class QuickActionButton(
    protected val button: FloatingActionButton,
    protected val fragment: Fragment
) {
    protected val context: Context by lazy { fragment.requireContext() }

    private val observer = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreateImpl() {
            onCreate()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResumeImpl() {
            onResume()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPauseImpl() {
            onPause()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroyImpl() {
            onDestroy()
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