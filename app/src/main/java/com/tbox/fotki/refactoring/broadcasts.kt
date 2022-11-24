package com.tbox.fotki.refactoring

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.tbox.fotki.refactoring.auth_providers.SessionManager
import com.tbox.fotki.refactoring.service.PhotoUploader
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.DownloadTask
import com.tbox.fotki.util.L
import com.tbox.fotki.util.updateProgressbarStatus


class SessionExpiredBroadcastReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        SessionManager.setSessionExpired(context)
    }
}

class SharingProgressBroadcastReceiver(val progressBar: ProgressBar): BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            Constants.SHARING_FILE_DOWNLOAD_STATUS, 0
        )
        progressBar.updateProgressbarStatus(status)

        L.print(this,"Shared status - $status")
    }
}

class SharingFileBroadcastReceiver(val activity: Activity, val rlProgressBar: RelativeLayout):BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        rlProgressBar.visibility = View.GONE
        L.print(this,"Shared finish received!")
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val downloadTask = DownloadTask.getInstance()
        if (downloadTask.isCompression && downloadTask.mFiletype.equals("jpg") && !downloadTask.isCompressionFailed) {
            downloadTask.isCompressionFailed = false
            downloadTask.isCompression = false
            PhotoUploader.shareFile(activity,PhotoUploader.TYPE_FOTKI_RESIZE)
        } else {
            PhotoUploader.shareFile(activity,PhotoUploader.TYPE_FOTKI_USUAL)
        }
    }
}

class SharingErrorBroadcastReceiver(val rlProgressBarLayout:RelativeLayout) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        rlProgressBarLayout.visibility = View.GONE
        L.print(this,"Shared error!")
    }
}