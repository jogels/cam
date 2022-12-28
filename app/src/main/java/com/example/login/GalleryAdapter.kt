package com.example.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.login.databinding.ItemVideoBinding
import com.example.login.model.Video

class GalleryAdapter(val listVideo: List<Video>, val onItemClick:(video:Video)-> Unit , val onDeleteItemClick: (video : Video) -> Unit): RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
    inner class GalleryViewHolder(val itemBinding:ItemVideoBinding):RecyclerView.ViewHolder(itemBinding.root)
    {
        fun bindData(video: Video){
            itemBinding.tvTitle.text=video.title
            itemBinding.tvSize.text=video.size
            itemBinding.tvDuration.text=video.duration
            itemBinding.btnDelete.setOnClickListener{
                onDeleteItemClick.invoke(video)
            }
            itemBinding.root.setOnClickListener{
                onItemClick.invoke(video)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val itemBinding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bindData(listVideo[position])
    }

    override fun getItemCount(): Int {
        return listVideo.size
    }


}

