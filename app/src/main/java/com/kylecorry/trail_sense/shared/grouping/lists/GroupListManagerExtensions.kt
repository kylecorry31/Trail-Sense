package com.kylecorry.trail_sense.shared.grouping.lists

import android.widget.TextView
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.shared.grouping.Groupable

fun <T : Groupable> GroupListManager<T>.bind(view: com.kylecorry.trail_sense.shared.views.SearchView) {
    view.setOnSearchListener {
        this.search(it)
    }
}

fun <T : Groupable> GroupListManager<T>.bind(
    list: AndromedaListView,
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