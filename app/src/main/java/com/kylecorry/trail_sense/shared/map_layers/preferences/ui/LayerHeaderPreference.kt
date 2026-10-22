package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
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

    var canMoveUp: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    var canMoveDown: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    var showDivider: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    var onMoveUp: (() -> Unit)? = null
    var onMoveDown: (() -> Unit)? = null

    init {
        layoutResource = R.layout.preference_layer_header
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        // Lets the drag and drop callback tell layer headers apart from other rows
        holder.itemView.tag = this
        holder.findViewById(R.id.layer_divider)?.isVisible = showDivider
        holder.itemView.setOnClickListener {
            onPreferenceClickListener?.onPreferenceClick(this)
        }

        val upButton = holder.findViewById(R.id.layer_move_up) as? ImageButton
        val downButton = holder.findViewById(R.id.layer_move_down) as? ImageButton
        val dropdown = holder.findViewById(R.id.layer_dropdown) as? ImageView

        bindArrowButton(
            upButton,
            canMoveUp,
        ) { onMoveUp?.invoke() }
        bindArrowButton(
            downButton,
            canMoveDown,
        ) { onMoveDown?.invoke() }

        dropdown?.setImageResource(
            if (isExpanded) R.drawable.ic_drop_down_expanded else R.drawable.ic_drop_down
        )

        val title = holder.findViewById(android.R.id.title) as? TextView
        title?.alpha = if (isLayerEnabled) {
            1f
        } else {
            0.4f
        }
    }

    private fun bindArrowButton(
        button: ImageButton?,
        enabled: Boolean,
        action: () -> Unit
    ) {
        button?.isEnabled = enabled
        button?.alpha = if (enabled) 1f else 0.4f
        button?.setOnClickListener {
            action()
        }
    }
}
