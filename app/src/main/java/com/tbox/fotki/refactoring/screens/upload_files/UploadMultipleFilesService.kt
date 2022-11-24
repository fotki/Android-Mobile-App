package com.tbox.fotki.refactoring.screens.upload_files

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.refactoring.general.ForegroundService
import com.tbox.fotki.refactoring.service.PhotoGalleryManager
import com.tbox.fotki.refactoring.service.PhotoUploader
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.LocalBroadcastHelper
import com.tbox.fotki.util.upload_files.FileType
import com.tbox.fotki.util.upload_files.FileUploader
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

data class FileStatus(
    @SerializedName("file_type") val fileType: FileType,
    @SerializedName("progress") var progress: Int = 0
)

class UploadMultipleFilesService : ForegroundService() {

    @Volatile
    private var filesQueue = java.util.LinkedHashMap<String, FileStatus>()
    @Volatile
    private var allFiles = HashMap<String, FileType>()
    @Volatile
    private var filesToUpload = LinkedHashMap<String, FileType>()

    private lateinit var uploadProperty: UploadProperty
    private val myBinder = MyLocalBinder()

    private lateinit var executorService:ExecutorService
    private lateinit var statusService:ExecutorService
    //private val queueFiles = MutableLiveData<UploadStatus>()

    private var isStarted = false

    inner class MyLocalBinder : Binder() {
        fun getService(): UploadMultipleFilesService {
            return this@UploadMultipleFilesService
        }
    }
    val photoUploader = PhotoUploader()

    fun startUpload(
        property: UploadProperty, isRestarted:Boolean = false
    ) {
        L.print(this, "service start upload")
        property.isUploading = true
        property.isFinished = false
        property.toPreferences(this)

        executorService = Executors.newFixedThreadPool(4)
        statusService = Executors.newFixedThreadPool(1)
        filesQueue = java.util.LinkedHashMap<String, FileStatus>()

        if(!isRestarted || filesToUpload.isEmpty() ){

            allFiles = HashMap()
            filesToUpload = LinkedHashMap()

            uploadProperty = property
            L.print(this, "Start uploading - ${property.fileTypes} from ${uploadProperty.itemCounter}")

            allFiles.clear()
            /*val startItem = if(uploadProperty.itemCounter==0) 0 else {
                uploadProperty.itemCounter = uploadProperty.itemCounter-1
                uploadProperty.itemCounter-1
            }*/

            for(i in uploadProperty.itemCounter until uploadProperty.fileTypes.size){
                var fileType = uploadProperty.fileTypes[i]
                allFiles[fileType.mFilePath] = fileType
                filesToUpload[fileType.mFilePath] = fileType
            }
/*
            property.fileTypes.forEachIndexed { i, fileType ->
                allFiles[fileType.mFilePath] = fileType
                filesToUpload[fileType.mFilePath] = fileType
            }
*/
        }

        val max = if(allFiles.size<4) allFiles.size else 4
        for(file in 1..max){
            loadNext()
        }

        isStarted = true

        statusService.submit {
            while (true) {
                L.print(this, "UploadStatus notification $filesQueue")
                publishProgressOutside()
                Thread.sleep(1000)
            }
        }
        L.print(this, "List of files - $allFiles")
    }

    @Synchronized
    private fun publishProgressOutside() {
        val json = Gson().toJson(filesQueue)
        val obj = JSONObject(json)
        obj.put("total", uploadProperty.fileTypes.size)
        obj.put("loaded", uploadProperty.itemCounter)

        val resultIntent = Intent(this, FotkiTabActivity::class.java)
        notificationHelper.sendNotification(
            "Upload in progress", resultIntent,
            "Uploaded - ${uploadProperty.itemCounter} / ${uploadProperty.fileTypes.size} photo"
        )
        Log.d("UploadFilesViewModel","UploadMultipleFilesService publishProgressOutside")

        LocalBroadcastHelper.sendNewDownloadStatus(
            this@UploadMultipleFilesService,
            obj.toString(), false
        )
        L.print(this, "UploadStatus - $obj")
    }

