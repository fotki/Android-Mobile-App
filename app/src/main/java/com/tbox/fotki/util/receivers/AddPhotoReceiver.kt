package com.tbox.fotki.util.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tbox.fotki.util.L
import com.tbox.fotki.util.LocalBroadcastHelper
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.util.sync_files.LogProvider
import com.tbox.fotki.util.sync_files.MediaSyncronizator
import com.tbox.fotki.util.sync_files.NetworkDetector

class AddPhotoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        L.print(this,"Add photo")
        LocalBroadcastHelper.sendProgressFileSync(context!!,true)
        LogProvider.writeToFile(LogProvider.START_PHOTO_RECEIVER, context!!)

        val mediaSyncronizator = MediaSyncronizator(context)
        val backupProperties = BackupProperties(context)

        LogProvider.writeToFile(
            "${LogProvider.RECEIVED_WITH_PROPERTIES} $backupProperties", context)

        var testRes = backupProperties.folderList.size > 0
        LogProvider.writeToFile(
            "${LogProvider.IS_NEED_TO_SYNC_WITH_FOLDERS} $testRes", context)

        if (!backupProperties.isBackupRoaming.value!!) {
            testRes = !NetworkDetector.testIsRoaming(context)
        }
        LogProvider.writeToFile(
            "${LogProvider.IS_NEED_TO_SYNC_WITH_ROAMING} $testRes", context)

        if (testRes) {
            LogProvider.writeToFile(LogProvider.NOT_IN_ROAMING_SERVICE_START, context)
            mediaSyncronizator.readAndSync()
        } else {
            LogProvider.writeToFile(LogProvider.IN_ROAMING_DETECTED, context)
        }
    }

    companion object {
        const val ACTION = "com.tbox.fotki.ADD_PHOTO"
    }
}