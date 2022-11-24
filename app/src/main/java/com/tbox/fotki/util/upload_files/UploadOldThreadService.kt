package com.tbox.fotki.util.upload_files

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
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UploadOldThreadService : Service() {

    private var uploadProperty = UploadProperty()
    private var sourceFile: File? = null
    private var compressedImage: File? = null
    private var totalSize: Long = 0
    private var mRetryHitCount: Int = 0

    var isCompressionAllow = false
    lateinit var notificationHelper: NotificationHelperNew
    private var executionService = Executors.newFixedThreadPool(1)
    private var currentSize = 0L
    private var isPaused = false


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
                if (uploadProperty.isUploading)
                    stopUpload()
            } catch (ex: Exception) {

            }
        }

        LocalBroadcastHelper.sendLogMessege(
            this@UploadOldThreadService, "Destroyed service"
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        uploadProperty.toPreferences(this)

        LocalBroadcastHelper.sendLogMessege(
            this@UploadOldThreadService, "Low memory"
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
            this@UploadOldThreadService, "Restart service"
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
            this@UploadOldThreadService, uploadProperty.fileTypes,
            if (uploadProperty.albumName.isEmpty())
                uploadProperty.albumName else uploadProperty.fileTypes[0].albumName,
            uploadProperty.itemCounter, uploadProperty.loadedSize
        )
        LocalBroadcastHelper.sendLogMessege(
            this@UploadOldThreadService, "Uploading started"
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
            this@UploadOldThreadService, "Uploading paused"
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
            this@UploadOldThreadService, uploadProperty.fileTypes,
            uploadProperty.albumName, uploadProperty.itemCounter, uploadProperty.loadedSize
        )
        uploadProperty.isUploading = true
        executionService = Executors.newFixedThreadPool(1)
        executionService.submit(callable)

        LocalBroadcastHelper.sendLogMessege(
            this@UploadOldThreadService, "Resumed upload"
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
            this@UploadOldThreadService, uploadProperty.fileTypes,
            uploadProperty.albumName, 0, 0, 0, 0, 0
        )
        PreferenceManager.getDefaultSharedPreferences(this@UploadOldThreadService)
            .edit().putInt(Constants.CURRENT_LOAD_ITEM, 0).apply()
        uploadProperty.isUploading = false
        isCompressionAllow = false
        LocalBroadcastHelper.sendCancelFileLoading(
            this@UploadOldThreadService,
            uploadProperty.fileTypes, uploadProperty.albumName, uploadProperty.itemCounter
        )
        notificationHelper.finishNotification()
        executionService.shutdownNow()
        uploadProperty.isUploading = false
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): UploadOldThreadService {
            return this@UploadOldThreadService
        }
    }

    private var numPrev = 0L
    private var sendedBytesCounterLong = 0L
    private var sendedBytesCounterShort = 0L

