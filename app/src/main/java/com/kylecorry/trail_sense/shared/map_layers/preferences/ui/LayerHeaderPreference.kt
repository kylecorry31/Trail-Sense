package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.trail_sense.R

class LayerHeaderPreference(context: Context) : Preference(context) {

    var isExpanded: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    var isLayerEnabled: Boolean = true
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        isIconSpaceReserved = false
        widgetLayoutResource = R.layout.preference_widget_dropdown
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val frame = holder.findViewById(android.R.id.widget_frame) as? android.view.ViewGroup
        val imageView = frame?.getChildAt(0) as? ImageView
        imageView?.setImageResource(
            if (isExpanded) R.drawable.ic_drop_down_expanded else R.drawable.ic_drop_down
        )

        val title = holder.findViewById(android.R.id.title) as? TextView
        title?.alpha = if (isLayerEnabled) {
            1f
        } else {
            0.4f
        }
    }
}
