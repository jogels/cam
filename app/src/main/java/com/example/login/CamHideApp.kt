package com.example.login

import android.app.Application
import android.util.Log
import java.util.*
import kotlin.concurrent.timerTask

class CamHideApp : Application() {
    var logoutListener: LogoutListener? = null
    var timer: Timer? = null
    override fun onCreate() {
        super.onCreate()

    }

    fun onUserSessionStart() {
        timer?.cancel()

        timer = Timer()

        timer?.schedule(timerTask {
            logoutListener?.onSessionLogout()
            Log.d("auto logout", "logout session")
        }, 15000)
    }

    fun resetSession() {
        onUserSessionStart()
        Log.d("auto logout", "reset session")

    }

    fun registerSessionListener(listener: LogoutListener) {
        logoutListener = listener
        Log.d("auto logout", "register session")
    }

}