    /*private fun uploadFakeFile(fileType: FileType) {

        val messageStart = Message()
        messageStart.arg1 = INSERT_FLAG
        messageStart.obj = fileType.mFilePath
        handler.sendMessage(messageStart)

        val step = Random().nextInt(5)+5
        var i = 1
        while (i < 100) {
            i += step
            val message = Message()
            message.arg1 = i
            message.obj = fileType.mFilePath
            L.print(this, "From while - $i")
            handler.sendMessage(message)
            Thread.sleep(1000)
        }

        val messageEnd = Message()
        messageEnd.arg1 = DELETE_FLAG
        messageEnd.obj = fileType.mFilePath
        handler.sendMessage(messageEnd)
    }*/

    fun pauseUploadService() {
        notificationHelper.finishNotification()
        executorService.shutdownNow()
        statusService.shutdownNow()
        L.print(this,"paused queue size - ${filesQueue.size}")
        L.print(this,"paused filesToUpload - ${filesToUpload.size}")
        L.print(this, "uploaded - ${uploadProperty.itemCounter}")

        for(fileStatus in filesQueue){
            filesToUpload[fileStatus.key] = fileStatus.value.fileType
        }
        L.print(this,"pausing with data - $filesToUpload")
        L.print(this,"paused size - ${filesToUpload.size}")

        uploadProperty.isUploading = false
        uploadProperty.toPreferences(this)

        LocalBroadcastHelper.sendLogMessege(
            this, "Uploading paused"
        )
        L.print(this, "TAG Service pausing.....")
    }

    fun resumeUploadService() {
        //isPaused = false
        this.uploadProperty = UploadProperty()
        this.uploadProperty.fromPreferences(this)
        FileUploader.instance.isDelete = uploadProperty.isDelete
        L.print(this, "TAG isDelete updateProgressBar - ${FileUploader.instance.isDelete}")
        //uploadProperty.itemCounter++
        L.print(this, "TAG counter - ${uploadProperty.itemCounter}")

        LocalBroadcastHelper.sendStartFileLoading(
            this, uploadProperty.fileTypes,
            uploadProperty.albumName, uploadProperty.itemCounter, uploadProperty.loadedSize
        )
        uploadProperty.isUploading = true

        GlobalScope.async{
            delay(1000)
            try {

                startUpload(uploadProperty, true)
            } catch (ex: Exception) {}
        }

        LocalBroadcastHelper.sendLogMessege(
            this, "Resumed upload"
        )
    }

    fun stopUpload() {
     //   OkHttpClient.Builder().build().dispatcher.cancelAll()
        LocalBroadcastHelper.sendCancelFileLoading(
            this, uploadProperty.fileTypes,
            uploadProperty.albumName, uploadProperty.itemCounter
        )
        photoUploader.stopUpload()
        notificationHelper.finishNotification()
        executorService.shutdownNow()
        statusService.shutdownNow()
        this.stopSelf()
        Log.d("UploadFilesViewModel","UploadMultipleFilesService stopUpload 3344")
        isStarted = false
        L.print(this, "Stop uploading")
        allFiles.clear()

        LocalBroadcastHelper.sendProgressFileLoading(
            this, uploadProperty.fileTypes,
            uploadProperty.albumName, 0, 0, 0, 0, 0
        )
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putInt(Constants.CURRENT_LOAD_ITEM, 0).apply()
        uploadProperty = UploadProperty()
        uploadProperty.toPreferences(this)
        //uploadProperty.isUploading = false
        //uploadProperty.isCompressionAllowed = false
        //uploadProperty.itemCounter = 0

        notificationHelper.finishNotification()
        uploadProperty.isUploading = false
        Thread.sleep(2000)
        LocalBroadcastHelper.sendNewDownloadStatus(
            this@UploadMultipleFilesService,
            "cancel", true
        )
        LocalBroadcastHelper.sendStoppedLoading(this@UploadMultipleFilesService)

    }

