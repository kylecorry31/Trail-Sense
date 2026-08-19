package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.findFragment
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.andromeda_temp.getOrNull
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import java.util.UUID

class PhotoUploadView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val imageHolder: View
    private val emptyView: View
    private val image: ImageView
    private val menuButton: View
    private val takePhotoButton: View
    private val selectPhotoButton: View

    private val files by lazy { FileSubsystem.getInstance(context) }
    private val uriPicker by lazy { IntentUriPicker(findFragment<AndromedaFragment>(), context) }

    private var onPhotoAddedListener: ((path: String) -> Unit)? = null
    private var menuItems: List<ListMenuItem> = emptyList()

    var folder: String = "photos"
    var maxSize: Size = Size(500, 500)
    var quality: Int = 75

    init {
        inflate(context, R.layout.view_photo_upload, this)
        imageHolder = findViewById(R.id.photo_upload_image_holder)
        emptyView = findViewById(R.id.photo_upload_empty)
        image = findViewById(R.id.photo_upload_image)
        menuButton = findViewById(R.id.photo_upload_menu_button)
        takePhotoButton = findViewById(R.id.photo_upload_take_photo_button)
        selectPhotoButton = findViewById(R.id.photo_upload_select_photo_button)

        menuButton.setOnClickListener {
            showMenu()
        }

        takePhotoButton.setOnClickListener {
            val fragment = findFragment<AndromedaFragment>()
            fragment.inBackground {
                importPhoto(CustomUiUtils.takePhoto(fragment))
            }
        }

        selectPhotoButton.setOnClickListener {
            val fragment = findFragment<AndromedaFragment>()
            fragment.inBackground(BackgroundMinimumState.Created) {
                importPhoto(uriPicker.open(listOf("image/*")).getOrNull())
            }
        }
    }

    fun setPhoto(path: String?) {
        imageHolder.isVisible = path != null
        emptyView.isVisible = path == null
        image.setImageDrawable(path?.let { files.drawable(it) })
    }

    fun setMenu(items: List<ListMenuItem>) {
        menuItems = items
        menuButton.isVisible = items.isNotEmpty()
    }

    fun setOnPhotoAddedListener(listener: ((path: String) -> Unit)?) {
        onPhotoAddedListener = listener
    }

    private fun showMenu() {
        Pickers.menu(menuButton, menuItems.map { it.text }) { selected ->
            menuItems[selected].action()
            true
        }
    }

    private suspend fun importPhoto(uri: Uri?) {
        uri ?: return
        val path = onIO { copyPhoto(uri) } ?: return
        onMain {
            onPhotoAddedListener?.invoke(path)
        }
    }

    private suspend fun copyPhoto(uri: Uri): String? {
        val file = files.copyToLocal(uri, folder, "${UUID.randomUUID()}.webp") ?: return null
        val path = files.getLocalPath(file)

        var rotation = 0
        tryOrLog {
            rotation = ExifInterface(file).rotationDegrees
        }

        val bmp = files.bitmap(path, maxSize) ?: return null
        val rotated = if (rotation != 0) {
            bmp.rotate(rotation.toFloat())
        } else {
            bmp
        }

        if (rotated != bmp) {
            bmp.recycle()
        }

        files.save(path, rotated, quality, true)

        return path
    }
}
