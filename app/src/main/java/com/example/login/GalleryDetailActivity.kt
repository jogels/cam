package com.example.login

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.MediaController
import androidx.core.content.FileProvider
import com.example.login.databinding.ActivityGalleryBinding
import com.example.login.databinding.ActivityGalleryDetailBinding
import com.example.login.databinding.ActivityHomeBinding
import com.example.login.model.Video
import java.io.File
import java.util.*

class GalleryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val video = intent.getParcelableExtra<Video>("VIDEO")
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.vvPlay)
        binding.vvPlay.setMediaController(mediaController)
        binding.vvPlay.setVideoPath(video?.videoUrl)
        binding.vvPlay.start()

        binding.btnShare.setOnClickListener(){
            val file = File(video?.videoUrl.orEmpty())
            val videoURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(
                    Objects.requireNonNull(applicationContext),
                    BuildConfig.APPLICATION_ID + ".provider", file)
            else
                Uri.fromFile(file)
            shareVideo(videoURI)
        }
    }

    fun shareVideo(uriToVideo: Uri){
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uriToVideo)
            type = "video/mp4"
        }
        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))

    }

}