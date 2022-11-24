package com.tbox.fotki.refactoring.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tbox.fotki.util.L
import java.io.File

object PhotoGalleryManager {
    fun deleteOneFile(applicationContext:Context, file: File) {
        if (file.exists()) {
            L.print(this,"Deleted exists")

            try {
                applicationContext.contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + "='"
                            + file.path + "'", null
                )
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
//                Crashlytics.logException(e)
            }
            file.delete()
            refreshGallery(applicationContext, file)
        }
    }

    fun refreshGallery(applicationContext:Context, file: File) {
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