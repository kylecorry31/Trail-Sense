package com.kylecorry.trail_sense.shared.views

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class PhotoPagerAdapter(private val photos: List<String>) :
    RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    override fun getItemCount(): Int {
        return photos.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = ImageView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    class PhotoViewHolder(private val view: ImageView) : RecyclerView.ViewHolder(view) {

        private val files by lazy { FileSubsystem.getInstance(view.context) }

        fun bind(photo: String) {
            view.setImageDrawable(files.drawable(photo))
        }
    }
}