/*
    private fun uploadFile(): String? {
        LogProvider.writeToFile(
            "begin to upload - " +
                    "${uploadProperty.fileTypes[uploadProperty.itemCounter]}"
            , baseContext
        )
        L.print(
            this,
            "MEDIA begin to upload - ${uploadProperty.fileTypes[uploadProperty.itemCounter]}"
        )

        var responseString: String? = null
        val httpclient = DefaultHttpClient()
        val httppost = HttpPost(Constants.BASE_URL + Constants.FILE_UPLOAD)

        try {
            httppost.entity = createEntity(AndroidMultiPartEntity.ProgressListener { num ->

                L.print(
                    this,
                    "TAG progress - $sendedBytesCounterLong : $sendedBytesCounterShort" +
                            " numPrev - $numPrev num - $num"
                )
                if ((num - numPrev) > 0) {
                    sendedBytesCounterLong += num - numPrev
                    sendedBytesCounterShort += num - numPrev
                }
                publishProgress(
                    (num / totalSize.toFloat() * 100).toInt(),
                    uploadProperty.itemCounter
                )
                numPrev = num

                if (Thread.currentThread().isInterrupted) {
                    throw RuntimeException()
                }

            })

            if (httppost.entity == null) {
                if (uploadProperty.albumId == 0L) {
                    MediaSyncronizator(baseContext).updateDeletedUpload(
                        uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
                    )
                }
                nextUpload()
            }
            // Making server call
            val response = httpclient.execute(httppost)
            val r_entity = response.entity

            val statusCode = response.statusLine.statusCode
            responseString = if (statusCode == 200) {
                // Server response
                if (uploadProperty.isDelete) {
                    deleteOneFile(sourceFile!!)
                    L.print(this, "LOG UPLOAD AND DELETE - $sourceFile")
                } else
                    L.print(this, "LOG UPLOAD - $sourceFile")

                EntityUtils.toString(r_entity)


            } else {
                sendError("Error occurred! Http Status Code: $statusCode")
                "Error occurred! Http Status Code: $statusCode"
            }

        } catch (e: ClientProtocolException) {
            responseString = e.toString()
            uploadProperty.toPreferences(this)

            Crashlytics.logException(e)
            TimeUnit.SECONDS.sleep(60)

            if (uploadProperty.albumId > 0)
                startServiceUpload()

        } catch (e: IOException) {
            if (e is UnknownHostException)
                sendErrorLog("No Internet connection!!!")
            else
                Crashlytics.logException(e)
            responseString = e.toString()
            sendErrorLog("Network connection failed. Try to restart.")
            LocalBroadcastHelper.sendServiceError(
                this@UploadThreadService,
                "Network connection failed. Try to restart."
            )
        }

        return responseString
    }*/
/*
    private fun uploadRetrofit() {
        val sourceImageFile = uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
        sourceFile = File(sourceImageFile)

        if (sourceFile == null) {
            return
        }
        GlobalScope.launch(Dispatchers.Main) {

        sourceFile?.let { file ->
            val mediaType = if (sourceImageFile.endsWith("png")) {
                "image/png"
            } else {
                "image/jpeg"
            }

            val filePart =
                MultipartBody.Part.createFormData(
                    "photo",
                    file.name,
                    file.toRequestBody(MediaType.get(mediaType)).counting(object :
                        OnByteWrittenListener {
                        override fun onByteWritten(byteWritten: Long, contentLength: Long) {
                            //byteWritten.toFloat() / contentLength.toFloat()
                            //100 * (byteWritten.toFloat() / contentLength.toFloat())
                            L.print(
                                this, "status - " +
                                        (100 * (byteWritten.toFloat() / contentLength.toFloat())).toString()
                            )

                            L.print(
                                this,
                                "TAG progress - $sendedBytesCounterLong : $sendedBytesCounterShort" +
                                        " numPrev - $numPrev num - $byteWritten"
                            )
                            if ((byteWritten - numPrev) > 0) {
                                sendedBytesCounterLong += byteWritten - numPrev
                                sendedBytesCounterShort += byteWritten - numPrev
                            }
                            publishProgress(
                                100 * (byteWritten.toFloat() / contentLength.toFloat()).toInt(),
                                uploadProperty.itemCounter
                            )
                            numPrev = byteWritten

                            if (Thread.currentThread().isInterrupted) {
                                throw RuntimeException()
                            }
                        }
                    })
                )


            filePart.toString()

            val album = if (uploadProperty.albumId == 0L) {
                uploadProperty.fileTypes[uploadProperty.itemCounter].albumId.toString()
            } else {
                uploadProperty.albumId.toString()
            }
            val session = uploadProperty.sessionId


            val baseUrl = BuildConfig.API_PROD
            val url = "upload"
            L.print(this,"Url - $url")

            val retrofit =
                Retrofit.Builder().client(okHttpClient).baseUrl(baseUrl)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            val sessionRequest: RequestBody =
                RequestBody.create(MediaType.parse("text/plain"), session)
            val albumRequest: RequestBody =
                RequestBody.create(MediaType.parse("text/plain"), album)

            val audioApi = retrofit.create(AudioApi::class.java)
            val res = audioApi.uploadAudioAsync(url,albumRequest, sessionRequest, filePart).enqueue(object : Callback<String>{
                override fun onFailure(call: Call<String>, t: Throwable) {
                    L.print(this, "FAILURE - $t $call")
                    uploadProperty.toPreferences(this@UploadThreadService)

                    Crashlytics.logException(t)
                    GlobalScope.async{ tryToRestart() }

                    //return e.toString()
                }

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    val message = Message()
                    L.print(this,"resp - ${response.body().toString()}")
                    //onPostExecute(response.body().toString())
                    message.obj = response.body().toString()
                    //uploadProperty.itemCounter++
                    handler.sendMessage(message)
                }
            })

                */
