package com.kylecorry.trail_sense.test_utils

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector

fun byResId(id: Int): BySelector {
    return By.res(
        TestUtils.context.resources.getResourcePackageName(id),
        TestUtils.context.resources.getResourceEntryName(id)
    )
}

fun BySelector.resId(id: Int): BySelector {
    return this.res(
        TestUtils.context.resources.getResourcePackageName(id),
        TestUtils.context.resources.getResourceEntryName(id)
    )
}