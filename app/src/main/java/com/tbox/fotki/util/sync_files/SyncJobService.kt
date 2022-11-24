package com.tbox.fotki.util.sync_files

import android.annotation.TargetApi
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.core.app.JobIntentService
import com.android.volley.VolleyError
import com.tbox.fotki.model.database.FilesEntity
import com.tbox.fotki.model.web_providers.IsExistsProvider
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.util.LocalBroadcastHelper
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.upload_files.FileType
import com.tbox.fotki.util.upload_files.UploadThreadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat


class SyncJobService : JobIntentService() {

    private var currentItem = 0

    private lateinit var arrayFiles: List<FilesEntity>
    private lateinit var mediaSyncronizator: MediaSyncronizator
    //TODO hardcoded string
    override fun onHandleWork(intentIn: Intent) {
        mediaSyncronizator = MediaSyncronizator(baseContext)
        LogProvider.writeToFile("Start syncing", baseContext)
        if (intentIn.action == ACTION_UPLOAD_FROM_DB) {
            LogProvider.writeToFile("Start syncing from DB ", baseContext)
            GlobalScope.launch(Dispatchers.IO) {
                arrayFiles = mediaSyncronizator.getPartOfSyncFiles()
                if (arrayFiles.isNotEmpty()){
                    LogProvider.writeToFile("$arrayFiles", baseContext)
                    testIfExists()
                } else {
                   startTestIsCreated()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceHelper(baseContext).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
    }

    //------------------------------------------------------------------------------------STEP-1----
    //----------------------------------------------------------------------------Test-is-exists----

    private fun testIfExists() {
        try {
            IsExistsProvider(baseContext, {
                    GlobalScope.launch(Dispatchers.IO) {startTestIsCreated()}
                }, arrayFiles as ArrayList<FilesEntity>
            )
        } catch (e: Exception) {
            LogProvider.writeToFile("Error in is photo exists - $e", baseContext)
        }
    }

    //------------------------------------------------------------------------------------STEP-2----
    //---------------------------------------------------------------Test-is-year-folder-created----


    private fun startTestIsCreated() {
        arrayFiles = mediaSyncronizator.getNotCheckFolderFiles()
        if (arrayFiles.isNotEmpty())
            testIsCreatedFolder(arrayFiles[currentItem])
        else
            startSyncService(0)
    }

    private fun testIsCreatedFolder(fileType: FilesEntity) {
        L.print(this,"MEDIA testIsCreatedFolder")

        LocalBroadcastHelper.sendProgressFileSync(baseContext,true)

        var yearFolderTitle =
            SimpleDateFormat(Folder.FOLDER_YEAR_TEMPLATE).format(File(fileType.fileName).lastModified())

        LogProvider.writeToFile("testIsCreatedFolder $yearFolderTitle", baseContext)

        WebManager.instance.getAccountTree(baseContext, object :
            WebManagerInterface {
            override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                L.print(this,"MEDIA res - $response")
                try {
                    var respCurr = response.getJSONObject(Constants.DATA)
                        .getJSONObject(Constants.ACCOUNT_TREE)
                        .getJSONObject(Constants.PRIVATE_FOLDER)
                        .getJSONArray(Constants.FOLDERS)

                    for (i in 0 until respCurr.length()) {
                        var currObject = respCurr.getJSONObject(i)
                        if (currObject.getString(Constants.FOLDER_NAME) == Folder.BACKUP_FOLDER_NAME) {
                            //Log.d("TAG_MEDIA", "resp - $currObject folder - $folder")
                            val foldersArray = currObject.getJSONArray(Constants.FOLDERS)

                            var yearFolder: JSONObject? = null
                            for (i in 0 until foldersArray.length()) {
                                val folderCurr = foldersArray.getJSONObject(i)
                                if (folderCurr.getString(Constants.FOLDER_NAME) == yearFolderTitle) {
                                    yearFolder = folderCurr
                                }
                            }

                            if (yearFolder == null) {
                                LogProvider.writeToFile(
                                    "We did't find year folder. Try to create",
                                    baseContext
                                )
                                createYearFolder(currObject, yearFolderTitle, fileType)
                            } else {
                                LogProvider.writeToFile("We found year folder", baseContext)
                                testArrayFolder(yearFolder, fileType)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
            override fun sendNetworkFailure(
                isInterNetAvailableFlag: Boolean,
                apiRequestType: ApiRequestType
            ) {
            }
        })
    }

    private fun createYearFolder(
        currObject: JSONObject?,
        yearFolderTitle: String?,
        fileType: FilesEntity
    ) {
        WebManager.instance.createFolder(baseContext, object :
            WebManagerInterface {
            override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                L.print(this,"MEDIA SUCCESS CREATED!!!! $response")
                LogProvider.writeToFile("SUCCESS CREATED!!!! $response", baseContext)

                testArrayFolder(response, fileType)
            }

            override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
            override fun sendNetworkFailure(
                isInterNetAvailableFlag: Boolean,
                apiRequestType: ApiRequestType
            ) {
            }
        }, currObject!!.getLong(Constants.FOLDER_ID_ENC), yearFolderTitle)
    }

    //------------------------------------------------------------------------------------STEP-3----
    //-----------------------------------------------------------------Test-is-day-album-created----

    private fun testArrayFolder(currObject: JSONObject, fileType: FilesEntity) {
        var folder =
            SimpleDateFormat(Folder.ALBUM_TEMPLATE).format(File(fileType.fileName).lastModified())
        var id = 0L
        var folderId = 0L
        if (currObject.has(Constants.ALBUMS)) {
            var albums = currObject.getJSONArray(Constants.ALBUMS)
            for (j in 0 until albums.length()) {
                var album = albums.getJSONObject(j)
                if (album.getString(Constants.ALBUM_NAME) == folder) {
                    id = album.getLong(Constants.ALBUM_ID_ENC)
                }
            }
            folderId = currObject.getLong(Constants.FOLDER_ID_ENC)
        } else {
            folderId = currObject.getJSONObject(Constants.DATA).getLong(
                Constants.FOLDER_ID)
        }

        if (id == 0L) {
            WebManager.instance.createAlbum(
                baseContext,
                object :
                    WebManagerInterface {
                    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                        var albumId =
                            response.getJSONObject(Constants.DATA)
                                .getLong(Constants.ALBUM_ID)
                        insertInQueque(fileType, folder, albumId)
                    }

                    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
                    override fun sendNetworkFailure(
                        isInterNetAvailableFlag: Boolean,
                        apiRequestType: ApiRequestType
                    ) {
                    }
                },
                folderId,
                folder
            )
        } else {
            insertInQueque(fileType, folder, id)
        }
    }

    //------------------------------------------------------------------------------------STEP-4----
    //--------------------------------------------------------------------Update-info-about-file----

    private fun insertInQueque(fileType: FilesEntity, folderName: String, albumId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            fileType.folder = folderName
            fileType.albumId = albumId
            fileType.loadedStatus = FilesEntity.STATUS_NOT_LOADED
            mediaSyncronizator.updateFile(fileType)
        }

        if(!PreferenceHelper(baseContext).getBoolean(BackupProperties.PREFERENCE_IS_BACKUP_STARTED)){
            LocalBroadcastHelper.sendProgressFileSync(baseContext,false)
            PreferenceHelper(baseContext).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
            return
        }

        LocalBroadcastHelper.sendProgressFileSync(baseContext)
        if (currentItem<arrayFiles.size-1){
            currentItem++
            testIsCreatedFolder(arrayFiles[currentItem])
        } else {
            startSyncService(0)
        }
    }


    //------------------------------------------------------------------------------------STEP-5----
    //-----------------------------------------------------------------Start-sevice-upload-files----

    private fun startSyncService(limit: Int) {
        //sessionId = Session.getInstance(baseContext).mSessionId!!
        GlobalScope.launch(Dispatchers.IO) {
            var toUpload = mediaSyncronizator.getNotLoadedFiles()
            L.print(this,"MEDIA to upload allNotLoaded count - ${toUpload.size}")
            LogProvider.writeToFile("to upload allNotLoaded count - ${toUpload.size}", baseContext)
            val listFiles = ArrayList<FileType>()
            for (file in toUpload) {
                listFiles.add(
                    FileType(
                        file.fileName,
                        file.mimeType,
                        file.albumId,
                        file.folder,
                        file.hashSHA
                    )
                )
            }
            startUploadService(listFiles)
        }
    }

    @UiThread
    private fun startUploadService(listFiles: ArrayList<FileType>) {
        if (listFiles.size > 0) {
            try{
                val session = Session.getInstance(baseContext)
                if (session.mIsSessionId!=null){
                    val uploadProperty =
                        UploadProperty(
                            "", listFiles,
                            session.mSessionId!!, 0L, 0,
                            false, 0, true, false, false
                        )
                    val intentService = Intent(baseContext, UploadThreadService::class.java)
                    intentService.putExtra(UploadThreadService.EXTRA_PROPERTY, uploadProperty)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        baseContext.startForegroundService(intentService)
                    } else {
                        baseContext.startService(intentService)
                    }
                }
            } catch(e:Exception){

            }
        } else {

            LogProvider.writeToFile(
                "No files to upload from SyncJobService " +
                        "listFiles.size == 0", baseContext
            )
            PreferenceHelper(baseContext).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
            LocalBroadcastHelper.sendProgressFileSync(baseContext,false)
        }
    }

    companion object {
        const val EXTRA_FILES = "property"
        private const val JOB_ID = 1200
        const val ACTION_UPLOAD = "upload"
        const val ACTION_UPLOAD_FROM_DB = "upload_from_db"
        const val PREF_LAST_UPLOADED_TIME = "last_uploaded_time"

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun enqueueWork(context: Context) {
            val intent = Intent(ACTION_UPLOAD_FROM_DB)

            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            var hasBeenScheduled = false
            for (jobInfo in scheduler.allPendingJobs) {
                if (jobInfo.id == SyncJobService.JOB_ID) {
                    hasBeenScheduled = true
                    break
                }
            }
            if (!hasBeenScheduled)
                enqueueWork(context, SyncJobService::class.java, JOB_ID, intent)
            else
                LocalBroadcastHelper.sendProgressFileSync(context,false)
        }
    }
}