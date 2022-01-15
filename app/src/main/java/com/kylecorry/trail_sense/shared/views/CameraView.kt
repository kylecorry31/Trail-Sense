package com.kylecorry.trail_sense.shared.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Size
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.setOnProgressChangeListener

class CameraView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    var camera: Camera? = null

    private val preview: PreviewView
    private val torchBtn: ImageButton
    private val zoomSeek: SeekBar
    private var imageListener: ((Bitmap) -> Unit)? = null
    private var captureListener: ((Bitmap) -> Unit)? = null
    private var isTorchOn = false

    fun start(resolution: Size? = null, onImage: ((Bitmap) -> Unit)? = null) {
        val owner = ViewTreeLifecycleOwner.get(this) ?: return
        camera?.stop(this::onCameraUpdate)
        imageListener = onImage
        camera = Camera(
            context,
            owner,
            previewView = preview,
            analyze = true,
            targetResolution = resolution
        )
        camera?.start(this::onCameraUpdate)
    }

    fun stop() {
        camera?.stop(this::onCameraUpdate)
        camera = null
        setZoom(0f)
        setTorch(false)
    }

    fun capture(onImage: (Bitmap) -> Unit) {
        synchronized(this) {
            captureListener = onImage
        }
    }

    fun setTorch(isOn: Boolean) {
        isTorchOn = isOn
        torchBtn.setImageResource(if (isOn) R.drawable.ic_torch_on else R.drawable.ic_torch_off)
        camera?.setTorch(isOn)
    }

    fun setShowTorch(shouldShow: Boolean) {
        torchBtn.isVisible = shouldShow
    }

    fun setZoom(zoom: Float) {
        zoomSeek.progress = (zoom * 100).toInt()
        camera?.setZoom(zoom)
    }

    fun setShowZoom(shouldShow: Boolean) {
        zoomSeek.isVisible = shouldShow
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        if (captureListener == null && imageListener == null) {
            camera?.image?.close()
            return true
        }
        try {
            val bitmap = camera?.image?.image?.toBitmap()
            bitmap?.let {
                captureListener?.invoke(it)
                imageListener?.invoke(it)

                synchronized(this) {
                    captureListener = null
                }
            }
        } catch (e: Exception) {
            // Do nothing
        } finally {
            camera?.image?.close()
        }
        return true
    }

    init {
        inflate(context, R.layout.view_camera, this)
        preview = findViewById(R.id.camera_preview)
        torchBtn = findViewById(R.id.camera_torch)
        zoomSeek = findViewById(R.id.camera_zoom)

        torchBtn.setOnClickListener {
            setTorch(!isTorchOn)
        }

        zoomSeek.setOnProgressChangeListener { progress, _ ->
            setZoom(progress / 100f)
        }
    }


}