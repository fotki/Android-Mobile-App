package com.tbox.fotki.model.images_provider

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.loader.AlbumLoader
import java.io.File

class ImageProvider(private val context: Context) {

    val buckets = MutableLiveData<ArrayList<Album>>()

    fun loadImageAndVideoBuckets(){
        val queryUri = MediaStore.Files.getContentUri("external")
        val cursor = context.contentResolver.query(
            queryUri,
            arrayOf(MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE),
            getSelectionImageAndVideo(), null, MediaStore.MediaColumns.DATE_ADDED)
        buckets.value =  getBuckets(cursor, queryUri, ::getSelectionImageAndVideoByBucketId)
    }

    private fun getSelectionImageAndVideo() =
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
    private fun getSelectionImageAndVideoByBucketId(bucketId: String) = "${getSelectionImageAndVideo()} AND ${MediaStore.MediaColumns.BUCKET_ID}=$bucketId"


    private fun getBuckets(cursor: Cursor?, queryUri: Uri, getSelectionByBucketIdFunction: (String) -> String): ArrayList<Album> {
        val bucketsList = arrayListOf<Album>()
        val bucketsIdList = HashSet<String>()
        if (cursor?.moveToLast() == true) {
            do {
                if (Thread.interrupted()) {
                    return arrayListOf()
                }
                val bucketId = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID))
                // Skip this bucket if already checked
                if (bucketsIdList.contains(bucketId)) continue
                bucketsIdList.add(bucketId)
                val bucketImagePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
                // Skip this bucket if it's not a File
                if (!File(bucketImagePath).exists()) continue
                val bucketName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME))
                val bucketMediaCount = getMediaCount(queryUri, getSelectionByBucketIdFunction(bucketId))
                bucketsList.add(Album(bucketId,getUri(cursor),bucketName,bucketMediaCount.toLong()))
                //bucketsList.add(GalleryMediaBucketModel(bucketId, bucketName, bucketImagePath, bucketMediaCount))
            } while (cursor.moveToPrevious())
        }
        cursor?.close()
        return bucketsList
    }


    private fun getUri(cursor: Cursor): Uri? {
        val id =
            cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
        val mimeType = cursor.getString(
            cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        )
        val contentUri: Uri
        contentUri = if (MimeType.isImage(mimeType)) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else if (MimeType.isVideo(mimeType)) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else { // ?
            MediaStore.Files.getContentUri("external")
        }
        return ContentUris.withAppendedId(contentUri, id)
    }

    private fun getMediaCount(queryUri: Uri, selection: String): Int {
        try {
            val cursor = context.contentResolver.query(
                queryUri, null,
                selection, null, MediaStore.MediaColumns.DATE_ADDED)
            if (cursor?.count != null && cursor.count >= 0) {
                return cursor.count
            }
            cursor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

}