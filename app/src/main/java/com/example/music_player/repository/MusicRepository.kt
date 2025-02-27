package com.example.music_player.repository

import com.example.music_player.apis.ApiInterface
import com.example.music_player.apis.RetrofitInstance
import com.example.music_player.model.MyData
import retrofit2.Response

class MusicRepository {
    private   val api:ApiInterface=RetrofitInstance.api
    suspend fun searchTracks(query: String): Response<MyData> {
        return api.getData(query)
    }
}