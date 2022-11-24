package com.tbox.fotki.refactoring.service

import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.util.*
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.util.sync_files.LogProvider
import com.tbox.fotki.util.sync_files.MediaSyncronizator
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.util.upload_files.AndroidMultiPartEntity
import com.tbox.fotki.util.upload_files.FileUploader
import com.tbox.fotki.util.upload_files.ImageHelper
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import okio.source
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ProtocolException
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UploadThreadService : Service() {

    private var uploadProperty = UploadProperty()
    private var sourceFile: File? = null
    private var compressedImage: File? = null
    private var totalSize: Long = 0
    private var mRetryHitCount: Int = 0

    lateinit var notificationHelper: NotificationHelperNew
    private var executionService = Executors.newFixedThreadPool(1)
    private var currentSize = 0L
    private var isPaused = false

    private val photoUploader = PhotoUploader()
    //private val audioApi: AudioApi by inject()
    private val okHttpClient: OkHttpClient by inject()

    private val myBinder = MyLocalBinder()
    override fun onCreate() {
        super.onCreate()
        L.print(this, "OnCreate UploadThreadService")
    }

    override fun onDestroy() {
        super.onDestroy()
        L.print(this, "TAG Destroy!!!")
        executionService.shutdownNow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationHelper.finishNotification()
            try {
                if (uploadProperty.isUploading){
                    stopUpload()
                    photoUploader.stopUpload()
                }

            } catch (ex: Exception) {

            }
        }

        LocalBroadcastHelper.sendLogMessege(
            this@UploadThreadService, "Destroyed service"
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        uploadProperty.toPreferences(this)

        LocalBroadcastHelper.sendLogMessege(
            this@UploadThreadService, "Low memory"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        L.print(this, "MEDIA RESTART SERVICE!!!!")
        notificationHelper =
            NotificationHelperNew(baseContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                (Math.random() * 10000).toInt(),
                notificationHelper.getNotification(
                    "Service started",
                    null,
                    "Service started",
                    false
                )
            )
        }


        LogProvider.writeToFile(LogProvider.RESTART_UPLOAD_SERVICE, baseContext)
        LocalBroadcastHelper.sendLogMessege(
            this@UploadThreadService, "Restart service"
        )

        val property = intent?.getParcelableExtra<UploadProperty>(EXTRA_PROPERTY)
        if (property != null) {
            L.print(this, "MEDIA $property")
            this.uploadProperty = property
        }

        LogProvider.writeToFile(
            "${LogProvider.SERVICE_WITH_PROPERTY} $uploadProperty", baseContext
        )
        L.print(this, "MEDIA property - $property sourceFile - $sourceFile")
        if (uploadProperty.fileTypes.size > 0) {
            sourceFile = File(uploadProperty.fileTypes[0].mFilePath)
            startUpload(uploadProperty)
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        notificationHelper = NotificationHelperNew(this)
        return myBinder
    }

    fun startUpload(
        property: UploadProperty
    ) {
        LogProvider.writeToFile(
            LogProvider.PREPARE_AND_LOAD_FIRST_FILE, baseContext
        )
        this.uploadProperty = property
        LocalBroadcastHelper.sendStartFileLoading(
            this@UploadThreadService, uploadProperty.fileTypes,
            if (uploadProperty.albumName.isEmpty())
                uploadProperty.albumName else uploadProperty.fileTypes[0].albumName,
            uploadProperty.itemCounter, uploadProperty.loadedSize
        )
        LocalBroadcastHelper.sendLogMessege(
            this@UploadThreadService, "Uploading started"
        )
        uploadProperty.isUploading = true
        uploadProperty.isFinished = false
        isPaused = false
        startServiceUpload()
    }


    fun pauseUploadService() {
        uploadProperty.isUploading = false
        isPaused = true
        notificationHelper.finishNotification()
        executionService.shutdownNow()
        uploadProperty.isUploading = false
        uploadProperty.toPreferences(this)

        LocalBroadcastHelper.sendLogMessege(
            this@UploadThreadService, "Uploading paused"
        )
        L.print(this, "TAG Service pausing.....")
    }

    fun resumeUploadService() {
        isPaused = false
        this.uploadProperty = UploadProperty()
        this.uploadProperty.fromPreferences(this)
        FileUploader.instance.isDelete = uploadProperty.isDelete
        L.print(this, "TAG isDelete updateProgressBar - ${FileUploader.instance.isDelete}")
        //uploadProperty.itemCounter++
        L.print(this, "TAG counter - ${uploadProperty.itemCounter}")

        LocalBroadcastHelper.sendStartFileLoading(
            this@UploadThreadService, uploadProperty.fileTypes,
            uploadProperty.albumName, uploadProperty.itemCounter, uploadProperty.loadedSize
        )
        uploadProperty.isUploading = true
        executionService = Executors.newFixedThreadPool(1)
        executionService.submit(callable)

        LocalBroadcastHelper.sendLogMessege(
            this@UploadThreadService, "Resumed upload"
        )
    }

    val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            onPostExecute(msg!!.obj as String)
        }
    }

    private var callable = Callable {
       val message = Message()
        message.obj = uploadImage()
        L.print(this,"message - ${message.obj}")
        handler.sendMessage(message)
        //uploadRetrofit()
    }

    private fun startServiceUpload() {
        L.print(this, "TAG isPaused - $isPaused")
        LogProvider.writeToFile(
            "${LogProvider.IS_PAUSED_UPLOAD_THREAD} $isPaused", baseContext
        )

        if (executionService.isShutdown) {
            if (isPaused) {
                isPaused = false
                return
            } else executionService = Executors.newFixedThreadPool(1)
        }
        notifyChanges(uploadProperty.itemCounter, 0)
        try {
            executionService.submit(callable)
        } catch (ex: Exception) {
        }
    }

    fun stopUpload() {
        isPaused = true
        LocalBroadcastHelper.sendProgressFileLoading(
            this@UploadThreadService, uploadProperty.fileTypes,
            uploadProperty.albumName, 0, 0, 0, 0, 0
        )
        PreferenceManager.getDefaultSharedPreferences(this@UploadThreadService)
            .edit().putInt(Constants.CURRENT_LOAD_ITEM, 0).apply()
        uploadProperty.isUploading = false
        uploadProperty.isCompressionAllowed = false
        LocalBroadcastHelper.sendCancelFileLoading(
            this@UploadThreadService,
            uploadProperty.fileTypes, uploadProperty.albumName, uploadProperty.itemCounter
        )
        notificationHelper.finishNotification()
        executionService.shutdownNow()
        uploadProperty.isUploading = false
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): UploadThreadService {
            return this@UploadThreadService
        }
    }

    private var numPrev = 0L
    private var sendedBytesCounterLong = 0L
    private var sendedBytesCounterShort = 0L


    suspend fun tryToRestart() {
        delay(60000)
        //TimeUnit.SECONDS.sleep(60)
        if (uploadProperty.albumId > 0)
            startServiceUpload()
    }

    fun uploadImage(): String? {
        sourceFile = File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
        val mediaType = uploadProperty.fileTypes[uploadProperty.itemCounter].mFileMimeType
        try {
            val sourceImageFile = uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
            var sourceFile = File(sourceImageFile)

            L.print(this, "File...::::" + sourceFile + " : " + sourceFile.exists())

            val MEDIA_TYPE = if (sourceImageFile.endsWith("png")) {
                "image/png".toMediaTypeOrNull()
            } /*else if (sourceImageFile.endsWith("mp4")){
                MediaType.parse("video/mp4")
            }*/ else {
                "image/jpeg".toMediaTypeOrNull()
            }
            L.print(this, "before - ${sourceFile.length()}")

            if (uploadProperty.isCompressionAllowed && mediaType.startsWith("image")) {
                L.print(this, "Try to compress - ${sourceFile.name} media type - $mediaType")
                sourceFile =
                    ImageHelper.compress(this@UploadThreadService, sourceFile) ?: sourceFile
            }
            L.print(this, "after - ${sourceFile.length()}")

            val body = createCustomRequestBody(MEDIA_TYPE!!, sourceFile)

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "photo",
                    sourceImageFile,
                    body
                )
                .addFormDataPart(Constants.SESSION_ID, uploadProperty.sessionId)

            requestBodyBuilder.addFormDataPart(
                "album_id_enc",
                if (uploadProperty.albumId == 0L) {
                    uploadProperty.fileTypes[uploadProperty.itemCounter].albumId.toString()
                } else {
                    uploadProperty.albumId.toString()
                }
            )

            val requestBody = requestBodyBuilder.build()
            val request = Request.Builder()
                .url(Constants.BASE_URL + Constants.FILE_UPLOAD)
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            L.print(this, "res - $response")
            return response.body?.string()

        } catch (e: IOException) {
            if (e is UnknownHostException) {
                sendErrorLog("No Internet connection!!!")
                sendErrorLog("Network connection failed. Try to restart.")
                LocalBroadcastHelper.sendServiceError(
                    this@UploadThreadService,
                    "Network connection failed. Try to restart."
                )
                return e.toString()

            } else if (e is ProtocolException){
                return "{}"
            } else {
                FirebaseCrashlytics.getInstance().recordException(e)
//                Crashlytics.logException(e)
                return e.toString()
            }


        } catch (e: UnsupportedEncodingException) {
            uploadProperty.toPreferences(this)
            GlobalScope.async{ tryToRestart() }
/*
            Crashlytics.logException(e)
            TimeUnit.SECONDS.sleep(60)

            if (uploadProperty.albumId > 0)
                startServiceUpload()
*/
            return e.toString()
        } catch (e: Exception) {
            L.print(this, "Other Error: " + e.localizedMessage)
        }
        return null
    }

    val progressHandler = object:Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            //publishProgress()
            publishProgress(
                msg.arg1, //percent,
                msg.arg2 //uploadProperty.itemCounter
            )
        }
    }

    private fun createCustomRequestBody(contentType: MediaType, file: File): RequestBody {
        return object : RequestBody() {
            override fun contentType() = contentType
            override fun contentLength() = file.length()
            override fun writeTo(sink: BufferedSink) {
                try {
                    val source = file.source()
                    //sink.writeAll(source);
                    val buf = Buffer()
                    var remaining = contentLength()
                    totalSize = contentLength()
                    var readCount = 0L
                    var num = 0L

                    readCount = source.read(buf, 1024 * 16)
                    var times = 0
                    while ((readCount != -1L)) {

                        sink.write(buf, readCount)
                        remaining -= readCount
                        num += readCount
                        L.print(
                            this,
                            "TAG progress - $sendedBytesCounterLong : $sendedBytesCounterShort" +
                                    " numPrev - $numPrev num - $num"
                        )
                        if ((num - numPrev) > 0) {
                            sendedBytesCounterLong += num - numPrev
                            sendedBytesCounterShort += num - numPrev
                        }
                        val percent = (num.toFloat() / contentLength().toFloat() * 100).toInt()
                        L.print(
                            this,
                            "PUBLISH PROGRESS - $percent numPrev - $numPrev counter length - ${contentLength()}"
                        )
                        if (times == 30) {
                            times = 0
                            val message = Message()
                            message.arg1 = percent
                            message.arg2 = uploadProperty.itemCounter
                            progressHandler.sendMessage(message)
                            TimeUnit.SECONDS.sleep(1)
                        } else {

                            times++
                        }
/*
                        publishProgress(
                            percent,
                            uploadProperty.itemCounter
                        )
*/
                        numPrev = num

                        if (Thread.currentThread().isInterrupted) {
                            throw RuntimeException()
                        }

                        L.print(
                            this,
                            "source size: " + contentLength() + " remaining bytes: " + remaining
                        )
                        readCount = source.read(buf, 1024 * 16)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onPostExecute(result: String?) {
        if(result == null) return
        
        try {

            L.print(this, "MEDIA_TAG response - $result")
            val jsonResponse = JSONObject(result)
            if (jsonResponse.has(Constants.OK)) {
                LocalBroadcastHelper.sendLogMessege(
                    this@UploadThreadService, "Upload file - ${sourceFile!!.absolutePath} finished"
                )
                if (jsonResponse.getInt(Constants.OK) == 1) {
                    mRetryHitCount = 0
                    LogProvider.writeToFile(
                        "Upload file - ${sourceFile!!.absolutePath} finished",
                        baseContext
                    )
                    if (uploadProperty.albumId == 0L) {
                        try {
                            MediaSyncronizator(baseContext).updateSuccessUploaded(
                                uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
                            )
                            LocalBroadcastHelper.sendProgressFileSync(
                                baseContext
                            )
                        } catch (ex: Exception) {
                        }
                    }
                    sourceFile?.let {
                        if(uploadProperty.isDelete){
                            deleteOneFile(it)
                        }
                    }
                    nextUpload()
                } else {
                    LogProvider.writeToFile(
                        "Upload file - ${sourceFile!!.absolutePath} " +
                                "finished with error $result", baseContext
                    )
                    if (mRetryHitCount < 3) {
                        try {
                            if (jsonResponse.getString(Constants.API_MESSAGE) == this@UploadThreadService.getString(
                                    R.string.wrong_setting_id_message
                                )
                            ) {
                                uploadProperty.isUploading = false
                                uploadProperty.isCompressionAllowed = false
                                LocalBroadcastHelper.sendSessionExpired(this@UploadThreadService)
                                notificationHelper.finishNotification()
                                LogProvider.writeToFile("Session expired!", baseContext)
                            } else {
                                mRetryHitCount++
/*                            uploadingTask = FileUploadingTask()
                            uploadingTask.execute()*/
                                startServiceUpload()
                            }
                        } catch (e: JSONException) {
                            FirebaseCrashlytics.getInstance().recordException(e)
//                            Crashlytics.logException(e)
                            e.printStackTrace()
                            sendErrorLog(e.toString())
                        }

                    } else {
                        mRetryHitCount = 0
                        if (uploadProperty.albumId == 0L) {

                            if (!PreferenceHelper(baseContext).getBoolean(BackupProperties.PREFERENCE_IS_BACKUP_STARTED)) {
                                //PreferenceHelper(baseContext).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
                                return
                            }

                            try {
                                MediaSyncronizator(baseContext).updateSuccessUploaded(
                                    uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
                                )
                                LocalBroadcastHelper.sendProgressFileSync(
                                    baseContext
                                )
                            } catch (ex: Exception) {
                            }
                        }
                        uploadProperty.itemCounter += 1
                        if (uploadProperty.itemCounter < uploadProperty.fileTypes.size) {
                            startServiceUpload()
                        }
                    }
                }
            } else {
                notificationHelper.finishNotification()
                LocalBroadcastHelper.sendProgressFileSync(this, false)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            TimeUnit.SECONDS.sleep(5)
            if (uploadProperty.albumId > 0L)
                startServiceUpload()
        }
    }

    private fun nextUpload() {
        L.print(this, "Try to next upload")
        if ((!PreferenceHelper(baseContext).getBoolean(BackupProperties.PREFERENCE_IS_BACKUP_ENABLE)
                    || !PreferenceHelper(baseContext).getBoolean(BackupProperties.PREFERENCE_IS_BACKUP_STARTED))
            && uploadProperty.albumId == 0L
        ) {
            LocalBroadcastHelper.sendFinishedFileLoading(
                this@UploadThreadService,
                uploadProperty.fileTypes,
                uploadProperty.albumName,
                uploadProperty.itemCounter,
                uploadProperty.loadedSize
            )
            notificationHelper.finishNotification()
            return
        }

        uploadProperty.itemCounter += 1
        uploadProperty.loadedSize += currentSize

        if (uploadProperty.itemCounter < uploadProperty.fileTypes.size) {
            uploadProperty.toPreferences(this)
            LocalBroadcastHelper.sendLogMessege(
                this@UploadThreadService,
                "Upload file ${uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath}"
            )
            startServiceUpload()
        } else {
            PreferenceManager.getDefaultSharedPreferences(this@UploadThreadService)
                .edit().putInt(Constants.CURRENT_LOAD_ITEM, 0).apply()
            uploadProperty.isUploading = false
            uploadProperty.isCompressionAllowed = false
            LocalBroadcastHelper.sendFinishedFileLoading(
                this@UploadThreadService,
                uploadProperty.fileTypes,
                uploadProperty.albumName,
                uploadProperty.itemCounter,
                uploadProperty.loadedSize
            )
            uploadProperty.loadedSize = 0L
            notificationHelper.finishNotification()
            uploadProperty.isUploading = false
            uploadProperty.isFinished = true
            uploadProperty.toPreferences(this)
            val resultIntent = Intent(this, FotkiTabActivity::class.java)
            if (uploadProperty.albumId != 0L)
                notificationHelper.sendNotificationNoizy(
                    "Uploading finished",
                    resultIntent,
                    "Your photos upload successfully finished!"
                )
            else
                sendBackupProgressNotification()
        }
    }

    private fun sendBackupProgressNotification() {
        GlobalScope.launch(Dispatchers.IO) {
            val notSyncCount = MediaSyncronizator(baseContext).getNotSyncFiles().size
            if (notSyncCount > 0)
                notificationHelper.sendNotification(
                    "Backup in progress", null,
                    "Not synchronized - $notSyncCount photos. Part of it has been successfully uploaded."
                )
        }
    }

    private fun publishProgress(vararg progress: Int?) {
        LocalBroadcastHelper.sendProgressFileLoading(
            this@UploadThreadService, uploadProperty.fileTypes,
            uploadProperty.albumName, progress[1]!!, progress[0]!!,
            uploadProperty.loadedSize, progress[2]!!, progress[3]!!
        )
        //notifyChanges(progress[0], progress[1])
    }

    private fun sendErrorLog(s: String) {
        if (uploadProperty.albumId != 0L) {
            val resultIntent = Intent(this, FotkiTabActivity::class.java)
            notificationHelper.sendNotification("Upload failed", resultIntent, "Failed because: $s")
            L.print(this, "TAG Error - $s")
            LogProvider.writeToFile("${LogProvider.UPLOAD_FAILED} $s", baseContext)
            LocalBroadcastHelper.sendLogMessege(
                this@UploadThreadService, "Upload failed. Failed because: $s"
            )
        }
    }

    private fun sendError(s: String) {
        CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL, s)
        LocalBroadcastHelper.sendServiceError(this@UploadThreadService, s)
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(Constants.CURRENT_LOAD_ITEM, uploadProperty.itemCounter).apply()
        uploadProperty.isUploading = false
    }

    private fun startNotification() {
        val resultIntent = Intent(this, FotkiTabActivity::class.java)
        notificationHelper.sendNotification(
            "Start uploading",
            resultIntent,
            "Your next photo begin to upload"
        )
    }

    private fun notifyChanges(i: Int?, num: Int?) {
        val resultIntent = Intent(this, FotkiTabActivity::class.java)
        if (uploadProperty.albumId == 0L) {
            notificationHelper.sendNotification(
                "Backup upload in progress", resultIntent,
                "Uploading part of photo for backup. Uploaded - $i / ${uploadProperty.fileTypes.size} photo"
            )
        } else {
            notificationHelper.sendNotification(
                "Upload in progress", resultIntent,
                "Uploaded - $i / ${uploadProperty.fileTypes.size} photo"
            )
        }
    }

    private fun deleteOneFile(file: File) {
        currentSize = file.length()
        if (file.exists()) {
            if (file.delete()) {
                Log.e("-->", "file Deleted :" + file.absolutePath)
                refreshGallery(file)
            } else {
                Log.e("-->", "file not Deleted :" + file.absolutePath)
            }
        }
    }

    private fun refreshGallery(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Write Kitkat version specific code for add entry to gallery database
            // Check for file existence
            if (file.exists()) {
                // Add / Move File
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(file)
                mediaScanIntent.data = contentUri
                applicationContext.sendBroadcast(mediaScanIntent)
            } else {
                // Delete File
                try {
                    applicationContext.contentResolver.delete(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA + "='"
                                + file.path + "'", null
                    )
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
//                    Crashlytics.logException(e)
                    sendErrorLog("Error while deleting - $e ")
                    //e.printStackTrace()
                }
            }
        } else {
            applicationContext.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_MOUNTED, Uri.parse(
                        "file://" + file.parentFile.absolutePath
                    )
                )
            )
        }
    }

    private fun callBroadCast() {
        if (Build.VERSION.SDK_INT >= 14) {
            Log.e("-->", " >= 14")
            MediaScannerConnection.scanFile(
                this,
                arrayOf(Environment.getExternalStorageDirectory().toString()), null
            ) { p0, p1 -> Log.d("TAG", "completed - $p0  $p1") }
        } else {
            Log.e("-->", " < 14")
            sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())
                )
            )
        }
    }


    companion object {
        val EXTRA_PROPERTY = "extra_property"
    }

}