/*.enqueue(object : Callback<JsonObject> {
                    override fun onFailure(t: Throwable) {
                        L.print(this, "FAILURE - $t")
                    }

                    override fun onResponse(
                        call: Call<JsonObject>,
                        response: Response<JSONObject>
                    ) {
                        if (response.isSuccessful) {
                            L.print(this,"SUCCESS")
                            //stateLiveData.postValue(FinishUpload)
                            if (response.body() != null) {
                                *//*
*/
/*val code = response.body()!!.getSafeString("error_code")
                                val message = response.body()!!.getSafeString("error_string")
                                if (code == "100") {
                                    stateLiveData.postValue(ErrorUpload(message))
                                } else {
                                    stateLiveData.postValue(FinishUpload)
                                }*//*
*/
/*

                                L.print(this,"response - ${response.body().toString()}")
                                val message = Message()
                                message.obj = response.body().toString()
                                handler.sendMessage(message)
                            } else {
                                L.print(this,"response - null")
                                //stateLiveData.postValue(ErrorUpload("Something was wrong. Please try again"))
                            }
                        } else {
                            L.print(this,"ERROR")
                        }
                    }
                })*//*


            L.print(this,"res - $res")
             }
        }
    }
*/

    suspend fun tryToRestart() {
        delay(20000)
        //TimeUnit.SECONDS.sleep(60)
        if (uploadProperty.albumId > 0)
            startServiceUpload()
    }

    fun uploadImage(): String? {
        sourceFile = File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
        try {
            val sourceImageFile = uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
            var sourceFile = File(sourceImageFile)

            L.print(this, "File...::::" + sourceFile + " : " + sourceFile.exists())

            val MEDIA_TYPE = if (sourceImageFile.endsWith("png")) {
                "image/png".toMediaTypeOrNull()
            } else {
                "image/jpeg".toMediaTypeOrNull()
            }

            if (isCompressionAllow) {
                sourceFile =
                    ImageHelper.compress(this@UploadOldThreadService, sourceFile) ?: sourceFile
            }

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "photo",
                    sourceImageFile,
                    createCustomRequestBody(MEDIA_TYPE!!, sourceFile)
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
            return response.body?.string()

        } catch (e: IOException) {
            if (e is UnknownHostException)
                sendErrorLog("No Internet connection!!!")
            else
                FirebaseCrashlytics.getInstance().recordException(e)
//                Crashlytics.logException(e)
            sendErrorLog("Network connection failed. Try to restart.")
            LocalBroadcastHelper.sendServiceError(
                this@UploadOldThreadService,
                "Network connection failed. Try to restart."
            )
            return e.toString()
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

    fun createCustomRequestBody(contentType: MediaType, file: File): RequestBody {
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

                    readCount = source.read(buf, 2048)

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
                        publishProgress(
                            (num / contentLength() * 100).toInt(),
                            uploadProperty.itemCounter
                        )
                        numPrev = num

                        if (Thread.currentThread().isInterrupted) {
                            throw RuntimeException()
                        }

                        L.print(
                            this,
                            "source size: " + contentLength() + " remaining bytes: " + remaining
                        )
                        readCount = source.read(buf, 2048)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onPostExecute(result: String?) {
        try {

            L.print(this, "MEDIA_TAG response - $result")
            val jsonResponse = JSONObject(result)
            if (jsonResponse.has(Constants.OK)) {
                LocalBroadcastHelper.sendLogMessege(
                    this@UploadOldThreadService, "Upload file - ${sourceFile!!.absolutePath} finished"
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
                    nextUpload()
                } else {
                    LogProvider.writeToFile(
                        "Upload file - ${sourceFile!!.absolutePath} " +
                                "finished with error $result", baseContext
                    )
                    if (mRetryHitCount < 3) {
                        try {
                            if (jsonResponse.getString(Constants.API_MESSAGE) == this@UploadOldThreadService.getString(
                                    R.string.wrong_setting_id_message
                                )
                            ) {
                                uploadProperty.isUploading = false
                                isCompressionAllow = false
                                LocalBroadcastHelper.sendSessionExpired(this@UploadOldThreadService)
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
                            4

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
                this@UploadOldThreadService,
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
                this@UploadOldThreadService,
                "Upload file ${uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath}"
            )
            startServiceUpload()
        } else {
            PreferenceManager.getDefaultSharedPreferences(this@UploadOldThreadService)
                .edit().putInt(Constants.CURRENT_LOAD_ITEM, 0).apply()
            uploadProperty.isUploading = false
            isCompressionAllow = false
            LocalBroadcastHelper.sendFinishedFileLoading(
                this@UploadOldThreadService,
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
            this@UploadOldThreadService, uploadProperty.fileTypes,
            uploadProperty.albumName, progress[1]!!, progress[0]!!,
            uploadProperty.loadedSize, 0, 0
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
                this@UploadOldThreadService, "Upload failed. Failed because: $s"
            )
        }
    }

    private fun sendError(s: String) {
        CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL, s)
        LocalBroadcastHelper.sendServiceError(this@UploadOldThreadService, s)
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(Constants.CURRENT_LOAD_ITEM, uploadProperty.itemCounter).apply()
        uploadProperty.isUploading = false
    }

    private fun createEntity(progressListener: AndroidMultiPartEntity.ProgressListener): AndroidMultiPartEntity? {
        try {
            val entity = AndroidMultiPartEntity(progressListener)

            if (uploadProperty.fileTypes[uploadProperty.itemCounter].mFileMimeType == Constants.VIDEO ||
                uploadProperty.fileTypes[uploadProperty.itemCounter].mFileMimeType == Constants.GIF
            ) {
                sourceFile = File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
                entity.addPart("photo", FileBody(sourceFile!!))
            } else {
                if (isCompressionAllow) {
                    sourceFile =
                        File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
                    compressedImage = ImageHelper.compress(this@UploadOldThreadService, sourceFile)
                    if (compressedImage != null) {
                        entity.addPart("photo", FileBody(compressedImage!!))
                    } else {
                        entity.addPart("photo", FileBody(sourceFile!!))
                    }
                } else {
                    sourceFile =
                        File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
                    // Adding file data to http body
                    entity.addPart("photo", FileBody(sourceFile!!))
                }
            }
            // Extra parameters if you want to pass to server
            entity.addPart(
                Constants.SESSION_ID,
                StringBody(uploadProperty.sessionId)
            )
            L.print(this, "MEDIA album_id - ${uploadProperty.albumId}")
            L.print(
                this,
                "MEDIA from flie - ${uploadProperty.fileTypes[uploadProperty.itemCounter].albumId}"
            )
            if (uploadProperty.albumId == 0L)
                entity.addPart(
                    "album_id_enc",
                    StringBody(uploadProperty.fileTypes[uploadProperty.itemCounter].albumId.toString())
                )
            else
                entity.addPart("album_id_enc", StringBody(uploadProperty.albumId.toString()))

            totalSize = entity.contentLength
            return entity
        } catch (ex: FileNotFoundException) {
            return null
        }
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

    private fun getUploadedSize(currFileCount: Int): Any? {
        var count = 0L
        for (i in 0..currFileCount) count += File(uploadProperty.fileTypes[i].mFilePath).length()
        return count.toString()
    }

    fun deleteOneFile(file: File) {
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

    fun refreshGallery(file: File) {
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