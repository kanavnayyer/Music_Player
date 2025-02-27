package com.example.music_player.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_player.model.MyData
import com.example.music_player.repository.MusicRepository
import kotlinx.coroutines.launch
import retrofit2.Response

class MusicViewModel() : ViewModel() {
private val repository=MusicRepository()
    private val _tracks = MutableLiveData<MyData?>()
    val tracks: LiveData<MyData?> get() = _tracks

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchTracks(query: String) {
        viewModelScope.launch {
            try {
                val response: Response<MyData> = repository.searchTracks(query)
                if (response.isSuccessful) {
                    _tracks.postValue(response.body())
                } else {
                    _error.postValue("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue("Exception: ${e.message}")
            }
        }
    }
}
