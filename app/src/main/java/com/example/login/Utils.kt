package com.example.login

import android.app.ActivityManager
import android.content.Context
import android.icu.util.TimeUnit
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.net.Uri
import java.io.File
import java.util.*


/**
 * Copyright (c) 2019 by Roman Sisik. All rights reserved.
 */
fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in manager!!.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false

}

fun deleteFile(path : String){
    val file = File(path)

    val result = file.delete()
    if (result) {
        println("Deletion succeeded.")
    } else {
        println("Deletion failed.")
    }
}
val File.size get() = if (!exists()) 0.0 else length().toDouble()
val File.sizeInKb get() = size / 1024
val File.sizeInMb get() = sizeInKb / 1024
val File.sizeInGb get() = sizeInMb / 1024
val File.sizeInTb get() = sizeInGb / 1024
fun File.sizeStr(): String = size.toString()
fun File.sizeStrInKb(decimals: Int = 0): String = "%.${decimals}f".format(sizeInKb)
fun File.sizeStrInMb(decimals: Int = 0): String = "%.${decimals}f".format(sizeInMb)
fun File.sizeStrInGb(decimals: Int = 0): String = "%.${decimals}f".format(sizeInGb)

fun File.sizeStrWithBytes(): String = sizeStr() + "b"
fun File.sizeStrWithKb(decimals: Int = 0): String = sizeStrInKb(decimals) + "Kb"
fun File.sizeStrWithMb(decimals: Int = 0): String = sizeStrInMb(decimals) + "Mb"
fun File.sizeStrWithGb(decimals: Int = 0): String = sizeStrInGb(decimals) + "Gb"

fun getFileSize(path: String): String{
    val file = File(path)
    return when{
        file.length()<1024->file.sizeStrWithBytes()
        file.length()>1024->file.sizeStrWithMb()

        else->file.sizeStrWithGb()
    }
}

fun String?.asUri(): Uri? {
    try {
        return Uri.parse(this)
    } catch (e: Exception) {
    }
    return null
}

val File.uri get() = this.absolutePath.asUri()

fun File.getMediaDuration(context: Context): Long {
    if (!exists()) return 0
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, uri)
    val duration = retriever.extractMetadata(METADATA_KEY_DURATION)
    retriever.release()

    return duration?.toLongOrNull() ?: 0
}

fun getReadableDuration(duration: Long): String{
    val s: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(duration)
    val m: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(duration)
    val h: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(duration)
    return String.format("%d:%02d:%02d", h, m, s)
}