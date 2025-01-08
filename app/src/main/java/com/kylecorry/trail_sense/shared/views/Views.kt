package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding

object Views {

    fun scroll(
        child: View,
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        padding: Int = 0
    ): View {
        val scrollView = ScrollView(child.context)
        scrollView.layoutParams = ViewGroup.LayoutParams(width, height)
        scrollView.setPadding(padding)
        scrollView.addView(child)
        return scrollView
    }

    fun linear(
        views: List<View>,
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        orientation: Int = LinearLayout.VERTICAL,
        padding: Int = 0
    ): View {
        val layout = LinearLayout(views.first().context)
        layout.layoutParams = ViewGroup.LayoutParams(width, height)
        layout.orientation = orientation
        layout.setPadding(padding, padding, padding, padding)

        views.forEach { view ->
            layout.addView(view)
        }

        return layout
    }

    fun text(
        context: Context,
        text: CharSequence?,
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    ): View {
        return TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(width, height)
            this.text = text
        }
    }

    fun image(
        context: Context,
        uri: Uri,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    ): View {
        return ImageView(context).apply {
            setImageURI(uri)
            layoutParams = ViewGroup.LayoutParams(width, height)
        }
    }

    fun image(
        context: Context,
        drawable: Drawable?,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    ): View {
        return ImageView(context).apply {
            setImageDrawable(drawable)
            layoutParams = ViewGroup.LayoutParams(width, height)
        }
    }

}