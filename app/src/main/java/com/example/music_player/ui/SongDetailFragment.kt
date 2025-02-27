package com.example.music_player.ui

import android.content.Intent
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
import com.example.music_player.Services.MusicService
import com.example.music_player.databinding.FragmentSongDetailBinding
import com.example.music_player.model.Data
import com.example.music_player.util.MediaPlayerManager
import com.example.music_player.viewModel.PlaybackViewModel

class SongDetailFragment : Fragment() {
    private lateinit var binding: FragmentSongDetailBinding
    private var track: Data? = null
    private lateinit var playbackViewModel: PlaybackViewModel
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongDetailBinding.inflate(inflater, container, false)
        track = arguments?.getParcelable("track")

        playbackViewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        track?.let {
            binding.songTitle.text = it.title
            binding.artistName.text = it.artist.name
            Glide.with(requireContext()).load(it.album.cover_medium).into(binding.albumCover)
            updatePlayPauseIcon()
        }

        binding.playPauseButton.setOnClickListener {
            togglePlayback()
        }

        playbackViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
//        binding.nextButton.setOnClickListener {
//            playbackViewModel.nextTrack(requireContext())
//        }

        setupSeekBar()
        return binding.root
    }

    private fun togglePlayback() {
        track?.let { track ->
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                action =
                    if (MediaPlayerManager.isPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY
                putExtra("TRACK_DATA", track)
            }
            requireContext().startService(intent)
            if (!MediaPlayerManager.isPlaying) {
                startSeekBarUpdater()
            }
        }
    }


    private fun setupSeekBar() {
        binding.seekBar.max = 100

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = MediaPlayerManager.getDuration() ?: 0
                    val newPosition = (progress.toFloat() / 100) * duration
                    MediaPlayerManager.seekTo(newPosition.toInt())
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
                val duration = MediaPlayerManager.getDuration() ?: 0
                if (duration > 0) {
                    binding.seekBar.progress = (currentPosition * 100 / duration).coerceIn(0, 100)
                }
                binding.startTime.text = formatTime(currentPosition)
                binding.totalTime.text = formatTime(duration)
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun formatTime(millis: Int): String {
        val minutes = millis / 60000
        val seconds = (millis % 60000) / 1000
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun updatePlayPauseIcon() {
        binding.playPauseButton.setImageResource(
            if (MediaPlayerManager.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}