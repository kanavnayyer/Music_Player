package com.example.music_player.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

import android.net.Uri
import com.example.music_player.model.Data

object MediaPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackUri: Uri? = null

    private var _isPlaying: Boolean = false
    var isPlaying: Boolean = false
        get() = _isPlaying

    private var lastPosition: Int = 0
    private var isLooping: Boolean = false
    private val playbackStateListeners = mutableListOf<PlaybackStateListener>()
    private var completionListener: CompletionListener? = null

    fun addPlaybackStateListener(listener: PlaybackStateListener) {
        playbackStateListeners.add(listener)
    }

    fun removePlaybackStateListener(listener: PlaybackStateListener) {
        playbackStateListeners.remove(listener)
    }

    fun setCompletionListener(listener: CompletionListener) {
        completionListener = listener
    }

    fun removeCompletionListener() {
        completionListener = null
    }

    fun playTrack(context: Context, trackUri: Uri) {
        if (currentTrackUri == trackUri && mediaPlayer != null) {
            mediaPlayer?.seekTo(lastPosition) // Resume from last position
            mediaPlayer?.start()
            _isPlaying = true // Update custom state
            notifyPlaybackStateChanged(true) // Notify listeners
            return
        }

        stopTrack() // Stop previous track if different

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(context, trackUri)
            prepareAsync()
            setOnPreparedListener {
                start()
                _isPlaying = true // Update custom state
                notifyPlaybackStateChanged(true) // Notify listeners
            }
            setOnCompletionListener {
                _isPlaying = false // Update custom state
                notifyPlaybackStateChanged(false) // Notify listeners
                completionListener?.onTrackCompleted()
            }
        }

        currentTrackUri = trackUri
    }


    fun pauseTrack() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                lastPosition = it.currentPosition
                it.pause()
                _isPlaying = false // Update custom state
                notifyPlaybackStateChanged(false) // Notify listeners
            }
        }
    }

    fun stopTrack() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        currentTrackUri = null
        _isPlaying = false // Update custom state
        notifyPlaybackStateChanged(false) // Notify listeners
    }

    fun getCurrentPosition(): Int? = mediaPlayer?.currentPosition
    fun getDuration(): Int? = mediaPlayer?.duration

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun enableLooping(loop: Boolean) {
        isLooping = loop
        mediaPlayer?.isLooping = loop
    }

    fun resumeTrack() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying = true // Update custom state
                notifyPlaybackStateChanged(true) // Notify listeners
            }
        }
    }

    fun getMediaPlayer(): MediaPlayer? = mediaPlayer

    private fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        playbackStateListeners.forEach { it.onPlaybackStateChanged(isPlaying) }
    }

    interface PlaybackStateListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    interface CompletionListener {
        fun onTrackCompleted()
    }
}
