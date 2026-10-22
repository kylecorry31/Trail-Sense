package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView

/**
 * A preference adapter whose rows can be moved and hidden while displayed.
 *
 * The changes only exist in this adapter, so the preferences must be rebuilt afterwards. Any change
 * to the preferences (such as rebuilding them) discards them.
 */
class ReorderablePreferenceAdapter(screen: PreferenceScreen) : PreferenceGroupAdapter(screen) {

    // The displayed preferences, null when the adapter hasn't been changed
    private var items: MutableList<Preference>? = null

    init {
        // Preference changes are synced with a full data set change, which discards the changes
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                items = null
            }
        })
    }

    /**
     * Hides the settings of every layer, leaving only the layer headers (and the rows after them).
     */
    fun hideLayerSettings() {
        val current = getDisplayedItems()
        for (position in current.indices.reversed()) {
            if (isLayerSetting(current[position])) {
                current.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    fun move(from: Int, to: Int) {
        val current = getDisplayedItems()
        current.add(to, current.removeAt(from))
        notifyItemMoved(from, to)
    }

    override fun getItemCount(): Int {
        return items?.size ?: super.getItemCount()
    }

    override fun getItem(position: Int): Preference? {
        return items?.getOrNull(position) ?: super.getItem(position)
    }

    private fun getDisplayedItems(): MutableList<Preference> {
        return items ?: MutableList(itemCount) { requireNotNull(super.getItem(it)) }
            .also { items = it }
    }

    private fun isLayerSetting(preference: Preference): Boolean {
        return preference is PreferenceCategory || preference.parent is PreferenceCategory
    }
}
