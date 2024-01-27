package com.kylecorry.trail_sense.tools.tools.ui.sort

import android.content.Context

class ToolSortFactory(private val context: Context) {

    fun getToolSort(sort: ToolSortType): ToolSort {
        return when (sort) {
            ToolSortType.Name -> AlphabeticalToolSort()
            ToolSortType.Category -> CategoricalToolSort(context)
        }
    }

}