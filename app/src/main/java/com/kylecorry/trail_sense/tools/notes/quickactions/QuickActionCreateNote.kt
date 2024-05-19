package com.kylecorry.trail_sense.tools.notes.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionCreateNote(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_notes)
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().navigateWithAnimation(R.id.fragmentToolNotesCreate)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.fragmentToolNotes)
        return true
    }

}