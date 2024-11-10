package com.kylecorry.trail_sense.tools.tools.ui.summaries

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

abstract class ToolSummaryView(
    protected val root: FrameLayout,
    protected val fragment: Fragment
) {
    protected val context: Context by lazy { fragment.requireContext() }

    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> {
                // Do nothing
            }
        }
    }

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            // Do nothing
        }

        override fun onViewDetachedFromWindow(v: View) {
            onDestroy()
        }
    }

    fun bind(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun unbind(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    open fun onCreate() {
        root.removeAllViews()
        root.isVisible = true
        root.addOnAttachStateChangeListener(attachStateChangeListener)
        root.setOnClickListener {
            onClick()
        }
    }

    open fun onResume() {
        // Do nothing
    }

    open fun onPause() {
        // Do nothing
    }

    open fun onDestroy() {
        root.removeOnAttachStateChangeListener(attachStateChangeListener)
    }

    open fun onClick() {
        // Do nothing
    }
}