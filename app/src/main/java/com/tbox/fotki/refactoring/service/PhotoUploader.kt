package com.tbox.fotki.refactoring.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simplemobiletools.commons.extensions.*
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.refactoring.screens.upload_files.UploadMultipleFilesService
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.LocalBroadcastHelper
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.util.upload_files.FileType
import com.tbox.fotki.util.upload_files.ImageHelper
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity.Companion.TAG
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ProtocolException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit


class PhotoUploader {

    private var numPrev = 0L
    private var sendedBytesCounterLong = 0L
    private var sendedBytesCounterShort = 0L
    private lateinit var client : OkHttpClient
    lateinit var TAG:String;
    var isCancel = false


    fun stopUpload() {
        Log.d("UploadFilesViewModel","PhotoUploader stopUpload"+client.dispatcher.runningCalls().size +"qued"+client.dispatcher.queuedCalls().size)
        client.dispatcher.cancelAll()
        isCancel = true
        for (call in client.dispatcher.queuedCalls()) {
                call.cancel()
            Log.d("UploadFilesViewModel","PhotoUploader queuedCalls")

        }
        for (call in client.dispatcher.runningCalls()) {
            Log.d("UploadFilesViewModel","PhotoUploader runningCalls")

            call.cancel()
        }
    }

    companion object {

        const val TYPE_FOTKI_RESIZE = "FotkiResize"
        const val TYPE_FOTKI_USUAL = "Fotki"
        lateinit var mActivity:Activity
        fun shareFile(activity: Activity, type: String) {
           mActivity = activity
            GlobalScope.launch {
                delay(2000)
                PreferenceHelper(activity).getString("shared_file_name")?.let { path ->
                    Log.d("shareError","PhotoUploader shareFile path "+path)
                    try {
//                        val intent = Intent(Intent.ACTION_SEND)
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                        intent.putExtra(Intent.EXTRA_STREAM, path)
//
//
//                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                        val resolvedInfoActivities: List<ResolveInfo> =
//                            activity.packageManager.queryIntentActivities(
//                                intent,
//                                PackageManager.MATCH_DEFAULT_ONLY
//                            )

//                        for (ri in resolvedInfoActivities) {
//                            activity.grantUriPermission(ri.activityInfo.packageName,
//                                Uri.parse(path), Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                        }
//                        intent.type = "image/*"
//                        activity.startActivity(Intent.createChooser(intent, "Share via"))


//                        val sharableIntent = Intent()
//                        sharableIntent.action = Intent.ACTION_SEND
//                        sharableIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        sharableIntent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//
//                        val imageUri = Uri.parse(path)
//                        val imageFile = File(imageUri.toString())
//                        val UriImage = FileProvider.getUriForFile(activity, "com.fotki.mobile.provider", imageFile)
//
//                        sharableIntent.type = "image/*"
//                        sharableIntent.putExtra(Intent.EXTRA_STREAM, UriImage)
//
//                        val chooser = Intent.createChooser(sharableIntent, "Chooser Title")
//                        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                        chooser.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//                        activity.startActivity(chooser)
                        activity.sharePathIntent(path, BuildConfig.APPLICATION_ID)

                    }catch (e:Exception){
                        Log.d("shareError","exception e "+e)
                    }

                }
            }
        }

        fun refreshGallery(file: File, applicationContext: Context) {
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
//                        Crashlytics.logException(e)
                        //sendErrorLog("Error while deleting - $e ")
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
    }

    fun uploadImage(fileType: FileType, uploadProperty: UploadProperty, context: Context, handler:Handler): String? {
        val mediaType = fileType.mFileMimeType
        TAG = fileType.mFilePath
        try {

            val sourceImageFile = fileType.mFilePath
            var sourceFile = File(sourceImageFile)

            L.print(this, "Upload - File...::::" + sourceFile + " : " + sourceFile.exists())

            val MEDIA_TYPE = when {
                sourceImageFile.endsWith("png") -> {
                    "image/png".toMediaTypeOrNull()
                }
                sourceImageFile.endsWith("mp4") -> {
                    "video/mp4".toMediaTypeOrNull()
                }
                else -> {
                    "image/jpeg".toMediaTypeOrNull()
                }
            }
            L.print(this, "Upload - before - ${sourceFile.length()} media type - $MEDIA_TYPE")

            if (uploadProperty.isCompressionAllowed && mediaType.startsWith("image")) {
                L.print(this, "Try to compress - ${sourceFile.name} media type - $mediaType")
                sourceFile = ImageHelper.compress(context, sourceFile) ?: sourceFile
                L.print(this, "Upload - After compress - ${sourceFile.length()}")
            }
            L.print(this, "Upload - after - ${sourceFile.length()} filename - $sourceImageFile")

            Log.d("UploadFilesViewModel"," photouploder uplod img before insert")

            val messageStart = Message()
            messageStart.arg1 = UploadMultipleFilesService.INSERT_FLAG
            messageStart.obj = fileType.mFilePath
            handler.sendMessage(messageStart)

            val body = createCustomRequestBody(MEDIA_TYPE!!, sourceFile, fileType, handler)

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
                .tag(TAG)
                .post(requestBody)
                .build()

            val clientBuilder = OkHttpClient.Builder()
            clientBuilder.retryOnConnectionFailure(true)
            client = clientBuilder.build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                L.print(this, "Error :(")
            }
            val messageEnd = Message()
            messageEnd.arg1 = UploadMultipleFilesService.DELETE_FLAG
            messageEnd.obj = fileType.mFilePath
            handler.sendMessage(messageEnd)
            Thread.sleep(2000)

            L.print(this, "Upload - res - $response")
            return response.body?.string()

        } catch (e: IOException) {

            if(isCancel)
                return ""
            L.print(this,"Error - IOException")
            TimeUnit.SECONDS.sleep(5)
            Log.d("UploadFilesViewModel", " photouploder $e catch  uplod img")
            uploadImage(fileType,uploadProperty,context,handler)

            if (e is UnknownHostException) {
                //sendErrorLog("No Internet connection!!!")
                //sendErrorLog("Network connection failed. Try to restart.")

                L.print(this,"Error - No Internet connection!!!")
                LocalBroadcastHelper.sendServiceError(
                    context,
                    "Network connection failed. Try to restart."
                )


                return e.toString()
            } else if (e is ProtocolException) {
                return "{}"
            } else {
                FirebaseCrashlytics.getInstance().recordException(e)
//                Crashlytics.logException(e)
                return e.toString()
            }
        } catch (e: UnsupportedEncodingException) {
            uploadProperty.toPreferences(context)
            //GlobalScope.async{ tryToRestart() }
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


    private fun createCustomRequestBody(contentType: MediaType, file: File, fileType:FileType, handler: Handler): RequestBody {
        return object : RequestBody() {
            override fun contentType() = contentType
            override fun contentLength() = file.length()
            override fun writeTo(sink: BufferedSink) {
                try {
                    val source = file.source()
                    //sink.writeAll(source);
                    val buf = Buffer()
                    var remaining = contentLength()
                    //totalSize = contentLength()
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
                            message.obj = fileType.mFilePath
                            handler.sendMessage(message)
                            // Progress here!~!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            /*val message = Message()
                            message.arg1 = percent
                            message.arg2 = uploadProperty.itemCounter
                            progressHandler.sendMessage(message)*/
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

}