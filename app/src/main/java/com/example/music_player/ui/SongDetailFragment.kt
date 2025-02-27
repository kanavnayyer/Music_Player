package com.example.music_player.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.music_player.R
import com.example.music_player.databinding.FragmentSongDetailBinding
import com.example.music_player.model.Data
import com.example.music_player.util.MediaPlayerManager
import com.example.music_player.viewModel.PlaybackViewModel

class SongDetailFragment : Fragment() {
    private lateinit var binding: FragmentSongDetailBinding
    private var track: Data? = null
    private lateinit var playbackViewModel: PlaybackViewModel
    private val handler = Handler(Looper.getMainLooper())

    private val playbackStateListener = object : MediaPlayerManager.PlaybackStateListener {
        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            if (!isAdded) return
            requireActivity().runOnUiThread {
                updatePlayPauseIcon()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongDetailBinding.inflate(inflater, container, false)
        track = arguments?.getParcelable("track")

        track?.let {
            binding.songTitle.text = it.title
            binding.artistName.text = it.artist.name
            Glide.with(requireContext()).load(it.album.cover_medium).into(binding.albumCover)

            updatePlayPauseIcon()
        }

        binding.playPauseButton.setOnClickListener {
            togglePlayback()
        }

        MediaPlayerManager.setPlaybackStateListener(playbackStateListener)
        playbackViewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        playbackViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        setupSeekBar()
        return binding.root
    }

    private fun togglePlayback() {
        if (MediaPlayerManager.isPlaying) {
            MediaPlayerManager.pauseTrack()
        } else {
            track?.let {
                MediaPlayerManager.playTrack(requireContext(), Uri.parse(it.preview))
                startSeekBarUpdater()
            }
        }
        playbackViewModel.setPlayingState(MediaPlayerManager.isPlaying)
    }

    private fun setupSeekBar() {
        binding.seekBar.max = 100

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPosition = (progress / 100f) * (MediaPlayerManager.getDuration() ?: 0)
                    MediaPlayerManager.seekTo(newPosition.toInt())
                    updateSeekBar(newPosition.toInt())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startSeekBarUpdater()
    }

    private fun startSeekBarUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                val currentPosition = MediaPlayerManager.getCurrentPosition() ?: 0
                updateSeekBar(currentPosition)
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun updateSeekBar(position: Int) {
        val duration = MediaPlayerManager.getDuration() ?: 0
        if (duration > 0) {
            binding.seekBar.progress = (position * 100) / duration
        }
        binding.startTime.text = formatTime(position)
        binding.totalTime.text = formatTime(duration)
    }

    private fun formatTime(millis: Int): String {
        val minutes = millis / 60000
        val seconds = (millis % 60000) / 1000
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = MediaPlayerManager.isPlaying
        binding.playPauseButton.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        requireActivity().findViewById<View>(R.id.playPauseButton)?.let { playButton ->
            (playButton as? android.widget.ImageButton)?.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MediaPlayerManager.removePlaybackStateListener()
        handler.removeCallbacksAndMessages(null)
    }
}
