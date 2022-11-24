package com.tbox.fotki.util.sync_files

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.tbox.fotki.util.L
import org.json.JSONArray

class BackupProperties(val context: Context) {

    var folderList = HashSet<String>()
    val started = MutableLiveData<Boolean>()
    val isBackgroungEnable = MutableLiveData<Boolean>()
    val isCellularPhotos = MutableLiveData<Boolean>()
    val isCellularVideos = MutableLiveData<Boolean>()
    val isBackupRoaming = MutableLiveData<Boolean>()

    var backupFolderId = 0L

    private val preferenceHelper = PreferenceHelper(context)
    init {
        folderList = mapToListAlbums(preferenceHelper.getString(PREFERENCE_ALBUM_LIST))

        isBackgroungEnable.value = preferenceHelper.getBoolean(PREFERENCE_IS_BACKUP_ENABLE)
        isCellularPhotos.value = preferenceHelper.getBooleanTrue(PREFERENCE_IS_CELLULAR_PHOTOS)
        isCellularVideos.value = preferenceHelper.getBoolean(PREFERENCE_IS_CELLULAR_VIDEOS)
        isBackupRoaming.value = preferenceHelper.getBoolean(PREFERENCE_IS_BACKUP_ROAMING)
        started.value = preferenceHelper.getBoolean(PREFERENCE_IS_BACKUP_STARTED)
        backupFolderId = preferenceHelper.getLong(PREFERENCE_BACKUP_FOLDER)
    }

    fun start(){
        started.value = true
        preferenceHelper.applyPrefs(hashMapOf(PREFERENCE_IS_BACKUP_STARTED to true,
            PREFERENCE_IS_BACKUP_ENABLE to true
            ))
    }

    fun clearFolders(){
        commitAlbumsToPrefs(HashSet())
    }

    fun stop(){
        started.value = false
        preferenceHelper.applyPrefs(hashMapOf(PREFERENCE_IS_BACKUP_STARTED to false,
            PREFERENCE_IS_BACKUP_ENABLE to false))
    }

    fun changeStatus(started:Boolean){
        //this.started.value = started
        preferenceHelper.applyPrefs(hashMapOf(PREFERENCE_IS_BACKUP_STARTED to started))
    }

    fun commitAlbumsToPrefs(listAlbums:HashSet<String>){
        val res = JSONArray()
        for(i in listAlbums){
            res.put(i)
        }
        preferenceHelper.applyPrefs(hashMapOf(PREFERENCE_ALBUM_LIST to res.toString()))
    }

    fun applyPrefs(){
        isCellularPhotos.observe(context as AppCompatActivity,
            Observer<Boolean> { value ->
                preferenceHelper.applyPrefs(hashMapOf(
                    PREFERENCE_IS_CELLULAR_PHOTOS to value))
            })
        isCellularVideos.observe(context,
            Observer<Boolean> { value ->
                preferenceHelper.applyPrefs(hashMapOf(
                    PREFERENCE_IS_CELLULAR_VIDEOS to value)) })
        isBackupRoaming.observe(context,
            Observer<Boolean> { value ->
                preferenceHelper.applyPrefs(hashMapOf(
                    PREFERENCE_IS_BACKUP_ROAMING to value)) })
        isBackgroungEnable.observe(context,
            Observer<Boolean> { value ->
                preferenceHelper.applyPrefs(hashMapOf(
                    PREFERENCE_IS_BACKUP_ENABLE to value)) })
    }

    fun invalidate() {
        folderList = mapToListAlbums(preferenceHelper.getString(PREFERENCE_ALBUM_LIST))
    }

    override fun toString(): String {
        return folderList.toString()
    }

    companion object{
        const val PREFERENCE_IS_BACKUP_ENABLE = "is_backup_enable"
        const val PREFERENCE_IS_CELLULAR_PHOTOS = "is_cellular_photos"
        const val PREFERENCE_IS_CELLULAR_VIDEOS = "is_cellular_videos"
        const val PREFERENCE_IS_BACKUP_ROAMING = "is_backup_roaming"
        const val PREFERENCE_IS_BACKUP_STARTED = "is_started_backup"
        const val PREFERENCE_BACKUP_FOLDER = "backup_folder"
        const val PREFERENCE_ALBUM_LIST = "album_list"

        fun mapToListAlbums(strArray: String?): HashSet<String> {
            val res = HashSet<String>()
            if (strArray.isNullOrEmpty()) return res

            val jsonArray = JSONArray(strArray)
            for(i in 0 until jsonArray.length()){
                res.add(jsonArray.getString(i))
            }
            return res
        }

        fun applyFolder(context: Context, id:Long){
            PreferenceHelper(context).applyPrefs(hashMapOf(PREFERENCE_BACKUP_FOLDER to id))
        }

        fun getFolderList(context: Context) = mapToListAlbums(
            PreferenceHelper(context).getString(PREFERENCE_ALBUM_LIST))

        fun getIsCellularPhotos(context: Context) =
            PreferenceHelper(context).getBooleanTrue(PREFERENCE_IS_CELLULAR_PHOTOS)

        fun getIsCellularVideos(context: Context) =
            PreferenceHelper(context).getBoolean(PREFERENCE_IS_CELLULAR_VIDEOS)
    }
}