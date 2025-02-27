package com.example.music_player.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

object MediaPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackUri: Uri? = null
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean
        get() = _isPlaying

    private var lastPosition: Int = 0
    private var isLooping: Boolean = false
    private var playbackStateListener: PlaybackStateListener? = null
    private var completionListener: CompletionListener? = null

    fun setPlaybackStateListener(listener: PlaybackStateListener) {
        playbackStateListener = listener
    }

    fun removePlaybackStateListener() {
        playbackStateListener = null
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
            setIsPlaying(true)
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
                setIsPlaying(true)
            }
            setOnCompletionListener {
                setIsPlaying(false)
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
                setIsPlaying(false)
            }
        }
    }

    fun stopTrack() {
        mediaPlayer?.release()
        mediaPlayer = null
        lastPosition = 0
        setIsPlaying(false)
        currentTrackUri = null
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
            }
        }
    }


    private fun setIsPlaying(value: Boolean) {
        _isPlaying = value
        playbackStateListener?.onPlaybackStateChanged(value)
    }

    fun getMediaPlayer(): MediaPlayer? {
        return mediaPlayer
    }

    interface PlaybackStateListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    interface CompletionListener {
        fun onTrackCompleted()
    }
}
