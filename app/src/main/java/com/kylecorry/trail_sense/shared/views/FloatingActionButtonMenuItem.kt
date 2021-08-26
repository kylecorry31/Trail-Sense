package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R

class FloatingActionButtonMenuItem(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private var textView: TextView
    private var fab: FloatingActionButton

    init {
        inflate(context, R.layout.view_floating_action_button_menu_item, this)
        textView = findViewById(R.id.fab_text)
        fab = findViewById(R.id.fab)
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.FloatingActionButtonMenuItem, 0, 0)
        setImageResource(a.getResourceId(R.styleable.FloatingActionButtonMenuItem_android_src, R.drawable.ic_add))
        textView.text = a.getString(R.styleable.FloatingActionButtonMenuItem_android_text)
        // TODO: Allow background and icon color
        a.recycle()
        layoutParams = generateDefaultLayoutParams()
        updateLayoutParams {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

    }

    fun setImageResource(@DrawableRes resId: Int) {
        fab.setImageResource(resId)
    }

    fun setImageDrawable(image: Drawable) {
        fab.setImageDrawable(image)
    }

    fun setText(text: String?) {
        textView.text = text
    }

    fun setItemOnClickListener(l: OnClickListener){
        setOnClickListener(l)
        fab.setOnClickListener(l)
    }

}