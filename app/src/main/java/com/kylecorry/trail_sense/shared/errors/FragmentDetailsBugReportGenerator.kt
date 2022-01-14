package com.kylecorry.trail_sense.shared.errors

import androidx.fragment.app.Fragment

class FragmentDetailsBugReportGenerator(private val fragment: Fragment) : IBugReportGenerator {
    override fun generate(): String {
        return "Fragment: ${fragment.javaClass.simpleName}"
    }
}