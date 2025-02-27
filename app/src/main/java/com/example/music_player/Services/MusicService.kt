package com.example.music_player.Services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.music_player.R
import com.example.music_player.model.Data
import com.example.music_player.util.MediaPlayerManager

class MusicService : Service() {
    private var currentTrack: Data? = null
    private lateinit var handler: Handler
    private var notificationLayout: RemoteViews? = null
    private var isUpdatingSeekBar = false
    private var currentAlbumArt: Bitmap? = null

    private var trackList: List<Data> = emptyList()
    private var currentIndex: Int = 0

    private val playbackStateListener = object : MediaPlayerManager.PlaybackStateListener {
        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            currentTrack?.let { track ->
                updateNotification(track, isPlaying)
                sendPlaybackStateBroadcast(isPlaying)
            }
        }
    }


    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        MediaPlayerManager.addPlaybackStateListener(playbackStateListener)
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            ACTION_PLAY -> {
//                val track = intent.getParcelableExtra<Data>("TRACK_DATA")
//                if (track != null) startMusicService(track)
//            }
//            ACTION_PAUSE -> pauseMusic()
//            ACTION_STOP -> stopMusic()
//        }
//        return START_STICKY
//    }
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) {
        Log.e("MusicService", "Intent is null in onStartCommand")
        return START_NOT_STICKY
    }

    when (intent.action) {
        ACTION_PLAY -> {
            val track = intent.getParcelableExtra<Data>("TRACK_DATA")

            if (track != null) {
                currentTrack = track  // ✅ Always update the track
                startMusicService(track)
            } else if (currentTrack != null) {
                // ✅ If no new track is provided, restart last played track
                startMusicService(currentTrack!!)
            } else {
                Log.e("MusicService", "No track available to play")
            }
        }

        ACTION_PAUSE -> pauseMusic()
        ACTION_STOP -> stopMusic()
    }

    return START_STICKY
}



    private fun startMusicService(track: Data) {
        currentTrack = track
        playMusic()
    }



    private fun playMusic() {
        currentTrack?.let { track ->
            val mediaPlayer = MediaPlayerManager.getMediaPlayer()
            if (mediaPlayer == null || !mediaPlayer.isPlaying) {
                MediaPlayerManager.playTrack(this, Uri.parse(track.preview))
                MediaPlayerManager.isPlaying = true // Ensure state is updated
                startSeekBarUpdate()
            } else {
                MediaPlayerManager.resumeTrack()
                MediaPlayerManager.isPlaying = true // Ensure state is updated
            }
            updateNotification(track, true)
            sendPlaybackStateBroadcast(true)
        }
    }

    private fun pauseMusic() {
        MediaPlayerManager.pauseTrack()
        MediaPlayerManager.isPlaying = false
        stopSeekBarUpdate()
        currentTrack?.let { track ->
            updateNotification(track, false)
        }
        sendPlaybackStateBroadcast(false)
    }

    private fun startSeekBarUpdate() {
        if (isUpdatingSeekBar) return

        isUpdatingSeekBar = true
        handler.post(object : Runnable {
            override fun run() {
                val position = MediaPlayerManager.getCurrentPosition() ?: 0
                val duration = MediaPlayerManager.getDuration() ?: 1
                val intent = Intent("SEEK_BAR_UPDATE").apply {
                    putExtra("currentPosition", position)
                    putExtra("duration", duration)
                }
                sendBroadcast(intent)

                if (isUpdatingSeekBar) {
                    handler.postDelayed(this, 1000) // Update every second
                }
            }
        })
    }

    private fun stopMusic() {
        MediaPlayerManager.stopTrack()
        MediaPlayerManager.isPlaying = false // Ensure state is updated
        stopSeekBarUpdate()
        stopForeground(true)
        stopSelf()
    }

    private fun stopSeekBarUpdate() {
        handler.removeCallbacksAndMessages(null)
        isUpdatingSeekBar = false
    }

    private fun updateNotification(track: Data, isPlaying: Boolean) {
        Glide.with(this).asBitmap().load(track.album.cover_medium).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                showNotification(track, resource, isPlaying)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun showNotification(track: Data, albumArt: Bitmap?, isPlaying: Boolean) {
        if (albumArt!=null){
            currentAlbumArt=albumArt
        }
        notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.notificationTitle, track.title)
            setTextViewText(R.id.notificationArtist, track.artist.name)
            setImageViewBitmap(R.id.notificationAlbumArt, albumArt)
            setImageViewResource(R.id.notificationPlayPause, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            setOnClickPendingIntent(R.id.notificationPlayPause, getPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY))
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setCustomContentView(notificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(isPlaying)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }


    private fun getPendingIntent(action: String): PendingIntent {
        val requestCode = if (action == ACTION_PLAY) 1 else 2

        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }

        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun handleIntentAction(action: String) {
        when (action) {
            ACTION_PLAY -> {
                MediaPlayerManager.resumeTrack()
                showNotification(currentTrack!!, currentAlbumArt, true)
            }
            ACTION_PAUSE -> {
                MediaPlayerManager.pauseTrack()
                showNotification(currentTrack!!, currentAlbumArt, false)
            }
        }
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW
            ).apply { setSound(null, null) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun sendPlaybackStateBroadcast(isPlaying: Boolean) {
        val intent = Intent("MUSIC_PLAYBACK_STATE").apply {
            putExtra("isPlaying", isPlaying)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaPlayerManager.removePlaybackStateListener(playbackStateListener)
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}