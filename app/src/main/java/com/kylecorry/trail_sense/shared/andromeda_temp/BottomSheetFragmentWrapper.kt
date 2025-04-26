package com.kylecorry.trail_sense.shared.andromeda_temp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.replaceChildFragment

class BottomSheetFragmentWrapper(private val fragment: Fragment) :
    BottomSheetDialogFragment(R.layout.bottom_sheet_fragment_wrapper) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        replaceChildFragment(
            fragment,
            R.id.fragment_holder
        )
    }

}

