package com.kylecorry.trail_sense.test_utils.views

import org.junit.Assert.assertEquals

fun TestView.isChecked(isChecked: Boolean = true): TestView {
    assertEquals(uiObject.isChecked, isChecked)
    return this
}