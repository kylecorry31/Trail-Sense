package com.kylecorry.trail_sense.shared

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class LockableScrollView(
    context: Context?,
    attrs: AttributeSet?
) : ScrollView(context, attrs) {

    var isScrollable = true

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> isScrollable && super.onTouchEvent(ev)
            else -> super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return isScrollable && super.onInterceptTouchEvent(ev)
    }

}