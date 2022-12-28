package com.example.login

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.login.databinding.ActivityHomeBinding
import com.example.login.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val helper: FirebaseHelper = FirebaseHelper()
    private val helperReference: PreferenceHelper = PreferenceHelper()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                CamService.ACTION_STOPPED -> flipButtonVisibility(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        helper.initFirebase()
        helperReference.initPreference(this)
        initView()


    }

    private fun checkPermission(onPermissionGranted: () -> Unit) {
        val permission = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // We don't have camera permission yet. Request it from the user.
            ActivityCompat.requestPermissions(this, permission, CODE_PERM_CAMERA)
        } else {
            onPermissionGranted.invoke()
        }

    }

    override fun onResume() {
        super.onResume()

        registerReceiver(receiver, IntentFilter(CamService.ACTION_STOPPED))

        val running = isServiceRunning(this, CamService::class.java)
        flipButtonVisibility(running)
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(receiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CODE_PERM_CAMERA -> {
                if (grantResults?.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        getString(R.string.err_no_cam_permission),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun initView() {

        binding.butStart.setOnClickListener {
            checkPermission {
                if (!isServiceRunning(this, CamService::class.java)) {
                    notifyService(CamService.ACTION_START)
                    finish()
                }
            }
        }

        binding.butStartPreview.setOnClickListener {
            checkPermission {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

                    // Don't have permission to draw over other apps yet - ask user to give permission
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivityForResult(settingsIntent, CODE_PERM_SYSTEM_ALERT_WINDOW)
                    return@checkPermission
                }

                if (!isServiceRunning(this, CamService::class.java)) {
                    notifyService(CamService.ACTION_START_WITH_PREVIEW)
                }
            }

        }

        binding.btnLogout.setOnClickListener{
            helper.logout()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
        binding.butStop.setOnClickListener {
            stopService(Intent(this, CamService::class.java))
        }

        binding.gallery.setOnClickListener{
            startActivity(Intent(this, GalleryActivity::class.java))
        }
    }

    private fun notifyService(action: String) {
        prepareVideoName {
            videoName,area,user_id,id_intv->
            val intent = Intent(this, CamService::class.java)
            intent.putExtra("VIDEO_NAME", videoName)
            intent.putExtra("AREA_NAME", area)
            intent.putExtra("USER_ID",user_id)
            intent.putExtra("ID_INTV",id_intv)
            intent.action = action
            startService(intent)
        }

    }

    private fun flipButtonVisibility(running: Boolean) {

        binding.butStart.visibility = if (running) View.GONE else View.VISIBLE
        binding.butStartPreview.visibility = if (running) View.GONE else View.VISIBLE
        binding.butStop.visibility = if (running) View.VISIBLE else View.GONE
    }

    fun prepareVideoName(onSuccess: (videoName: String, area: String, user_id:String, id_intv:String) -> Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            helper.getUserData(
                user_id = user.uid,
                onSuccess = {
                    val kodeGerai = helperReference.getKodeGerai()
                    val status = helperReference.getStatus()
                    val region = helperReference.getRegion()
                    val storeName = helperReference.getStoreName()
                    val visit = helperReference.getVisit()
                    val fullName = it["fullname"]
                    val area = it["area"]
                    val videoName = "${kodeGerai}_${status}_${region}_${storeName}_${visit}_${fullName}_"

                    onSuccess.invoke(videoName, area.toString(), user.uid,
                        ""
                    )
                },
                onFailure = {
                    Toast.makeText(this, "Error:$it", Toast.LENGTH_LONG).show()
                }
            )
        }
    }


    companion object {

        val CODE_PERM_SYSTEM_ALERT_WINDOW = 6111
        val CODE_PERM_CAMERA = 6112

    }
}