    fun isStarted():Boolean{
        return isStarted
    }

    override fun onBind(p0: Intent?): IBinder? {
        return myBinder
    }

    val handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            msg?.let {
                when (msg.arg1) {
                    DELETE_FLAG -> removeFile(msg.obj as String)
                    INSERT_FLAG -> addFile(msg.obj as String)
                    else -> updateStatus(msg.obj as String, msg.arg1)
                }
            }
        }
    }

    private fun updateStatus(filePath: String, percent: Int) {
        val fileStatus = filesQueue[filePath]
        fileStatus?.let { status ->
            status.progress = percent
            filesQueue.put(filePath, status)
        }
        Log.d("UploadFilesViewModel","uplodmulfileservice updateStatus file - $filePath progress - $percent")

        L.print(this, "file - $filePath progress - $percent")
    }

    @Synchronized
    private fun removeFile(filePath: String) {
        val file = File(filePath)
        uploadProperty.loadedSize += file.length()
        uploadProperty.itemCounter++
        uploadProperty.toPreferences(this)
        L.print(this, "$filePath FINISHED")
        filesQueue.remove(filePath)
        filesToUpload.remove(filePath)

        if(uploadProperty.isDelete){
            PhotoGalleryManager.deleteOneFile(applicationContext, file)
        }

        L.print(this, "to upload: $filesToUpload")
        if (filesQueue.size == 0) {

            val resultIntent = Intent(this, FotkiTabActivity::class.java)
            notificationHelper.sendNotificationNoizy(
                "Uploading finished",
                resultIntent,
                "Your photos upload successfully finished!"
            )

            isStarted = false
            statusService.shutdownNow()
            executorService.shutdownNow()
            uploadProperty.isUploading = false
            uploadProperty.isFinished = true
            Log.d("UploadFilesViewModel","uplodmulfileservice remove file")
            LocalBroadcastHelper.sendNewDownloadStatus(
                this@UploadMultipleFilesService,
                "", true
            )
        } else {
            loadNext()
        }
    }

    private fun loadNext() {
        if(filesToUpload.size>0){
            val file = nextUploadFile()
            file?.let {
                if (!filesQueue.containsKey(file.mFilePath)) {
                    filesQueue[file.mFilePath] = FileStatus(it)
                }
            }
            L.print(this,"load next - ${file?.mFilePath}")
            if(!executorService.isShutdown){
                try{
                    executorService.submit {
                        file?.let {
                            //uploadFakeFile(file)
                            Log.d("UploadFilesViewModel","uplodmulfileservice load next")

                            uploadFile(file)
                        }
                    }
                } catch (ex:Exception){ }
            }
        }
    }

    private fun uploadFile(file: FileType) {
        Log.d("UploadFilesViewModel","uplodmulfileservice uploadFileeee")
        photoUploader.uploadImage(file,uploadProperty,this, handler)
    }

    @Synchronized
    fun nextUploadFile()= filesToUpload.remove(filesToUpload.keys.first())

    @Synchronized
    private fun addFile(filePath: String) {
        L.print(this, "$filePath STARTED")

        L.print(this, "filesQueue - $filesQueue")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        L.print(this,"service on start command")
        Log.d("UploadFilesViewModel","UploadMultipleFilesService onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d("UploadFilesViewModel","UploadMultipleFilesService onDestroy")

        L.print(this,"service Upload destroyed")
        try{
            uploadProperty.isUploading = false
            uploadProperty.toPreferences(this)
        } catch (ex:Exception){}
        super.onDestroy()
        L.print(this,"service Destroyed upload")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        uploadProperty.isUploading = false
        uploadProperty.toPreferences(this)
        L.print(this,"service removed task")
        super.onTaskRemoved(rootIntent)
        L.print(this,"service removed task after")
    }

    companion object {
        const val DELETE_FLAG = -1
        const val INSERT_FLAG = -2
    }
}