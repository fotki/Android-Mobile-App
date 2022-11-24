package com.tbox.fotki.refactoring.general

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tbox.fotki.util.L
import com.tbox.fotki.util.NotificationHelperNew
import com.tbox.fotki.view.activities.SplashScreenActivity
import org.koin.android.ext.android.inject
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules

abstract class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundService"

    protected lateinit var  notificationHelper: NotificationHelperNew
    companion object {

        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelperNew(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    protected fun startWithForeground(id:Int, message: String) {
        val notificationIntent = Intent(this, SplashScreenActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, 0
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationHelper.createNotificationChannelNoizy(
                NotificationHelperNew.CHANNEL_ID,
                NotificationHelperNew.CHANNEL_NAME,
                NotificationHelperNew.CHANNEL_DESCRIPTION,true)

        }
        val notification = notificationHelper.getNotification(
            "Service started",
            null,
            "Service started",
            false
        )



        startForeground(id, notification)

    }

    override fun onDestroy() {
        L.print(this,"service DESTROY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            serviceChannel.description = "Service foreground"

            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
}