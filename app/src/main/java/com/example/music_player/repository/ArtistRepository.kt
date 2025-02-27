package com.example.music_player.repository

import com.example.music_player.model.ArtistName
import com.example.music_player.util.Constants
import com.example.music_player.util.Constants.drakeImg
import com.example.music_player.util.Constants.weekImg


class ArtistRepository {
    fun getImageItems(): List<ArtistName> {
        return listOf(
            ArtistName(Constants.edImg, "Ed Sheeran"),
            ArtistName(drakeImg, "Drake"),
            ArtistName(Constants.arianaImg, "Ariana Grande"),
            ArtistName(Constants.billieImg, "Billie Eilish"),
            ArtistName(weekImg, "The Weeknd")
        )
    }
}




