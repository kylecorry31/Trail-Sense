package com.kylecorry.trail_sense.shared.camera

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentPhotoImportSheetBinding


class PhotoImportBottomSheetFragment(
    private val resolution: Size? = null,
    private val onCapture: (bitmap: Bitmap?) -> Unit
) : BoundBottomSheetDialogFragment<FragmentPhotoImportSheetBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.camera.clipToOutline = true
        binding.camera.start(resolution)

        binding.toolTitle.rightQuickAction.setOnClickListener {
            onCapture(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            binding.camera.capture {
                onCapture(it)
                dismiss()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val sheetDialog = it as BottomSheetDialog
            val root = sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!
            val behavior = BottomSheetBehavior.from(root)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            root.layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, getWindowHeight())
        }
        return dialog
    }

    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (context as Activity?)!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    override fun onDestroyView() {
        if (isBound) {
            binding.camera.stop()
        }
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding.camera.stop()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPhotoImportSheetBinding {
        return FragmentPhotoImportSheetBinding.inflate(layoutInflater, container, false)
    }
}