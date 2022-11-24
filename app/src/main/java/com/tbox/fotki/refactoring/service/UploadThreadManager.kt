package com.tbox.fotki.refactoring.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.refactoring.screens.upload_files.UploadMultipleFilesService
import com.tbox.fotki.util.L
import com.tbox.fotki.util.Utility
import com.tbox.fotki.util.upload_files.FileType
import java.util.ArrayList

class UploadThreadManager(val activity: AppCompatActivity) {

    lateinit var myService: UploadMultipleFilesService
    private var isBound = false

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder = service as UploadMultipleFilesService.MyLocalBinder
            myService = binder.getService()
            isBound = true

            L.print(this,"TAG Connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            L.print(this,"TAG Not connected")
        }
    }

    init {
        val intent = Intent(activity, UploadFilesService::class.java)
        activity.bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
    }

    fun startUploadService(
        albumName: String, fileTypes: ArrayList<FileType>, sessionId: String,
        albumId: Long, itemCounter: Int, isDelete: Boolean, isCompressionAllow:Boolean
    ) {
        fileTypes.forEach { file ->
            //myService.upload(file)
        }
/*
        myService.startUpload(
            UploadProperty(
                albumName,
                fileTypes,
                sessionId,
                albumId,
                itemCounter,
                isDelete,
                0L,
                false,
                true,
                isCompressionAllow
            )
        )
*/
    }

    fun stopUploadService() {
        myService.stopUpload()
    }
    fun pauseUploadService() {
        myService.pauseUploadService()
    }
    fun resumeUploadService() {
        myService.resumeUploadService()
    }
    fun destroy() {

        val uploadProperty = UploadProperty()
        uploadProperty.fromPreferences(activity)
        uploadProperty.isUploading = false
        uploadProperty.toPreferences(activity)

        activity.unbindService(myConnection)
        val intent = Intent(activity, UploadThreadService::class.java)
        activity.stopService(intent)
    }

    fun testHasUploads(success: () -> Unit) {
        val property = UploadProperty()
        property.fromPreferences(activity)
        L.print(this,"TAG property - $property")

        if (property.albumId == 0L) return

        if (property.itemCounter < property.fileTypes.size - 1 && !property.isUploading) {
            Utility.instance.showResumeAlertDialog(activity,
                activity.getString(R.string.has_uploads_text)) {
                resumeUploadService()
                success.invoke()
            }
        }
    }
}