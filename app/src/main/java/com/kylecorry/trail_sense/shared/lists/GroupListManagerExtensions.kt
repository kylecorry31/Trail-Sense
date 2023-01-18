package com.kylecorry.trail_sense.shared.lists

import android.widget.TextView
import androidx.appcompat.widget.SearchView
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.ceres.list.CeresListView
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.trail_sense.shared.extensions.setOnQueryTextListener
import com.kylecorry.trail_sense.shared.grouping.Groupable

fun <T : Groupable> GroupListManager<T>.bind(view: SearchView) {
    view.setOnQueryTextListener { _, _ ->
        this.search(view.query)
        true
    }
}

fun <T : Groupable> GroupListManager<T>.bind(
    list: CeresListView,
    title: TextView,
    mapper: ListItemMapper<T>,
    titleProvider: (root: T?) -> String
) {
    onChange = { root, items, rootChanged ->
        tryOrLog {
            list.setItems(items, mapper)
            if (rootChanged) {
                list.scrollToPosition(0, false)
            }
            title.text = titleProvider(root)
        }
    }
}