package com.example.music_player.apis

import com.example.music_player.model.MyData
import com.example.music_player.util.Constants.apiKey
import com.example.music_player.util.Constants.deezerApi
import com.example.music_player.util.Constants.q
import com.example.music_player.util.Constants.search
import com.example.music_player.util.Constants.xRapidApiHost
import com.example.music_player.util.Constants.xRapidApikey
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiInterface {


    @Headers(
        "$xRapidApikey: $apiKey",
        "$xRapidApiHost: $deezerApi"
    )
    @GET(search)
    suspend fun getData(
        @Query(q) query: String
    ): Response<MyData>


}

