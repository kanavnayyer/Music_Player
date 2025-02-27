package com.example.music_player.viewModel





import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.music_player.util.MediaPlayerManager

class PlaybackViewModel : ViewModel() {
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    init {

        MediaPlayerManager.addPlaybackStateListener(object : MediaPlayerManager.PlaybackStateListener {
            override fun onPlaybackStateChanged(isPlaying: Boolean) {
                _isPlaying.postValue(isPlaying)
            }
        })
    }

    fun setPlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

}
