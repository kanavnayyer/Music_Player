package com.example.music_player.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.music_player.databinding.ItemArtistBinding
import com.example.music_player.model.ArtistName


class ArtistAdapter(
    private val imageItems: List<ArtistName>,
    private val onClick: (ArtistName) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ImageViewHolder>() {

    class ImageViewHolder(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = imageItems[position]
        holder.binding.imageItem = imageItem
        Glide.with(holder.binding.imageView.context)
            .load(imageItem.imageUrl)
            .into(holder.binding.imageView)
        holder.binding.root.setOnClickListener {
            onClick(imageItem)
        }
    }

    override fun getItemCount() = imageItems.size
}