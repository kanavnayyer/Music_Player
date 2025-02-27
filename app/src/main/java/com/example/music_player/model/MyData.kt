package com.example.music_player.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MyData(
    val data: List<Data>,
    val next: String,
    val total: Int
):Parcelable