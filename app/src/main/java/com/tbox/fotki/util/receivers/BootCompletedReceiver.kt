package com.tbox.fotki.util.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tbox.fotki.util.sync_files.AlarmHelper

class BootCompletedReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (ACTION == intent!!.action) {
            AlarmHelper().getInstance().startAlarms(context!!)
        }
    }
    companion object{
        private const val ACTION = "android.intent.action.BOOT_COMPLETED"
    }
}