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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
    private var zoomListener: ((Float) -> Unit)? = null
    private var imageListener: ((Bitmap) -> Unit)? = null
    private var captureListener: ((Bitmap) -> Unit)? = null
    private var isTorchOn = false
    private var zoom: Float = 0f

    fun start(resolution: Size? = null, lifecycleOwner: LifecycleOwner? = null, onImage: ((Bitmap) -> Unit)? = null) {
        val owner = lifecycleOwner ?: ViewTreeLifecycleOwner.get(this) ?: return
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED){
            return
        }
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
    }

    fun capture(onImage: (Bitmap) -> Unit) {
        synchronized(this) {
            captureListener = onImage
        }
    }

    fun setTorch(isOn: Boolean) {
        isTorchOn = isOn
        torchBtn.setImageResource(if (isOn) R.drawable.ic_torch_on else R.drawable.ic_torch_off)
        camera?.setTorch(isTorchOn)
    }

    fun setShowTorch(shouldShow: Boolean) {
        torchBtn.isVisible = shouldShow
    }

    fun setZoom(zoom: Float) {
        zoomSeek.progress = (zoom * 100).toInt()
        this.zoom = zoom
        camera?.setZoom(zoom)
    }

    fun setOnZoomChangeListener(listener: ((zoom: Float) -> Unit)?){
        zoomListener = listener
    }

    fun setShowZoom(shouldShow: Boolean) {
        zoomSeek.isVisible = shouldShow
    }

    fun setScaleType(type: PreviewView.ScaleType){
        preview.scaleType = type
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        camera?.setZoom(zoom)
        camera?.setTorch(isTorchOn)
        if (captureListener == null && imageListener == null) {
            camera?.image?.close()
            return true
        }
        try {
            val rotation = camera?.image?.imageInfo?.rotationDegrees?.toFloat() ?: 0f
            val bitmap = camera?.image?.image?.toBitmap(rotation)
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
            zoomListener?.invoke(progress / 100f)
            setZoom(progress / 100f)
        }
    }


}