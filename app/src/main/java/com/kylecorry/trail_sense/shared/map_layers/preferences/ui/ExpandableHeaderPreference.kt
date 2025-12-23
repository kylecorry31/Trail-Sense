package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.trail_sense.R

class ExpandableHeaderPreference(context: Context) : Preference(context) {

    var isExpanded: Boolean = false
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
        val icon = holder.findViewById(android.R.id.widget_frame) as? android.view.ViewGroup
        val imageView = icon?.getChildAt(0) as? ImageView
        imageView?.setImageResource(
            if (isExpanded) R.drawable.ic_drop_down_expanded else R.drawable.ic_drop_down
        )
    }
}
