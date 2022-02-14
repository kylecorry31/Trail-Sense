package com.kylecorry.trail_sense.shared.extensions

import androidx.appcompat.widget.SearchView

fun SearchView.setOnQueryTextListener(listener: (query: String?, submitted: Boolean) -> Boolean) {
    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            listener(query, true)
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            listener(query?.toString(), false)
            return true
        }
    })
}