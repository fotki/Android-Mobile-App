package com.tbox.fotki.util.sync_files

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.tbox.fotki.util.L
import com.tbox.fotki.util.LocalBroadcastHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


object LogProvider {

    private const val FILE_NAME = "log.txt"
    private const val TEMPLATE = "yyyy.MM.dd HH:mm:ss "
    private const val SUPPORT_EMAIL = "support@fotki.com"

    private fun getFilePath(context: Context) = File(context.externalCacheDir?.absolutePath, FILE_NAME)

    fun clearFile(context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(FileOutputStream(getFilePath(context).absolutePath,false))
            outputStreamWriter.write("")
            outputStreamWriter.close()
        } catch (e: IOException) {
            L.print(this,"Exception File write failed: $e")
        }
    }
    fun convertTimeToTemplate(time:Long) = SimpleDateFormat(TEMPLATE).format(Date(time))!!

    fun createLogMessage(data:String) = convertTimeToTemplate(Date().time)+data+"\r\n"

    fun writeToFile(data: String, context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val outputStreamWriter =
                    OutputStreamWriter(FileOutputStream(getFilePath(context).absolutePath,true))
                val bw = BufferedWriter(outputStreamWriter)
                bw.write(createLogMessage(data))
                //bw.newLine()
                bw.close()
                outputStreamWriter.close()
            } catch (e: IOException) {
                L.print(this,"Exception File write failed: $e")
            }
            LocalBroadcastHelper.sendLogHistoryMessege(context,data)
        }
    }

    fun readFromFile(context: Context): String? {
        var ret = ""
        try {
            val inputStream= FileInputStream(getFilePath(context))
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String?
                var stringBuilder = ""
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder = receiveString+"\n"+stringBuilder
                }
                inputStream.close()
                ret = stringBuilder
            }
        } catch (e: FileNotFoundException) {
            L.print(this,"login activity File not found: $e")
        } catch (e: IOException) {
            L.print(this,"login activity Can not read file: $e")
        }
        return ret
    }

    fun sendToEmail(activity: Activity){
        ShareCompat.IntentBuilder.from(activity)
            .setType("text/html")
            .addEmailTo(SUPPORT_EMAIL)
            .setSubject("Log from application")
            .setText("Log from backup app")
            .setStream(FileProvider.getUriForFile(activity, activity.packageName,
                getFilePath(activity)))
            .setChooserTitle("Choose")
            .startChooser()
    }

    const val START_PHOTO_RECEIVER = "Photo receiver started"
    const val RECEIVED_WITH_PROPERTIES = "Received with properties -"
    const val IS_NEED_TO_SYNC_WITH_FOLDERS = "Is need to sync with folders - "
    const val IS_NEED_TO_SYNC_WITH_ROAMING = "Is need to sync with roaming - "
    const val NOT_IN_ROAMING_SERVICE_START = "Not in roaming service start!"
    const val IN_ROAMING_DETECTED = "In roaming detected!"
    const val RESTART_UPLOAD_SERVICE = "Restart upload service."
    const val SERVICE_WITH_PROPERTY = "Service with property - "
    const val PREPARE_AND_LOAD_FIRST_FILE = "Prepare and start upload first file"
    const val IS_PAUSED_UPLOAD_THREAD = "isPaused upload thread - "
    const val UPLOAD_FAILED = "Upload failed. Failed because:"
}