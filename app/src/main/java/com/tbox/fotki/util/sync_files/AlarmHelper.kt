package com.tbox.fotki.util.sync_files

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tbox.fotki.util.receivers.AddPhotoReceiver
import java.util.*


class AlarmHelper {
    //TODO hardcoded string
    private var am: AlarmManager? = null
    private var sender: PendingIntent? = null

    private var instance: AlarmHelper? = null

    fun getInstance(): AlarmHelper {
        if (instance == null) {
            instance = AlarmHelper()
        }
        return instance!!
    }

    private fun changeAlarms(context: Context, calendarTime: Long, isStart: Boolean) {

        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sender = Intent(context, AddPhotoReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, 0, intent, 0)
        }
        if (isStart) {


            am?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendarTime+START_DELAY,
                INTERVAL,
                sender
            )
            LogProvider.writeToFile("Alarms started at " +
                    LogProvider.convertTimeToTemplate(calendarTime),context)
        }else
            am?.cancel(sender)
    }

    fun startAlarms(context: Context) {
        changeAlarms(context, Calendar.getInstance().timeInMillis, true)
    }

    fun stopAlarms(context: Context) {
        changeAlarms(context, 0, false)
    }

    companion object {
        private const val REQUEST_ALARM = 1234
        private const val START_DELAY = 1000*5
        private const val INTERVAL = (1000 * 60 * 5).toLong()
    }
}