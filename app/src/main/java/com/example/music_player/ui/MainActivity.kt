package com.example.music_player.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.music_player.R
import com.example.music_player.Services.MusicService
import com.example.music_player.databinding.ActivityMainBinding
import com.example.music_player.model.ArtistName
import com.example.music_player.model.Data
import com.example.music_player.ui.adapters.ArtistAdapter
import com.example.music_player.ui.adapters.TrackAdapter
import com.example.music_player.util.MediaPlayerManager
import com.example.music_player.viewModel.ArtistViewModel
import com.example.music_player.viewModel.MusicViewModel
import com.example.music_player.viewModel.PlaybackViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var playbackViewModel: PlaybackViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: TrackAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentTrack: Data? = null
    private lateinit var artistviewModel: ArtistViewModel
    private lateinit var selectedImageItem: ArtistName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        artistviewModel = ViewModelProvider(this)[ArtistViewModel::class.java]

        requestNotificationPermission()

        MediaPlayerManager.setCompletionListener(object : MediaPlayerManager.CompletionListener {
            override fun onTrackCompleted() {
                runOnUiThread {
                    binding.playPauseButton.setImageResource(R.drawable.ic_play)
                }
            }
        })

        adapter = TrackAdapter(emptyList()) { track -> onTrackClicked(track) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val recyclerViewArtist = binding.recyclerViewArtist
        recyclerViewArtist.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        artistviewModel.imageItems.observe(this) { imageItems ->
            recyclerViewArtist.adapter = ArtistAdapter(imageItems) { imageItem ->
                selectedImageItem = imageItem
                Toast.makeText(this, "Selected: ${imageItem.text}", Toast.LENGTH_SHORT).show()
                viewModel.fetchTracks(imageItem.text)
            }
        }

        binding.recyclerView.adapter = adapter
        playbackViewModel = ViewModelProvider(this)[PlaybackViewModel::class.java]

        playbackViewModel.isPlaying.observe(this) { isPlaying ->
            binding.playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        viewModel.tracks.observe(this) { myData ->
            myData?.let { adapter.updateData(it.data) }
        }

        viewModel.fetchTracks("Eminem")

        binding.playPauseButton.setOnClickListener {
            togglePlayback()
        }

        binding.bottomMusicBar.setOnClickListener {
            openSongDetailFragment()
        }

        MediaPlayerManager.setPlaybackStateListener(object :
            MediaPlayerManager.PlaybackStateListener {
            override fun onPlaybackStateChanged(isPlaying: Boolean) {
                runOnUiThread {
                    binding.playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                }
            }
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onTrackClicked(track: Data) {
        currentTrack = track
        binding.bottomMusicBar.visibility = View.VISIBLE
        binding.songTitle.text = track.title
        Glide.with(this).load(track.album.cover_medium).into(binding.songImage)

        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY
            putExtra("TRACK_DATA", track)
        }
        startService(intent)

        binding.playPauseButton.setImageResource(R.drawable.ic_pause)
    }

    private fun stopCurrentTrack() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }

//    private fun togglePlayback() {
//        if (MediaPlayerManager.isPlaying) {
//            MediaPlayerManager.pauseTrack()
//        } else {
//            currentTrack?.let {
//                MediaPlayerManager.playTrack(this, Uri.parse(it.preview))
//            }
//        }
//        playbackViewModel.setPlayingState(MediaPlayerManager.isPlaying)
//    }
    private fun togglePlayback() {
        if (MediaPlayerManager.isPlaying) {
            MediaPlayerManager.pauseTrack()
        } else {
            currentTrack?.let {
                MediaPlayerManager.playTrack(this, Uri.parse(it.preview))
            }
        }
        playbackViewModel.setPlayingState(MediaPlayerManager.isPlaying)
        updateNotification()
    }


        private fun updateNotification() {
            val intent = Intent(this, MusicService::class.java).apply {
                action = if (MediaPlayerManager.isPlaying) MusicService.ACTION_PLAY else MusicService.ACTION_PAUSE
            }
            startService(intent)
        }




    private fun updatePlayPauseIcon() {
        binding.playPauseButton.setImageResource(
            if (MediaPlayerManager.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun openSongDetailFragment() {
        val existingFragment = supportFragmentManager.findFragmentByTag("SongDetailFragment")
        if (existingFragment == null) {
            currentTrack?.let { track ->
                val fragment = SongDetailFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("track", track)
                    }
                }

                binding.blockingView.visibility = View.VISIBLE
                binding.fragmentContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment, "SongDetailFragment")
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCurrentTrack()
        MediaPlayerManager.removeCompletionListener()
        MediaPlayerManager.removePlaybackStateListener()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentByTag("SongDetailFragment")
        if (fragment != null) {
            binding.blockingView.visibility = View.GONE
            binding.fragmentContainer.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}
