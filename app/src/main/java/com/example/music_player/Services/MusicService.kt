package com.example.music_player.Services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.music_player.R
import com.example.music_player.model.Data
import com.example.music_player.util.MediaPlayerManager

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Data? = null
    private lateinit var handler: Handler
    private var notificationLayout: RemoteViews? = null
    private var isUpdatingSeekBar = false

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val track = intent.getParcelableExtra<Data>("TRACK_DATA")
                if (track != null) startMusicService(track)
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
        if (MediaPlayerManager.getMediaPlayer()?.isPlaying == true) return

        currentTrack?.let { track ->
            val mediaPlayer = MediaPlayerManager.getMediaPlayer()
            if (mediaPlayer != null && mediaPlayer.isPlaying.not() && mediaPlayer.currentPosition > 0) {
                // Resume playback if already initialized
                MediaPlayerManager.resumeTrack()
            } else {
                // Start new playback if not initialized
                val trackUri = Uri.parse(track.preview)
                MediaPlayerManager.playTrack(this, trackUri)
            }

            startSeekBarUpdate()

            Glide.with(this).asBitmap().load(track.album.cover_medium).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    showNotification(track, resource, isPlaying = true)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        }
    }


    private fun pauseMusic() {
        MediaPlayerManager.pauseTrack()
        stopSeekBarUpdate()

        currentTrack?.let { track ->
            Glide.with(this).asBitmap().load(track.album.cover_medium).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    showNotification(track, resource, isPlaying = false)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        }
    }



    private fun stopMusic() {
        MediaPlayerManager.stopTrack()
        stopSeekBarUpdate()
        stopForeground(true)
        stopSelf()
    }


    private fun showNotification(track: Data, albumArt: Bitmap?, isPlaying: Boolean) {
        notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.notificationTitle, track.title)
            setTextViewText(R.id.notificationArtist, track.artist.name)

            if (albumArt != null) {
                setImageViewBitmap(R.id.notificationAlbumArt, albumArt)
            } else {
                setImageViewResource(R.id.notificationAlbumArt, R.drawable.ic_launcher_background)
            }

            setImageViewResource(R.id.notificationPlayPause, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            setOnClickPendingIntent(R.id.notificationPlayPause, getPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY))
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setCustomContentView(notificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun updateNotificationSeekBar() {
        val mediaPlayer = MediaPlayerManager.getMediaPlayer() ?: return
        val duration = mediaPlayer.duration

        if (duration > 0) {
            val progress = (mediaPlayer.currentPosition * 100) / duration
            notificationLayout?.setProgressBar(R.id.notificationSeekBar, 100, progress, false)

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildUpdatedNotification())
        }
    }


    private fun scheduleSeekBarUpdate() {
        if (isUpdatingSeekBar) return
        isUpdatingSeekBar = true
        handler.post(object : Runnable {
            override fun run() {
                updateNotificationSeekBar()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun stopSeekBarUpdate() {
        handler.removeCallbacksAndMessages(null)
        isUpdatingSeekBar = false
    }

    private fun startSeekBarUpdate() {
        scheduleSeekBarUpdate()
    }

    private fun buildUpdatedNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setCustomContentView(notificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
