package com.tbox.fotki.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.upload_files.FileType
import java.util.ArrayList

object LocalBroadcastHelper {
    fun sendStartFileLoading(context: Context, mFileTypes: ArrayList<FileType>, mAlbumName: String,
                             mItemCounter: Int, loadedSize:Long){
        val intent = Intent(Constants.UPLOAD_STARTED)
        intent.putExtra(Constants.BEING_UPLOADED_STATUS, 0)
        intent.putExtra(Constants.FILE_BEING_UPLOADING, mFileTypes[mItemCounter].mFilePath)
        intent.putExtra(
            Constants.BEING_UPLOADING_FILE_MIMETYPE,
            mFileTypes[mItemCounter].mFileMimeType
        )
        intent.putExtra(Constants.CURRENT_FILE_COUNT, mItemCounter)
        intent.putExtra(Constants.TOTAL_FILES_TO_UPLOAD, mFileTypes.size)
        intent.putExtra(Constants.UPLOADING_ALBUM_NAME, mAlbumName)
        intent.putExtra(Constants.FILE_UPLOADED_LENGTH, loadedSize)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun sendProgressFileLoading(context: Context, mFileTypes: ArrayList<FileType>, mAlbumName: String,
                             mItemCounter: Int, progress:Int, loadedSize:Long,
                                fileLoadSize:Int, speed:Int){
        val intent = Intent(Constants.UPDATE_CURRENT_PROGRESS)
        intent.putExtra(Constants.BEING_UPLOADED_STATUS, progress)
        intent.putExtra(Constants.CURRENT_FILE_COUNT, mItemCounter)
        intent.putExtra(Constants.FILE_LOAD_SIZE, fileLoadSize)
        intent.putExtra(Constants.FILE_LOAD_SPEED, speed)

        try{
            intent.putExtra(Constants.FILE_BEING_UPLOADING, mFileTypes[mItemCounter].mFilePath)
        } catch (ex:Exception){
            if(mFileTypes.size>0)
            intent.putExtra(Constants.FILE_BEING_UPLOADING, mFileTypes[mFileTypes.size-1].mFilePath)
        }
        try{
            intent.putExtra(Constants.BEING_UPLOADING_FILE_MIMETYPE, mFileTypes[mItemCounter].mFileMimeType)
        } catch (ex:Exception){
            if(mFileTypes.size>0)
            intent.putExtra(Constants.BEING_UPLOADING_FILE_MIMETYPE, mFileTypes[mFileTypes.size-1].mFileMimeType)
        }

        intent.putExtra(Constants.FILE_UPLOADED_LENGTH, loadedSize)
        intent.putExtra(Constants.TOTAL_FILES_TO_UPLOAD, mFileTypes.size)
        intent.putExtra(Constants.UPLOADING_ALBUM_NAME, mAlbumName)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun sendFinishedFileLoading(context: Context, mFileTypes: ArrayList<FileType>, mAlbumName: String,
                                mItemCounter: Int, loadedSize: Long){
        val intent = Intent(Constants.ALL_UPLOADS_DONE)
        intent.putExtra(Constants.BEING_UPLOADED_STATUS, 100)
        intent.putExtra(Constants.CURRENT_FILE_COUNT, mItemCounter)
        intent.putExtra(Constants.TOTAL_FILES_TO_UPLOAD, mFileTypes.size)
        intent.putExtra(Constants.UPLOADING_ALBUM_NAME, mAlbumName)
        intent.putExtra(Constants.FILE_UPLOADED_LENGTH,loadedSize)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendCancelFileLoading(context: Context, mFileTypes: ArrayList<FileType>, mAlbumName: String,
                                mItemCounter: Int){
        val intent = Intent(Constants.SERVICE_CANCEL)
        intent.putExtra(Constants.BEING_UPLOADED_STATUS, 100)
        intent.putExtra(Constants.CURRENT_FILE_COUNT, mItemCounter)
        intent.putExtra(Constants.TOTAL_FILES_TO_UPLOAD, mFileTypes.size)
        intent.putExtra(Constants.UPLOADING_ALBUM_NAME, mAlbumName)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendStoppedLoading(context: Context){
        val intent = Intent(Constants.UPLOADINGSTOPPED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun sendSessionExpired(context: Context){
        val intent = Intent(Constants.SESSION_EXPIRED)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendServiceError(context: Context, error: String){
        val intent = Intent(Constants.SERVICE_ERROR)
        intent.putExtra(Constants.SHARING_FILE_ERROR, error)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendLogMessege(context: Context, message:String){
        val intent = Intent(Constants.SERVICE_MESSAGE)
        intent.putExtra(Constants.MESSAGE, message)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendLogHistoryMessege(context: Context, message:String){
        val intent = Intent(Constants.SERVICE_LOG_MESSAGE)
        intent.putExtra(Constants.MESSAGE, message)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendProgressFileSync(context: Context) {
        val intent = Intent(Constants.FILE_SYNC)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendProgressFileSync(context: Context, isStarted:Boolean) {
        val intent = Intent(Constants.FILE_SYNC)
        intent.putExtra(Constants.SYNC_STARTED, isStarted)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }

    fun sendNewDownloadStatus(context: Context, json: String, isFinished:Boolean) {
        Log.d("UploadFilesViewModel","LocalBroadcastHelper sendNewDownloadStatus")

        val intent = Intent(Constants.DOWNLOAD_STATUS_UPDATED)
        intent.putExtra(Constants.STATUS, json)
        intent.putExtra(Constants.UPLOADING_IN_PROGRESS,isFinished)
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(intent)
    }
}