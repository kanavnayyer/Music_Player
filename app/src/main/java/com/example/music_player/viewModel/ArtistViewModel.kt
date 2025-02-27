package com.example.music_player.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.music_player.model.ArtistName
import com.example.music_player.repository.ArtistRepository


class ArtistViewModel : ViewModel() {
    private val repository = ArtistRepository()
    private val _imageItems = MutableLiveData<List<ArtistName>>()
    val imageItems: LiveData<List<ArtistName>> get() = _imageItems

    init {
        loadImageItems()
    }

    private fun loadImageItems() {
        _imageItems.value = repository.getImageItems()
    }
}