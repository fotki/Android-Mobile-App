package com.tbox.fotki.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.tbox.fotki.R

class NotificationHelperNew(val context: Context) {

    private val notificationManager: NotificationManager = context.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager

    fun sendNotification(title: String, resultIntent: Intent?, message: String){
        sendNotification(title,resultIntent,message,false)
    }
    fun sendNotificationNoizy(title: String, resultIntent: Intent?, message: String){
        sendNotification(title,resultIntent,message,true)
    }

    fun getNotification(title: String, resultIntent: Intent?, message: String, isNoizy:Boolean):Notification{
        if (needChannel()) {
            createNotificationChannelNoizy(
                CHANNEL_ID,
                CHANNEL_NAME,
                CHANNEL_DESCRIPTION,isNoizy)
        }

        val notification = if (needChannel()) {
            Notification.Builder(context,
                CHANNEL_ID
            )
        } else {
            Notification.Builder(context)
        }
        notification.setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(false)

        if (resultIntent!=null){
            val pendingIntent = PendingIntent.getActivity(
                context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            notification.setContentIntent(pendingIntent)
        }

        if (needChannel()) {
            notification.setChannelId(CHANNEL_ID)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (isNoizy) notification.setSound(alarmSound)
        return notification.build()
    }

    fun sendNotification(title: String, resultIntent: Intent?, message: String, isNoizy:Boolean) {
        notificationManager.notify(NOTIFICATION_ID, getNotification(title,resultIntent,message,isNoizy))
    }

    private fun needChannel() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannelNoizy(
        id: String, name: String,
        description: String , izNoizy:Boolean
    ) {
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)

        channel.description = description
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.lightColor = Color.RED
        channel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
        if (izNoizy){
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val att = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            channel.setSound(alarmSound, att)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun finishNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.cancel(NOTIFICATION_ID)
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "com.tbox.fotki.news"
        const val CHANNEL_NAME = "Uploading files"
        const val CHANNEL_DESCRIPTION = "Uploading in progress"
    }
}