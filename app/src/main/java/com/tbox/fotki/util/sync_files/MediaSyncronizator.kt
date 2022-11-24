package com.tbox.fotki.util.sync_files

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.android.volley.VolleyError
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.util.upload_files.FileType
import com.tbox.fotki.util.upload_files.UploadJobService
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.model.database.AppDatabase
import com.tbox.fotki.model.database.FilesEntity
import com.tbox.fotki.model.database.SingletonHolder
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.collections.ArrayList


class MediaSyncronizator(val context: Context) :
    WebManagerInterface {

    private val db = object : SingletonHolder<AppDatabase, Context>({
        androidx.room.Room.databaseBuilder(it, AppDatabase::class.java, "history.db").build()
    }) {}

    private var arrayFiles = ArrayList<FileType>()
    private var albumName = ""
    private var sessionId = ""
    private lateinit var backupProperties: BackupProperties
    //TODO hardcoded string
    fun readAndSync() {
        backupProperties = BackupProperties(context)
        LogProvider.writeToFile("Media syncronizator read and sync", context)
        InsertJobService.enqueueWork(context)
    }

    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        var albumId = 0L
        when (apiRequestType) {
            ApiRequestType.CREATE_ALBUM_API -> {
                var id = response.getJSONObject(Constants.DATA).getLong(Constants.ALBUM_ID)
                L.print(this,"TAG_MEDIA response - $id")
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(Folder.LAST_FOLDER_ID, id).apply()
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(Folder.LAST_CREATED_ALBUM, albumName).apply()
                albumId = id
            }
            ApiRequestType.GET_FOLDER_CONTENT -> {
                val array = response.getJSONObject(Constants.DATA).getJSONArray(Constants.ALBUMS)
                for (i in 0 until array.length()) {
                    if ((array[i] as JSONObject).getString(Constants.ALBUM_NAME) == albumName) {
                        albumId = (array[i] as JSONObject).getLong(Constants.ALBUM_ID_ENC)
                    }
                }

            }
        }
        val uploadProperty = UploadProperty(
            albumName, arrayFiles,
            sessionId, backupProperties.backupFolderId, 0,
            false, 0, true, false, false
        )

        UploadJobService.enqueueWork(context, uploadProperty)
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
    }


    fun updateSuccessUploaded(fileName: String) {
        try {
            GlobalScope.launch(Dispatchers.IO) {
                L.print(this,
                    "MEDIA UPDATED - $fileName to ${FilesEntity.LOADED}" +
                            "${db.getInstance(context).historyDao().updateNewStatus(
                                fileName, FilesEntity.LOADED
                            )}"
                )
            }
        } catch (e:Exception){}
    }

    fun updateSuccessSyncedSHA(sha: String, status:Int) {
        GlobalScope.launch(Dispatchers.IO) {
            L.print(this,
                "MEDIA UPDATED - $sha to $status" +
                        "${db.getInstance(context).historyDao().updateNewStatusSHA(sha, status)}"
            )
        }
    }


    fun updateSuccessUploadedNotSync(fileName: String) {
        L.print(this,
            "MEDIA UPDATED - $fileName to ${FilesEntity.LOADED}" +
                    "${db.getInstance(context).historyDao().updateNewStatus(
                        fileName, FilesEntity.LOADED
                    )}"
        )
    }

    fun getNotSyncFiles() = db.getInstance(context).historyDao().allForSync
    fun getPartOfSyncFiles() = db.getInstance(context).historyDao().next30ForSync
    fun getNotLoadedFiles() = db.getInstance(context).historyDao().allNotLoaded
    fun getAllSyncFile() = db.getInstance(context).historyDao().all
    fun updateDeletedUpload(fileName: String) {
        GlobalScope.launch(Dispatchers.IO) {
            db.getInstance(context).historyDao().updateNewStatus(fileName, FilesEntity.DELETED)
        }
    }

    fun updateFile(fileType: FilesEntity) {
        db.getInstance(context).historyDao().update(fileType)
    }

    fun clearUpload() {
        GlobalScope.launch(Dispatchers.IO) {
            db.getInstance(context).historyDao().deleteAll()
        }
    }

    fun getNotCheckFolderFiles() = db.getInstance(context).historyDao().allNotChecked

}