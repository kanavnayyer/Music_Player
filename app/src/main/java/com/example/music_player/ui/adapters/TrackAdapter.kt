package com.example.music_player.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.music_player.databinding.ItemSongBinding
import com.example.music_player.model.Data

class TrackAdapter(
    private var tracks: List<Data>,
    private val onTrackClick: (Data) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    private var currentTrack: Data? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding, onTrackClick)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    fun updateData(newTracks: List<Data>) {
        tracks = newTracks
        notifyDataSetChanged()
    }

    fun getCurrentTrack(): Data? {
        return currentTrack
    }

    inner class TrackViewHolder(
        private val binding: ItemSongBinding,
        private val onTrackClick: (Data) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Data) {
            binding.track = track
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                currentTrack = track
                onTrackClick(track)
            }
        }
    }
}
