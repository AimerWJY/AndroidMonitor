package com.aimerwjy.androidmonitor

import android.app.Application
import com.aimerwjy.androidmonitor.crash.JavaCrashHandler

class AndroidMonitorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initCrashHandler()
    }

    private fun initCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler(JavaCrashHandler(this))
    }

}