package com.example.login.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val title:String,
    val videoUrl:String,
    val size:String,
    val duration: String

):Parcelable
