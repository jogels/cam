package com.example.login

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity(), LogoutListener {

    val camHideApp : CamHideApp = CamHideApp()
    val helper : FirebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.initFirebase()
        Log.d("auto logout", "on Create BaseActivity ")

    }


    override fun onResume() {
        super.onResume()
        camHideApp.registerSessionListener( this)
        Log.d("auto logout", "on Resume")

    }

    override fun onSessionLogout() {
      helper.logout()
        Log.d("auto logout", "logout Base activity")

    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        camHideApp.resetSession()
        Log.d("auto logout", "on User Interaction")


    }

}