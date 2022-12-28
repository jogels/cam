package com.example.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.login.databinding.ActivityGalleryBinding
import com.example.login.databinding.ActivityLoginBinding
import com.example.login.model.Video

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpListVideo()
    }

fun setUpListVideo(){
    val listVideo = getListVideo()
    val adapter = GalleryAdapter(listVideo, onItemClick = {
        val intent = Intent (this, GalleryDetailActivity::class.java)
        intent.putExtra("VIDEO", it)
        startActivity(intent)
    }, onDeleteItemClick = {
        com.example.login.deleteFile(it.videoUrl)
    })
    binding.rvVideo.adapter = adapter
}

    fun getListVideo():List<Video> {
        val directory =  getExternalFilesDir(CamService.VIDEO_DIRECTORY_NAME)
        val files = directory?.listFiles()
        val videos = mutableListOf<Video>()
        files?.forEach {
            videos.add(Video(
                title=it.name,videoUrl=it.path, size = getFileSize(it.path),
                duration = getReadableDuration(it.getMediaDuration(this))
            ))
        }
      return videos
    }
}