package com.kylecorry.trail_sense.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListView<T>(private val view: RecyclerView, @LayoutRes private val itemLayoutId: Int, private val onViewBind: (View, T) -> Unit) {

    private val adapter: Adapter
    private val layoutInflater: LayoutInflater

    init {
        val layoutManager = LinearLayoutManager(view.context)
        view.layoutManager = layoutManager

        layoutInflater = LayoutInflater.from(view.context)

        adapter = Adapter(listOf())
        view.adapter = adapter
    }

    fun setData(data: List<T>){
        adapter.data = data
    }

    
    inner class Adapter(mData: List<T>) : RecyclerView.Adapter<Holder>() {

        var data: List<T> = mData
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(itemLayoutId, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(data[position])
        }

    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(detail: T) {
           onViewBind(itemView, detail)
        }
    }


}