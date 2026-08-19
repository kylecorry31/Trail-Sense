package com.kylecorry.trail_sense.shared.views

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.andromeda.views.list.ListMenuItem

class PhotoUploadPagerAdapter(
    private val folder: String,
    private val onPhotoAdded: (path: String) -> Unit,
    private val menuItems: (position: Int) -> List<ListMenuItem>
) : RecyclerView.Adapter<PhotoUploadPagerAdapter.PhotoViewHolder>() {

    private var photos: List<String> = emptyList()

    fun setPhotos(photos: List<String>) {
        this.photos = photos
        notifyDataSetChanged()
    }

    // The last page is used to add another photo
    override fun getItemCount(): Int {
        return photos.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = PhotoUploadView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        view.folder = folder
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PhotoViewHolder(private val view: PhotoUploadView) :
        RecyclerView.ViewHolder(view) {

        fun bind(position: Int) {
            val photo = photos.getOrNull(position)
            view.setPhoto(photo)
            view.setMenu(if (photo == null) emptyList() else menuItems(position))
            view.setOnPhotoAddedListener(onPhotoAdded)
        }
    }
}
