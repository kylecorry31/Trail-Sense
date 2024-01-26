package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SearchView

class SearchBarPreference(context: Context, attributeSet: AttributeSet?) :
    Preference(context, attributeSet) {

    private var searchListener: ((String) -> Unit)? = null

    init {
        layoutResource = R.layout.preference_search
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        val search = holder.findViewById(R.id.search) as SearchView
        search.setOnSearchListener {
            searchListener?.invoke(it)
        }
    }

    fun setOnSearchListener(listener: (String) -> Unit) {
        searchListener = listener
    }


}