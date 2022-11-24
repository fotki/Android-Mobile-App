package com.tbox.fotki.model.database

import android.content.Context
import android.content.CursorLoader
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.util.sync_files.NetworkDetector
import com.tbox.fotki.util.upload_files.FileType
import com.tbox.fotki.util.upload_files.UploadJobService
import com.zhihu.matisse.internal.entity.Item

object PhotoCursorProvider{
    private val QUERY_URI = MediaStore.Files.getContentUri("external")
    private const val COLUMN_COUNT = "count"
    private val PROJECTION = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATA,
        "duration",
        "bucket_id"
    )
    private const val SELECTION_ALL = (
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    //+ ")"
                    + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0 "
                    + " AND " + MediaStore.MediaColumns.DATE_ADDED + "> ? "
                    + " AND bucket_id=?"
            )

    private fun getSelectionAlbumArgs(lastTime: Long, album: String): Array<String> {
        return arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            lastTime.toString(),
            album
        )
    }

    private val ORDER_BY = MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
    fun preparePhotoQuery(context: Context): ArrayList<FileType> {
        val lastUploaded = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(UploadJobService.PREF_LAST_UPLOADED_TIME, 0)

        val resolver = context.contentResolver
        val arrayFiles = ArrayList<FileType>()
        val networkType = NetworkDetector.getNetworkConnect(context)

        for (albumId in BackupProperties.getFolderList(context)) {
            /*val cursor = resolver.query(QUERY_URI,PROJECTION, SELECTION_ALL,
                getSelectionAlbumArgs(if(lastUploaded>0) lastUploaded else Date().time/1000,
                    albumId), ORDER_BY)*/
            val cursor = resolver.query(
                QUERY_URI,
                PROJECTION,
                SELECTION_ALL,
                getSelectionAlbumArgs(2000L, albumId),
                ORDER_BY
            )
            Log.d("TAG_MEDIA", "cursor - ${cursor!!.count}")

            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val item = Item.valueOf(cursor)
                    if (networkType == NetworkDetector.TYPE_WIFI ||
                        (networkType == NetworkDetector.TYPE_NETWORK &&
                                item.isImage && BackupProperties.getIsCellularPhotos(context)) ||
                        (networkType == NetworkDetector.TYPE_NETWORK &&
                                item.isVideo && BackupProperties.getIsCellularVideos(context))
                    ) {
                        arrayFiles.add(
                            FileType(
                                cursor.getString(
                                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)),
                                context.contentResolver.getType(item.uri)!!
                            )
                        )
                    }
                } while (cursor.moveToNext())
            }
        }
        Log.d("MEDIA_TAG", "Array files - ${arrayFiles.size} network - $networkType")
        return arrayFiles
    }

    private fun getPath(context: Context, contentUri: Uri): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val loader = CursorLoader(context, contentUri, proj, null, null, null)
        val cursor = loader.loadInBackground()
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val result = cursor.getString(column_index)
        cursor.close()
        return result
    }
}