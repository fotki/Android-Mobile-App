package com.tbox.fotki.util.upload_files

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.JobIntentService
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.LocalBroadcastHelper
import com.tbox.fotki.util.NotificationHelperNew
import com.tbox.fotki.util.sync_files.MediaSyncronizator
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import java.io.File
import java.io.IOException
import java.util.*


class UploadJobService : JobIntentService() {

    private var uploadProperty = UploadProperty()
    private var totalSize = 0L
    private var mRetryHitCount = 0
    private var sourceFile: File? = null
    private lateinit var uploadingTask: FileUploadingTask
    private val notificationHelperNew by lazy {
        NotificationHelperNew(
            baseContext
        )
    }

    override fun onHandleWork(intentIn: Intent) {
        if (intentIn.action == ACTION_UPLOAD) {
            uploadProperty = intentIn.getParcelableExtra(EXTRA_PROPERTY)!!
            L.print(this,"MEDIA new property - $uploadProperty")

            if (uploadProperty.fileTypes.size > 0) {
                sourceFile = File(uploadProperty.fileTypes[0].mFilePath)

                val fileName = sourceFile!!.name
                notificationHelperNew.sendNotification(
                    "Backup loading", null,
                    "Backup loading started with file $fileName"
                )
                uploadingTask = FileUploadingTask()
                uploadingTask.execute()
            }

        }
    }

    private inner class FileUploadingTask : AsyncTask<Void, Int, String>() {

        override fun doInBackground(vararg params: Void) = uploadFile()

        private fun uploadFile(): String? {
            var responseString: String?
            val httpclient = DefaultHttpClient()
            val httppost = HttpPost(Constants.BASE_URL + Constants.FILE_UPLOAD)
            try {
                httppost.entity = createEntity(AndroidMultiPartEntity.ProgressListener { num ->
                    L.print(this,"MEDIA progress $sourceFile num - $num")
                    publishProgress(
                        (num / totalSize.toFloat() * 100).toInt(),
                        uploadProperty.itemCounter
                    )
                })
                val response = httpclient.execute(httppost)
                val r_entity = response.entity

                val statusCode = response.statusLine.statusCode
                responseString = if (statusCode == 200) {
                    // Server response
                    EntityUtils.toString(r_entity)
                } else {
                    sendError("Error occurred! Http Status Code: $statusCode")
                    "Error occurred! Http Status Code: $statusCode"
                }

            } catch (e: ClientProtocolException) {
                responseString = e.toString()
                sendErrorLog("Error: $e")
            } catch (e: IOException) {
                responseString = e.toString()
                sendErrorLog("Error: $e")
                uploadProperty.isUploading = false
            }
            return responseString
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            L.print(this,"MEDIA $result")

            MediaSyncronizator(baseContext).updateSuccessUploaded(
                uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath
            )
            L.print(this,
                "MEDIA LOADED SUCCESSFULL ${uploadProperty.itemCounter}  to  " +
                        "album - ${uploadProperty.fileTypes[uploadProperty.itemCounter].albumName}! "
            )

            mRetryHitCount = 0
            uploadProperty.itemCounter += 1
            if (uploadProperty.itemCounter < uploadProperty.fileTypes.size) {
                // Log.d("TAG","Upload started - $sourceFile")
                val fileName =
                    File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath).name
                notificationHelperNew.sendNotification(
                    "Backup loading", null,
                    "Backup proceed with file $fileName"
                )


                uploadingTask = FileUploadingTask()
                uploadingTask.execute()
            } else {
                PreferenceManager.getDefaultSharedPreferences(this@UploadJobService)
                    .edit().putInt(Constants.CURRENT_LOAD_ITEM, 0).apply()
                uploadProperty.isUploading = false
                PreferenceManager.getDefaultSharedPreferences(this@UploadJobService)
                    .edit().putLong(PREF_LAST_UPLOADED_TIME, Date().time / 1000).apply()
                notificationHelperNew.sendNotification(
                    "Backup loading", null,
                    "Loading finished. Uploaded ${uploadProperty.fileTypes.size} files"
                )

            }
        }
    }

    private fun sendErrorLog(s: String) {
    }

    private fun sendError(s: String) {
        LocalBroadcastHelper.sendServiceError(this@UploadJobService, s)
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(Constants.CURRENT_LOAD_ITEM, uploadProperty.itemCounter).apply()
    }

    private fun createEntity(progressListener: AndroidMultiPartEntity.ProgressListener): AndroidMultiPartEntity? {
        val entity = AndroidMultiPartEntity(progressListener)

        if (uploadProperty.fileTypes[uploadProperty.itemCounter].mFileMimeType == Constants.VIDEO ||
            uploadProperty.fileTypes[uploadProperty.itemCounter].mFileMimeType == Constants.GIF
        ) {
            sourceFile = File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
            entity.addPart("photo", FileBody(sourceFile!!))
        } else {
            sourceFile = File(uploadProperty.fileTypes[uploadProperty.itemCounter].mFilePath)
            // Adding file data to http body
            entity.addPart("photo", FileBody(sourceFile!!))
            //}
        }
        // Extra parameters if you want to pass to server
        entity.addPart(
            Constants.SESSION_ID,
            StringBody(uploadProperty.sessionId)
        )
        entity.addPart(
            Constants.ALBUM_ID_ENC,
            StringBody(uploadProperty.fileTypes[uploadProperty.itemCounter].albumId.toString())
        )

        totalSize = entity.contentLength
        return entity
    }

    companion object {
        const val EXTRA_PROPERTY = "property"
        private const val JOB_ID = 1300
        const val ACTION_UPLOAD = "upload"
        const val PREF_LAST_UPLOADED_TIME = "last_uploaded_time"

        fun enqueueWork(context: Context, uploadProperty: UploadProperty) {
            val intent = Intent(ACTION_UPLOAD)
            intent.putExtra(EXTRA_PROPERTY, uploadProperty)
            enqueueWork(context, UploadJobService::class.java, JOB_ID, intent)
        }
    }

}