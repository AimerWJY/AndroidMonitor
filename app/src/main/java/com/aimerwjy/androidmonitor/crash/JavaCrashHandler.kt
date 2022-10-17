package com.aimerwjy.androidmonitor.crash

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.io.*
import java.util.*
import kotlin.system.exitProcess

class JavaCrashHandler : Thread.UncaughtExceptionHandler {

    private var mContext: Context

    constructor(context: Context) {
        mContext = context
    }


    override fun uncaughtException(t: Thread, ex: Throwable) {
        if (!handleException(ex)) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(t, ex)
        } else {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
            }
            // 退出程序
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        // 使用Toast来显示异常信息
        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_SHORT)
                    .show()
                Looper.loop()
            }
        }.start()
        // 收集设备参数信息
        collectDeviceInfo(mContext)
        return true
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    fun collectDeviceInfo(ctx: Context) {
        try {
            val pm = ctx.packageManager
            val pi = pm.getPackageInfo(
                ctx.packageName,
                PackageManager.GET_ACTIVITIES
            )
            if (pi != null) {
                val versionName = if (pi.versionName == null) "null" else pi.versionName
                val versionCode = pi.versionCode.toString() + ""
            }
        } catch (e: PackageManager.NameNotFoundException) {

        }
        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
            } catch (e: Exception) {

            }
        }
    }

}