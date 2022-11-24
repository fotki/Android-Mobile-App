package com.tbox.fotki.view_model

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.tbox.fotki.model.entities.ProgressEntity

open class ProgressNavigationViewModel:BaseViewModel() {

    val uploadingProgressVisibility = MutableLiveData<Int>()
    val progressMessage = MutableLiveData<String>()
    val uploadingProgressMessage = MutableLiveData<String>()
    val uploadingProgressMax = MutableLiveData<Int>()
    val uploadingProgressValue = MutableLiveData<Int>()
    val isSyncInProgress = MutableLiveData<Boolean>()

    protected val mAllUploadDone = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUloadProgress(ProgressEntity(intent))
            uploadingComplete()
        }
    }
    protected val mUploadingStarted = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUloadProgress(ProgressEntity(intent))
        }
    }
    protected val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUloadProgress(ProgressEntity(intent))
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updateUloadProgress(progressEntity: ProgressEntity) {
        //isSyncInProgress.value = true
        uploadingProgressVisibility.value = View.VISIBLE
        uploadingProgressMax.value = progressEntity.totalFiles

        var fileBeingUploading = progressEntity.currentFileCount
        if (fileBeingUploading < progressEntity.totalFiles) {
            if (fileBeingUploading > 0) {
                fileBeingUploading -= 1
            }
        }
        uploadingProgressValue.value = fileBeingUploading
        if (progressEntity.isBackup)
            uploadingProgressMessage.value = "$fileBeingUploading of ${progressEntity.totalFiles}  " +
                    "files has been uploaded. Backup in progress..."
        else
            uploadingProgressMessage.value = "$fileBeingUploading of ${progressEntity.totalFiles}  " +
                   "files has been uploaded."
    }

    protected fun uploadingComplete() {
        uploadingProgressVisibility.value = View.GONE
        isSyncInProgress.value = false
    }

}