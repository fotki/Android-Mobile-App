package com.tbox.fotki.util

import android.app.Activity
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.PowerManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplemobiletools.commons.extensions.areDigitsOnly
import com.simplemobiletools.commons.extensions.getDataColumn

fun View.getParentActivity(): AppCompatActivity?{
    var context = this.context
    while (context is ContextWrapper) {
        if (context is AppCompatActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Activity.registerBroadcasts(broadcasts: HashMap<String, BroadcastReceiver>){
    for(key in broadcasts.keys){
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcasts[key]!!, IntentFilter(key)
        )
    }
}

fun Activity.destroyBroadcasts(broadcasts: HashMap<String, BroadcastReceiver>){
    for(key in broadcasts.keys){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcasts[key]!!)
    }
}

fun AppCompatActivity.hideKeyboard(){
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = currentFocus
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun ProgressBar.updateProgressbarStatus(value: Int) {
    progress = value

    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val mWakeLock = pm.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        javaClass.name
    )
    mWakeLock!!.acquire()

    max = 100
    isIndeterminate = false
}
private fun isMediaDocument(uri: Uri) = uri.authority == "com.android.providers.media.documents"

private fun isDownloadsDocument(uri: Uri) = uri.authority == "com.android.providers.downloads.documents"

private fun isExternalStorageDocument(uri: Uri) = uri.authority == "com.android.externalstorage.documents"

// some helper functions were taken from https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
fun Context.getRealPathFromURIMy(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    if (isDownloadsDocument(uri)) {
        val id = DocumentsContract.getDocumentId(uri)
        L.print(this, "File id - $id")

        if (id.areDigitsOnly()) {
            val newUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
            val path = getDataColumn(newUri)
            L.print(this, "Path - $path")
            if (path != null) {
                return path
            }
        }
    } else if (isExternalStorageDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val parts = documentId.split(":")
        if (parts[0].equals("primary", true)) {
            return "${Environment.getExternalStorageDirectory().absolutePath}/${parts[1]}"
        }
    } else if (isMediaDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val split = documentId.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
        val type = split[0]

        val contentUri = when (type) {
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val selection = "_id=?"
        val selectionArgs = arrayOf(split[1])
        val path = getDataColumn(contentUri, selection, selectionArgs)
        if (path != null) {
            return path
        }
    }

    return getDataColumn(uri)
}

fun Context.getDataColumnMy(
    uri: Uri?, selection: String?,
    selectionArgs: Array<String?>?
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column
    )
    try {
        cursor = contentResolver.query(
            uri!!, projection, selection, selectionArgs,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val column_index: Int = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(column_index)
        }
    } finally {
        if (cursor != null) cursor.close()
    }
    return null
}
