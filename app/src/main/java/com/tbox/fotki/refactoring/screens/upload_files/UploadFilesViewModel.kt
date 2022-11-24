package com.tbox.fotki.refactoring.screens.upload_files

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.upload_files.FileUploader
import org.json.JSONObject

class UploadFilesViewModel:ViewModel() {

    val currentStatus = MutableLiveData<UploadStatus>()
    lateinit var broadcasts: HashMap<String, BroadcastReceiver>

    fun initBroadCasts(startUpload:()->Unit, finishUpload:()->Unit, errorUpload:()->Unit){
        broadcasts = hashMapOf(
            Constants.DOWNLOAD_STATUS_UPDATED to object:BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent) {
                    val status = intent.getStringExtra(Constants.STATUS)?:"{}"
                    val isFinished = intent.getBooleanExtra(Constants.UPLOADING_IN_PROGRESS,false)
                    if (isFinished && status == "cancel"  ){
                        Log.d("UploadFilesViewModel","cancel run")
                        currentStatus.value = UploadStatus(0f,0f,0,0,
                            "",0f, false, "",0f, false,
                            "",0f, false,"",0f, false)
                    }
                    else if(isFinished && status != "cancel"){
                        Log.d("UploadFilesViewModel","finish run")
                        currentStatus.value = UploadStatus(0f,0f,0,0,
                            "",0f, false, "",0f, false,
                            "",0f, false,"",0f, false)
                        finishUpload()
                    } else {

                        startUpload()
                        Log.d("UploadFilesViewModel","start  run status "+status)
                        currentStatus.value = convertToUploadStatus(status)
                        //currentStatus.value = UploadStatus()
                    }
                }
            },
            Constants.SHARING_FILE_ERROR to object:BroadcastReceiver(){
                override fun onReceive(p0: Context?, p1: Intent?) {
                    errorUpload()
                }

            }
        )
    }

    private fun convertToUploadStatus(status: String): UploadStatus {
        val jsonObj = JSONObject(status)
        //L.print(jsonObj.keys())

        val uploadStatus = UploadStatus(0f,0f,0,0,
            "",0f, false, "",0f, false,
            "",0f, false,"",0f, false)

        var pos = 1
        jsonObj.keys().forEach {key ->

            when (key) {
                "total" -> {
                    uploadStatus.allCount = jsonObj.getInt(key)
                }
                "loaded" -> {
                    uploadStatus.loaded = jsonObj.getInt(key)
                }
                else -> {

                    val item = jsonObj.getJSONObject(key)
                    val progress = item.getInt("progress").toFloat()
                    val type = item.getJSONObject("file_type").getString("mime_type") == "video"
                    L.print(this, "File loading status $item")
                    val fileName = item.getJSONObject("file_type").getString("file_path")

                    when(pos){
                        1 -> {
                            uploadStatus.progress1 = progress
                            uploadStatus.fileName1 = fileName
                            uploadStatus.isVideo1 = type
                        }
                        2 -> {
                            uploadStatus.progress2 = progress
                            uploadStatus.fileName2 = fileName
                            uploadStatus.isVideo2 = type
                        }
                        3 -> {
                            uploadStatus.progress3 = progress
                            uploadStatus.fileName3 = fileName
                            uploadStatus.isVideo3 = type
                        }
                        4 -> {
                            uploadStatus.progress4 = progress
                            uploadStatus.fileName4 = fileName
                            uploadStatus.isVideo4 = type
                        }
                        else -> {
                            uploadStatus.progress1 = progress
                            uploadStatus.fileName1 = fileName
                            uploadStatus.isVideo1 = type
                        }
                    }
                    pos++
                }
            }

        }
        L.print(this, "Status - $uploadStatus")
        return uploadStatus
    }


}