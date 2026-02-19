package com.kylecorry.trail_sense.test_utils

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector

fun byResId(id: Int, packageName: String? = null): BySelector {
    return By.res(
        packageName ?: TestUtils.context.resources.getResourcePackageName(id),
        TestUtils.context.resources.getResourceEntryName(id)
    )
}