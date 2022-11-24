package com.tbox.fotki.view.activities.backup_settings_activity

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.CompoundButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.tbox.fotki.R
import com.tbox.fotki.model.images_provider.ImageProvider
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.view_model.ProgressNavigationViewModel
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.model.AlbumCollection
import org.json.JSONException
import org.json.JSONObject
import java.util.HashSet

class BackupSettingsViewModel : ProgressNavigationViewModel() {

    val photoCheckListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        backupProperties.isCellularPhotos.value = isChecked
    }
    val roamingCheckListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        backupProperties.isBackupRoaming.value = isChecked
    }

    val broadcasts: HashMap<String, BroadcastReceiver>
    lateinit var backupProperties: BackupProperties
    val mAlbumCollection = AlbumCollection()
    var albums = ArrayList<Album>()

    val statusSyncVisibility = MutableLiveData<Int>()

    val statusAccount = MutableLiveData<String>()

    val backupFolderMessage = MutableLiveData<String>()

    val isBackupPhoto = MutableLiveData<Boolean>()
    val isBackupVideo = MutableLiveData<Boolean>()


    val isBackupRoaming = MutableLiveData<Boolean>()

    val isBackupEnable = MutableLiveData<Boolean>()
    val isBackupStarted = MutableLiveData<Boolean>()
    val tvStopText = MutableLiveData<String>()

    init {
        broadcasts = hashMapOf(
            Constants.UPLOAD_STARTED to mUploadingStarted,
            Constants.UPDATE_CURRENT_PROGRESS to mMessageReceiver,
            Constants.ALL_UPLOADS_DONE to mAllUploadDone
        )
        isSyncInProgress.value = false
    }

    val handlerUpdateStatistic = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg!!.arg1 + msg.arg2 > 0) {
                progressMessage.value =
                    "Files in selected folders: ${msg.what}\nFiles to be uploaded: ${msg.arg1}\nSyncing..."
            } else {
                progressMessage.value = "No media to upload"
                isSyncInProgress.value = false
            }
        }
    }

    //restore backupProperties
    fun restoreFromBackupProperties() {
        L.print(this, "backup properties - $backupProperties")
        backupProperties.isCellularPhotos.value?.let { isBackupPhoto.value = it }
        backupProperties.isCellularVideos.value?.let { isBackupVideo.value = it }
        backupProperties.isBackupRoaming.value?.let { isBackupRoaming.value = it }
        isBackupEnable.value = backupProperties.isBackgroungEnable.value
        backupProperties.started.value?.let { isBackupStarted.value = it }
        backupProperties.applyPrefs()
    }

    // changed switcher background upload
    fun checkChanged(p1: Boolean) {
        backupProperties.isBackgroungEnable.value = p1
        isBackupEnable.value = p1
        isBackupStarted.value = p1
        backupProperties.changeStatus(p1)
    }

    fun getAccountInfoFromResponse(response: JSONObject) {
        try {
            if (response.getInt(Constants.OK) == 1) {
                val accInfo = response.getJSONObject(Constants.DATA).getJSONObject(
                    Constants.ACCOUNT_INFO
                )
                statusAccount.value = String.format(
                    "Space used - %.1f GB from %.1f GB",
                    accInfo.getString(Constants.SPACE_USED).toDouble() / (1024 * 1024),
                    accInfo.getString(Constants.SPACE_LIMIT).toDouble() / (1024 * 1024)
                )
            } else {
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getAlbumsInfo(activity: FragmentActivity, callback:AlbumCollection.AlbumCallbacks,
                      savedInstanceState: Bundle?) {
        /*mAlbumCollection.onCreate(activity, callback)
        mAlbumCollection.onRestoreInstanceState(savedInstanceState)
        mAlbumCollection.loadAlbums()*/
        val imageProvider = ImageProvider(activity)
        imageProvider.buckets.observe(activity, Observer {
            L.print(this,"albums - $it")
            it?.let{
                if (it.size>0){
                    albums = it

                    L.print(this,"TAG reloadAlbums ${backupProperties.folderList.size}")
                    backupProperties.invalidate()

                    L.print(this,"MEDIA reload before showSelected")
                    val selectedStr =
                        showSelectedAlbums(activity,backupProperties.folderList, albums)
                    if (selectedStr.isEmpty()) {
                        backupFolderMessage.value = activity.resources.getString(R.string.none)
                    } else {
                        backupFolderMessage.value = selectedStr
                    }

                }
            }
        })
        imageProvider.loadImageAndVideoBuckets()

    }

    private fun showSelectedAlbums(context: Context, folderList: HashSet<String>, albums: java.util.ArrayList<Album>): String {
        var res = ""
        for (album in albums) {
            if (folderList.contains(album.id)) {
                res += ", " + album.getDisplayName(context)
            }
        }
        if (res.isNotEmpty()) res = res.substring(2)
        return res
    }
}